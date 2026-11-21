package com.escuela.pendientes

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Captura de la pantalla principal (en CI: ./gradlew recordPaparazziDebug). */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, theme = "android:Theme.Material.Light.NoActionBar")

    @Test fun principal() = paparazzi.snapshot {
        val items = catalog().map { if (it.due < LocalDate.of(2026, 11, 1)) it.copy(done = true) else it }
        Theme { PendientesScreen(items, LocalDate.of(2026, 11, 28), {}, {}, {}, { _, _ -> }) }
    }
}
