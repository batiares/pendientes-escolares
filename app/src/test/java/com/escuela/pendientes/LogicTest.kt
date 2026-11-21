package com.escuela.pendientes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class LogicTest {
    private val nov = YearMonth.of(2026, 11)

    @Test fun fechaExplicitaDelDocumento() =
        assertEquals("Agenda inicial" to LocalDate.of(2026, 10, 1), dueFor("Agenda inicial (01 oct)", YearMonth.of(2026, 10)))

    @Test fun agendaInicialPrimerDiaHabil() =
        assertEquals(LocalDate.of(2026, 11, 2), dueFor("Agenda inicial", nov).second) // 1 nov 2026 es domingo

    @Test fun restoUltimoDiaHabil() =
        assertEquals(LocalDate.of(2026, 11, 30), dueFor("Incapacidades", nov).second) // lunes

    @Test fun ultimoDiaHabilSaltaFinDeSemana() =
        assertEquals(LocalDate.of(2027, 1, 29), dueFor("Incapacidades", YearMonth.of(2027, 1)).second) // 31 ene es domingo

    @Test fun recordatorioSoloDentroDeDosDias() {
        val item = Item("x", "Reporte", LocalDate.of(2026, 11, 30))
        assertFalse(needsReminder(item, LocalDate.of(2026, 11, 27)))
        assertTrue(needsReminder(item, LocalDate.of(2026, 11, 28)))
        assertTrue(needsReminder(item, LocalDate.of(2026, 11, 30)))
        assertFalse(needsReminder(item, LocalDate.of(2026, 12, 1)))
        assertFalse(needsReminder(item.copy(done = true), LocalDate.of(2026, 11, 29)))
    }

    @Test fun catalogoSinIdsRepetidosYDentroDelCiclo() {
        val c = catalog()
        assertEquals(c.size, c.map { it.id }.toSet().size)
        assertTrue(c.all { it.due >= LocalDate.of(2026, 8, 1) && it.due <= LocalDate.of(2027, 7, 31) })
    }

    @Test fun jsonIdaYVuelta() {
        val c = catalog().take(3) + Item("u-1", "Extra", LocalDate.of(2027, 2, 3), done = true, custom = true)
        assertEquals(c, fromJson(toJson(c)))
    }
}
