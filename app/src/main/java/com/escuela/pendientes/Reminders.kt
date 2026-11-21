package com.escuela.pendientes

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Guarda la lista en el teléfono; la primera vez la llena con el documento del ciclo. */
object Store {
    private const val CATALOG_VERSION = 2 // 2: fechas ajustadas al calendario SEP (sin vacaciones ni suspensiones)
    private fun prefs(c: Context) = c.getSharedPreferences("pendientes", Context.MODE_PRIVATE)
    fun load(c: Context): List<Item> {
        val p = prefs(c)
        val saved = p.getString("items", null)?.let(::fromJson) ?: return catalog()
        if (p.getInt("catalog", 1) >= CATALOG_VERSION) return saved
        // Actualiza las fechas de la lista del documento sin perder lo palomeado ni lo agregado.
        val fresh = catalog().associateBy { it.id }
        return saved.map { old -> fresh[old.id]?.let { old.copy(due = it.due) } ?: old }.also { save(c, it) }
    }
    fun save(c: Context, items: List<Item>) =
        prefs(c).edit().putString("items", toJson(items)).putInt("catalog", CATALOG_VERSION).apply()
}

private const val CHANNEL = "entregas"

/** Revisa una vez al día (8 a. m. aprox.) y avisa de los documentos que se entregan en 2 días, mañana u hoy. */
class ReminderWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result {
        val today = LocalDate.now()
        Store.load(applicationContext).filter { needsReminder(it, today) }.forEach {
            post(applicationContext, it, "Entregar ${whenText(daysLeft(it, today))}: ${it.title}",
                "Fecha límite: ${it.due.format(longDate)}")
        }
        return Result.success()
    }
}

fun scheduleReminders(c: Context) {
    val now = LocalDateTime.now()
    var next = now.toLocalDate().atTime(ALL_DAY_HOUR)
    if (!next.isAfter(now)) next = next.plusDays(1)
    val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
        .build()
    WorkManager.getInstance(c).enqueueUniquePeriodicWork("recordatorios", ExistingPeriodicWorkPolicy.KEEP, req)
}

private val es = Locale("es", "MX")
private val longDate = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)
val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", es)

fun whenText(days: Long) = when (days) { 0L -> "HOY"; 1L -> "mañana"; else -> "en $days días" }

private fun post(c: Context, item: Item, title: String, text: String) {
    if (Build.VERSION.SDK_INT >= 33 &&
        c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val nm = c.getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel(CHANNEL, "Entregas y eventos", NotificationManager.IMPORTANCE_HIGH))
    val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    val n = Notification.Builder(c, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(Notification.BigTextStyle().bigText(text))
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    nm.notify(item.id.hashCode(), n)
}

// La URI hace única la alarma de cada evento, así se puede cancelar o reemplazar.
private fun alarmIntent(c: Context, id: String) = PendingIntent.getBroadcast(
    c, 0, Intent(c, AlarmReceiver::class.java).setData(Uri.parse("pendientes://item/" + Uri.encode(id))),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

/** Quita las alarmas de [old] y programa las de [new] que todavía no pasan. */
fun syncAlarms(c: Context, old: List<Item>, new: List<Item>) {
    val am = c.getSystemService(AlarmManager::class.java)
    old.filter { alarmAt(it) != null }.forEach { am.cancel(alarmIntent(c, it.id)) }
    val now = LocalDateTime.now()
    new.forEach { item ->
        val at = alarmAt(item)?.takeIf { it.isAfter(now) } ?: return@forEach
        val ms = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = alarmIntent(c, item.id)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
    }
}

/** Suena el aviso de un evento; al reiniciar o actualizar la app vuelve a programar todo (Android borra las alarmas). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
        val items = Store.load(c)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            syncAlarms(c, emptyList(), items)
            return
        }
        val id = intent.data?.lastPathSegment ?: return
        val item = items.find { it.id == id } ?: return
        val hour = item.time?.format(timeFmt) ?: "todo el día"
        post(c, item, "${item.kind.emoji} ${item.title}",
            "${whenText(daysLeft(item, LocalDate.now())).replaceFirstChar { it.uppercase() }}, $hour" +
                if (item.notes.isNotBlank()) "\n${item.notes}" else "")
    }
}
