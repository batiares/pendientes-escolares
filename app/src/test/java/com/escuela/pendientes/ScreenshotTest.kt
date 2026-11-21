package com.escuela.pendientes

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Capturas de las pantallas (en CI: ./gradlew recordPaparazziDebug). */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(screenHeight = 3600),
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    private val today = LocalDate.of(2026, 11, 28)
    private val items = catalog().map { if (it.due < LocalDate.of(2026, 11, 1)) it.copy(done = true) else it } +
        Item("u-1", "Junta con mesa directiva", LocalDate.of(2026, 11, 30), custom = true, kind = Kind.EVENT,
            time = LocalTime.of(10, 0), remind = 60, notes = "Dirección, llevar informe") +
        Item("u-2", "Llamar a supervisión", LocalDate.of(2026, 11, 30), custom = true, kind = Kind.REMINDER,
            time = LocalTime.of(13, 30), remind = 15) +
        Item("u-3", "Posada navideña", LocalDate.of(2026, 12, 18), custom = true, kind = Kind.EVENT, remind = 1440)

    private fun screen(selected: LocalDate, editing: Item? = null) = paparazzi.snapshot {
        Theme { CalendarScreen(items, today, {}, {}, { _, _ -> }, { _, _ -> }, selected, editing) }
    }

    @Test fun noviembre() = screen(LocalDate.of(2026, 11, 30))
    @Test fun diciembre() = screen(LocalDate.of(2026, 12, 25))
    @Test fun marzo() = screen(LocalDate.of(2027, 3, 16))
    @Test fun nuevoEvento() = screen(today, Item("u-9", "Festival de invierno", LocalDate.of(2026, 12, 11), custom = true,
        kind = Kind.EVENT, time = LocalTime.of(9, 0), remind = 60))
}
