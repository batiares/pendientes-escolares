package com.escuela.pendientes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val es = Locale("es", "MX")
val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d 'de' MMM", es)
private val dayTitleFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", es)

private val monthEmoji = mapOf(
    8 to "🎒", 9 to "🎒", 10 to "🎃", 11 to "🌼", 12 to "🎄", 1 to "⛄",
    2 to "💖", 3 to "🌷", 4 to "🐣", 5 to "🌸", 6 to "☀️", 7 to "🎓",
)

fun newItem(date: LocalDate) =
    Item("u-${System.currentTimeMillis()}", "", date, custom = true, kind = Kind.EVENT, remind = 60)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    items: List<Item>,
    today: LocalDate,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    pickDate: (LocalDate, (LocalDate) -> Unit) -> Unit,
    pickTime: (java.time.LocalTime, (java.time.LocalTime) -> Unit) -> Unit,
    initialSelected: LocalDate = today,
    initialEditing: Item? = null,
) {
    var selected by rememberSaveable { mutableStateOf(initialSelected) }
    var month by rememberSaveable { mutableStateOf(YearMonth.from(initialSelected)) }
    var editing by remember { mutableStateOf(initialEditing) }
    fun select(d: LocalDate) { selected = d; month = YearMonth.from(d) }

    editing?.let { e ->
        Editor(e, isNew = items.none { it.id == e.id }, pickDate, pickTime,
            onClose = { editing = null },
            onSave = { onSave(it); select(it.due); editing = null },
            onDelete = { onDelete(it); editing = null })
        return
    }

    val byDate = items.groupBy { it.due }
    val dayItems = byDate[selected].orEmpty().sortedWith(compareBy({ it.time }, { it.kind }, { it.title }))
    val soon = items.filter { needsReminder(it, today) }.sortedBy { it.due }
    val late = items.filter { it.kind == Kind.DOC && !it.done && it.due < today }.sortedBy { it.due }
    val docs = items.filter { it.kind == Kind.DOC }

    Scaffold(
        containerColor = Cream,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editing = newItem(selected) }, containerColor = Coral,
                contentColor = Color.White, shape = RoundedCornerShape(50)) { Text("＋  Agregar", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        },
    ) { pad ->
        LazyColumn(contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp), modifier = Modifier.padding(pad)) {
            item {
                Header(month, docs.count { it.done }, docs.size,
                    onPrev = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) },
                    onToday = { select(today) })
            }
            item {
                MonthGrid(month, selected, today, byDate, onSelect = ::select,
                    onSwipe = { month = month.plusMonths(it.toLong()) })
            }
            item { Legend(month) }
            if (late.isNotEmpty() || soon.isNotEmpty()) item {
                Column(
                    Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF1C1))
                        .border(2.dp, Sun, RoundedCornerShape(20.dp)).padding(14.dp),
                ) {
                    if (late.isNotEmpty()) Text("⚠️ ${late.size} documentos atrasados · ver el primero", color = Late,
                        fontWeight = FontWeight.Bold, modifier = Modifier.clickable { select(late.first().due) }.padding(vertical = 4.dp))
                    soon.forEach {
                        Text("⏰ Entregar ${whenText(daysLeft(it, today))}: ${it.title}", color = Soon, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { select(it.due) }.padding(vertical = 3.dp))
                    }
                }
            }
            item {
                Text(selected.format(dayTitleFmt).replaceFirstChar { it.uppercase() }, color = Ink, fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
                sepCalendar[selected].orEmpty().forEach { s ->
                    val c = Color(s.type.color)
                    Text("${s.type.emoji}  ${s.name}" + if (s.name != s.type.label) " · ${s.type.label}" else "",
                        color = if (s.type.strong) Color.White else Ink, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (s.type.strong) c else c.copy(alpha = .25f)).padding(12.dp))
                }
                if (dayItems.isEmpty()) Text("Nada agendado este día. Toca ＋ Agregar para poner un evento o recordatorio.",
                    color = Muted, modifier = Modifier.padding(vertical = 8.dp))
            }
            items(dayItems, key = { it.id }) { ItemCard(it, today, onOpen = { editing = it }, onToggle = onSave) }
        }
    }
}

@Composable
private fun Header(month: YearMonth, done: Int, total: Int, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Coral, Color(0xFFFF9F45), Sun))).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pendientes de la Dirección · Preescolar", color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("Hoy", color = Coral, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(50))
                .background(Color.White).clickable(onClick = onToday).padding(horizontal = 14.dp, vertical = 6.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            NavButton("‹", onPrev)
            Text("${monthEmoji.getValue(month.monthValue)} " + month.atDay(1).format(monthFmt).replaceFirstChar { it.uppercase() },
                color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f))
            NavButton("›", onNext)
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
            color = Color.White, trackColor = Color.White.copy(alpha = .35f), drawStopIndicator = {},
        )
        Text("⭐ $done de $total documentos entregados", color = Color.White, fontWeight = FontWeight.Bold,
            fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun NavButton(text: String, onClick: () -> Unit) =
    Text(text, color = Coral, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White).clickable(onClick = onClick))

@Composable
private fun MonthGrid(
    month: YearMonth, selected: LocalDate, today: LocalDate, byDate: Map<LocalDate, List<Item>>,
    onSelect: (LocalDate) -> Unit, onSwipe: (Int) -> Unit,
) {
    val offset = month.atDay(1).dayOfWeek.value % 7 // la semana empieza en domingo
    val cells = offset + month.lengthOfMonth()
    var drag = 0f
    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).padding(6.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = { if (drag > 80) onSwipe(-1) else if (drag < -80) onSwipe(1); drag = 0f }) { _, d -> drag += d }
            },
    ) {
        Row {
            listOf("D", "L", "M", "M", "J", "V", "S").forEach {
                Text(it, color = Muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        for (row in 0 until (cells + 6) / 7) Row {
            for (col in 0..6) {
                val day = row * 7 + col - offset + 1
                if (day < 1 || day > month.lengthOfMonth()) Spacer(Modifier.weight(1f).height(56.dp))
                else DayCell(month.atDay(day), selected, today, byDate[month.atDay(day)].orEmpty(), onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: LocalDate, today: LocalDate, items: List<Item>, onSelect: (LocalDate) -> Unit, modifier: Modifier) {
    val sep = sepCalendar[date].orEmpty()
    val main = sep.firstOrNull { it.type.strong } ?: sep.firstOrNull()
    val bg = when {
        main == null -> Color.Transparent
        main.type.strong -> Color(main.type.color)
        else -> Color(main.type.color).copy(alpha = .28f)
    }
    val textColor = if (main?.type?.strong == true) Color.White else Ink
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(56.dp).padding(2.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .border(if (date == selected) 3.dp else 0.dp, if (date == selected) Ink else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onSelect(date) }.padding(top = 4.dp),
    ) {
        Text("${date.dayOfMonth}", color = if (date == today && main?.type?.strong != true) Coral else textColor,
            fontWeight = if (date == today || sep.any { it.type == Sep.CONMEMORATIVO }) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.size(26.dp).then(if (date == today) Modifier.border(2.dp, if (main?.type?.strong == true) Color.White else Coral, CircleShape) else Modifier)
                .padding(top = 2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 3.dp)) {
            items.take(3).forEach {
                Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor(it, today)).border(1.dp, Color.White, CircleShape))
            }
            if (items.size > 3) Text("+${items.size - 3}", fontSize = 9.sp, color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}

private fun dotColor(item: Item, today: LocalDate) = when {
    item.kind != Kind.DOC -> item.kind.color
    item.done -> Muted
    item.due < today -> Late
    else -> Coral
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(month: YearMonth) {
    val types = sepCalendar.filterKeys { YearMonth.from(it) == month }.values.flatten().map { it.type }.distinct().sorted()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 10.dp)) {
        Kind.entries.forEach { k -> Chip(k.color, "${k.emoji} ${k.label}", dot = true) }
        types.forEach { t -> Chip(Color(t.color), "${t.emoji} ${t.label}") }
    }
}

@Composable
private fun Chip(color: Color, text: String, dot: Boolean = false) =
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(50))
        .background(Color.White).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Box(Modifier.size(if (dot) 9.dp else 12.dp).clip(if (dot) CircleShape else RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = Ink)
    }

fun remindLabel(item: Item) = when {
    item.kind == Kind.DOC -> "aviso 2 días antes"
    else -> remindOptions(item.time != null).firstOrNull { it.first == item.remind }?.second?.lowercase() ?: "con aviso"
}

@Composable
private fun ItemCard(item: Item, today: LocalDate, onOpen: () -> Unit, onToggle: (Item) -> Unit) {
    val days = daysLeft(item, today)
    val status = when {
        item.kind != Kind.DOC -> ""
        item.done -> "✅ entregado"
        days < 0 -> "⚠️ atrasado"
        days <= REMIND_DAYS -> "⏰ ${whenText(days)}"
        else -> ""
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (item.done) Color(0xFFF3F1F7) else Color.White).clickable(onClick = onOpen),
    ) {
        Box(Modifier.width(8.dp).height(72.dp).background(item.kind.color))
        Text(item.kind.emoji, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(item.title, color = if (item.done) Muted else Ink, fontWeight = FontWeight.Bold,
                textDecoration = if (item.done) TextDecoration.LineThrough else null)
            Text((item.time?.format(timeFmt) ?: "Todo el día") + " · ${remindLabel(item)}" + if (status.isNotEmpty()) "  $status" else "",
                color = if (status.startsWith("⚠")) Late else if (status.startsWith("⏰")) Soon else Muted, fontSize = 13.sp)
            if (item.notes.isNotBlank()) Text(item.notes, color = Muted, fontSize = 13.sp, maxLines = 2)
        }
        if (item.kind == Kind.DOC) Checkbox(checked = item.done, onCheckedChange = { onToggle(item.copy(done = it)) },
            colors = CheckboxDefaults.colors(checkedColor = Coral, uncheckedColor = Coral))
        else Spacer(Modifier.width(12.dp))
    }
}
