package com.escuela.pendientes

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleReminders(this)
        val start = Store.load(this)
        syncAlarms(this, start, start)
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            var items by remember { mutableStateOf(start) }
            fun update(new: List<Item>) { syncAlarms(this, items, new); items = new; Store.save(this, new) }
            Theme {
                CalendarScreen(
                    items = items,
                    today = LocalDate.now(),
                    onSave = { s -> update(if (items.any { it.id == s.id }) items.map { if (it.id == s.id) s else it } else items + s) },
                    onDelete = { gone -> update(items.filter { it.id != gone.id }) },
                    pickDate = { initial, done ->
                        DatePickerDialog(this, { _, y, m, d -> done(LocalDate.of(y, m + 1, d)) },
                            initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                    },
                    pickTime = { initial, done ->
                        TimePickerDialog(this, { _, h, m -> done(LocalTime.of(h, m)) },
                            initial.hour, initial.minute, DateFormat.is24HourFormat(this)).show()
                    },
                )
            }
        }
    }
}

// Paleta alegre de preescolar.
val Coral = Color(0xFFFF6B6B)
val Sun = Color(0xFFFFC93C)
val Ink = Color(0xFF3A2E5C)
val Late = Color(0xFFE03E3E)
val Soon = Color(0xFFE07B00)
val Muted = Color(0xFF8A84A3)
val Cream = Color(0xFFFFF8EE)

val Kind.color get() = when (this) { Kind.DOC -> Coral; Kind.EVENT -> Color(0xFF4D96FF); Kind.REMINDER -> Color(0xFF2EB872) }

@Composable
fun Theme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(primary = Coral, onSurface = Ink, background = Cream, surface = Cream),
    content = content,
)
