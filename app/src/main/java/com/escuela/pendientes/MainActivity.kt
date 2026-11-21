package com.escuela.pendientes

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleReminders(this)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            var items by remember { mutableStateOf(Store.load(this)) }
            fun update(new: List<Item>) { items = new; Store.save(this, new) }
            Theme {
                PendientesScreen(
                    items = items,
                    today = LocalDate.now(),
                    onChange = { changed -> update(items.map { if (it.id == changed.id) changed else it }) },
                    onAdd = { update(items + it) },
                    onDelete = { gone -> update(items.filter { it.id != gone.id }) },
                    pickDate = { start, done ->
                        DatePickerDialog(this, { _, y, m, d -> done(LocalDate.of(y, m + 1, d)) },
                            start.year, start.monthValue - 1, start.dayOfMonth).show()
                    },
                )
            }
        }
    }
}

// Paleta alegre de preescolar.
private val Coral = Color(0xFFFF6B6B)
private val Sun = Color(0xFFFFC93C)
private val Ink = Color(0xFF3A2E5C)
private val Late = Color(0xFFE03E3E)
private val Soon = Color(0xFFE07B00)
private val Muted = Color(0xFF8A84A3)
private val Cream = Color(0xFFFFF8EE)

/** Color y emoji de cada mes del ciclo. */
private val monthStyle = mapOf(
    8 to (Color(0xFF4D96FF) to "🎒"), 9 to (Color(0xFF4D96FF) to "🎒"), 10 to (Color(0xFFFF8E3C) to "🎃"),
    11 to (Color(0xFFB15EFF) to "🌼"), 12 to (Color(0xFF2EB872) to "🎄"), 1 to (Color(0xFF3FA7D6) to "⛄"),
    2 to (Color(0xFFFF5D8F) to "💖"), 3 to (Color(0xFF4CAF50) to "🌷"), 4 to (Color(0xFFF0A500) to "🐣"),
    5 to (Color(0xFFFF6FB5) to "🌸"), 6 to (Color(0xFFFF9F1C) to "☀️"), 7 to (Color(0xFF7C5CFF) to "🎓"),
)

@Composable
fun Theme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(primary = Coral, onSurface = Ink, background = Cream, surface = Cream),
    content = content,
)

private val es = Locale("es", "MX")
private val dayFmt = DateTimeFormatter.ofPattern("EEE d 'de' MMM", es)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", es)

@Composable
fun PendientesScreen(
    items: List<Item>,
    today: LocalDate,
    onChange: (Item) -> Unit,
    onAdd: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    pickDate: (LocalDate, (LocalDate) -> Unit) -> Unit,
) {
    val byMonth = items.sortedWith(compareBy({ it.due }, { it.title })).groupBy { YearMonth.from(it.due) }
    val soon = items.filter { needsReminder(it, today) }.sortedBy { it.due }
    val late = items.count { !it.done && it.due < today }
    val done = items.count { it.done }
    // Índice de la cabecera del mes actual: 2 tarjetas de arriba + (cabecera + renglones) de cada mes anterior.
    val startIndex = 2 + byMonth.filterKeys { it < YearMonth.from(today) }.values.sumOf { it.size + 1 }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Cream,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = true }, containerColor = Coral, contentColor = Color.White,
                shape = RoundedCornerShape(50)) { Text("✏️  Agregar pendiente", fontWeight = FontWeight.Bold) }
        },
    ) { pad ->
        LazyColumn(state = list, contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), modifier = Modifier.padding(pad)) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Coral, Color(0xFFFF9F45), Sun))).padding(20.dp),
                ) {
                    Text("🍎 ✏️ 📚", fontSize = 26.sp)
                    Text("Pendientes de la Dirección", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Preescolar · Ciclo 2026-2027", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { if (items.isEmpty()) 0f else done.toFloat() / items.size },
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)),
                        color = Color.White, trackColor = Color.White.copy(alpha = .35f), drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("⭐ $done de ${items.size} entregados" + if (late > 0) "  ·  ⚠️ $late atrasados" else "",
                        color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
            }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFFFFF1C1))
                        .border(2.dp, Sun, RoundedCornerShape(24.dp)).padding(16.dp),
                ) {
                    Text("⏰ Entregas de los próximos $REMIND_DAYS días", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    if (soon.isEmpty()) Text("¡Todo tranquilo! 🌈 Te aviso $REMIND_DAYS días antes de cada entrega.", color = Ink)
                    soon.forEach {
                        Text("• ${it.title} — ${whenText(daysLeft(it, today))}", color = Soon, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
            byMonth.forEach { (ym, monthItems) ->
                val (color, emoji) = monthStyle.getValue(ym.monthValue)
                item {
                    Row(Modifier.padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$emoji  " + ym.atDay(1).format(monthFmt).replaceFirstChar { it.uppercase() },
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 16.dp, vertical = 8.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("${monthItems.count { it.done }}/${monthItems.size}", color = color, fontWeight = FontWeight.Bold)
                    }
                }
                items(monthItems, key = { it.id }) { item -> ItemRow(item, color, today, onChange, onDelete, pickDate) }
            }
        }
    }
    if (adding) AddDialog(today, pickDate, onDismiss = { adding = false }) { onAdd(it); adding = false }
}

@Composable
private fun ItemRow(
    item: Item, color: Color, today: LocalDate, onChange: (Item) -> Unit, onDelete: (Item) -> Unit,
    pickDate: (LocalDate, (LocalDate) -> Unit) -> Unit,
) {
    val days = daysLeft(item, today)
    val (dateColor, note) = when {
        item.done -> Muted to "✅ entregado"
        days < 0 -> Late to "⚠️ atrasado"
        days <= REMIND_DAYS -> Soon to "⏰ ${whenText(days)}"
        else -> Muted to ""
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (item.done) Color(0xFFF3F1F7) else Color.White)
            .border(2.dp, color.copy(alpha = if (item.done) .15f else .35f), RoundedCornerShape(18.dp))
            .padding(end = 8.dp),
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onChange(item.copy(done = it)) },
            colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(item.title, fontWeight = FontWeight.SemiBold, textDecoration = if (item.done) TextDecoration.LineThrough else null,
                color = if (item.done) Muted else Ink)
            // Tocar la fecha permite cambiarla.
            Text("📅 " + item.due.format(dayFmt) + if (note.isNotEmpty()) "  $note" else "", color = dateColor, fontSize = 13.sp,
                fontWeight = if (note.isNotEmpty() && !item.done) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable { pickDate(item.due) { onChange(item.copy(due = it)) } }.padding(vertical = 4.dp))
        }
        if (item.custom) TextButton(onClick = { onDelete(item) }) { Text("🗑️", fontSize = 18.sp) }
    }
}

@Composable
private fun AddDialog(today: LocalDate, pickDate: (LocalDate, (LocalDate) -> Unit) -> Unit, onDismiss: () -> Unit, onSave: (Item) -> Unit) {
    var title by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(today.plusDays(7)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        title = { Text("✏️ Nuevo documento o trabajo", fontWeight = FontWeight.ExtraBold, color = Ink) },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("¿Qué hay que entregar?") }, singleLine = true,
                    shape = RoundedCornerShape(16.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { pickDate(due) { due = it } }, shape = RoundedCornerShape(50)) {
                    Text("📅 Entrega: ${due.format(dayFmt)}")
                }
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), shape = RoundedCornerShape(50), onClick = {
                onSave(Item("u-${System.currentTimeMillis()}", title.trim(), due, custom = true))
            }) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
