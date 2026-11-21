package com.escuela.pendientes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/** Opciones de aviso (minutos antes; null = sin aviso), como en Google Calendar. */
fun remindOptions(hasTime: Boolean): List<Pair<Int?, String>> = listOf(
    null to "Sin aviso",
    0 to if (hasTime) "A la hora" else "Ese día 8 a. m.",
    15 to "15 min antes",
    60 to "1 hora antes",
    1440 to "1 día antes",
)

/** Pantalla completa para crear o editar un documento, evento o recordatorio. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Editor(
    original: Item,
    isNew: Boolean,
    pickDate: (java.time.LocalDate, (java.time.LocalDate) -> Unit) -> Unit,
    pickTime: (LocalTime, (LocalTime) -> Unit) -> Unit,
    onClose: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: (Item) -> Unit,
) {
    var item by remember { mutableStateOf(original) }
    // En las capturas de prueba no hay botón "atrás".
    if (LocalOnBackPressedDispatcherOwner.current != null) BackHandler(onBack = onClose)

    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("✕", fontSize = 22.sp, color = Ink) }
            Text(if (isNew) "Nuevo" else "Editar", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                modifier = Modifier.weight(1f))
            Button(enabled = item.title.isNotBlank(), shape = RoundedCornerShape(50),
                onClick = { onSave(item.copy(title = item.title.trim(), notes = item.notes.trim())) }) {
                Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Section("¿Qué es?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Kind.entries.forEach { k ->
                val on = item.kind == k
                Text("${k.emoji} ${k.label}", color = if (on) Color.White else Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) k.color else Color.White)
                        .border(2.dp, k.color, RoundedCornerShape(50))
                        .clickable { item = item.copy(kind = k, remind = if (k == Kind.DOC) null else item.remind ?: 60) }
                        .padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }

        Section("Título")
        OutlinedTextField(item.title, { item = item.copy(title = it) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (item.kind == Kind.DOC) "Ej. Reporte de incidencias" else "Ej. Festival de primavera") },
            shape = RoundedCornerShape(16.dp), textStyle = LocalTextStyle.current.copy(fontSize = 18.sp))

        Section("¿Cuándo?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("📅 " + item.due.format(dayFmt)) { pickDate(item.due) { item = item.copy(due = it) } }
            Pill("🕐 " + (item.time?.format(timeFmt) ?: "Todo el día")) {
                pickTime(item.time ?: LocalTime.of(9, 0)) { item = item.copy(time = it) }
            }
            if (item.time != null) TextButton(onClick = { item = item.copy(time = null) }) { Text("Quitar hora", color = Coral) }
        }
        sepCalendar[item.due].orEmpty().filter { it.type.noClasses }.forEach {
            Text("${it.type.emoji} Ojo: ese día es ${it.name.lowercase()} (${it.type.label.lowercase()}).", color = Late,
                fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Section("Aviso")
        if (item.kind == Kind.DOC) {
            Text("⏰ Te aviso 2 días antes, 1 día antes y el mismo día, a las 8 a. m., hasta que lo palomees.", color = Ink)
        } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            remindOptions(item.time != null).forEach { (min, label) ->
                val on = item.remind == min
                Text(label, color = if (on) Color.White else Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) item.kind.color else Color.White)
                        .border(1.dp, item.kind.color, RoundedCornerShape(50)).clickable { item = item.copy(remind = min) }
                        .padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }

        Section("Notas")
        OutlinedTextField(item.notes, { item = item.copy(notes = it) }, modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            placeholder = { Text("Lugar, qué llevar, a quién avisar…") }, shape = RoundedCornerShape(16.dp))

        if (item.kind == Kind.DOC) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            Checkbox(item.done, { item = item.copy(done = it) }, colors = CheckboxDefaults.colors(checkedColor = Coral))
            Text("Ya lo entregué", color = Ink, fontWeight = FontWeight.SemiBold)
        }

        if (!isNew && original.custom) OutlinedButton(onClick = { onDelete(original) }, shape = RoundedCornerShape(50),
            modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) { Text("🗑️  Borrar", color = Late, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Section(text: String) =
    Text(text, color = Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

@Composable
private fun Pill(text: String, onClick: () -> Unit) =
    Text(text, color = Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).border(2.dp, Sun, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp))
