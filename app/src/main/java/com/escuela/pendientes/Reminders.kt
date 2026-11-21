package com.escuela.pendientes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Guarda la lista en el teléfono; la primera vez la llena con el documento del ciclo. */
object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("pendientes", Context.MODE_PRIVATE)
    fun load(c: Context): List<Item> = prefs(c).getString("items", null)?.let(::fromJson) ?: catalog()
    fun save(c: Context, items: List<Item>) = prefs(c).edit().putString("items", toJson(items)).apply()
}

private const val CHANNEL = "entregas"
private const val REMIND_HOUR = 8

/** Revisa una vez al día (8 a. m. aprox.) y avisa de lo que se entrega en 2 días, mañana u hoy. */
class ReminderWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result {
        notifyDue(applicationContext, LocalDate.now())
        return Result.success()
    }
}

fun scheduleReminders(c: Context) {
    val now = LocalDateTime.now()
    var next = now.toLocalDate().atTime(REMIND_HOUR, 0)
    if (!next.isAfter(now)) next = next.plusDays(1)
    val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
        .build()
    WorkManager.getInstance(c).enqueueUniquePeriodicWork("recordatorios", ExistingPeriodicWorkPolicy.KEEP, req)
}

private val dateFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "MX"))

fun whenText(days: Long) = when (days) { 0L -> "HOY"; 1L -> "mañana"; else -> "en $days días" }

fun notifyDue(c: Context, today: LocalDate) {
    if (Build.VERSION.SDK_INT >= 33 &&
        c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val nm = c.getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(NotificationChannel(CHANNEL, "Entregas próximas", NotificationManager.IMPORTANCE_HIGH))
    val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    Store.load(c).filter { needsReminder(it, today) }.forEach { item ->
        val n = android.app.Notification.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Entregar ${whenText(daysLeft(item, today))}: ${item.title}")
            .setContentText("Fecha límite: ${item.due.format(dateFmt)}")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(item.id.hashCode(), n)
    }
}
