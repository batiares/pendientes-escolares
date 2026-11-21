package com.escuela.pendientes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class LogicTest {
    private val nov = YearMonth.of(2026, 11)

    @Test fun fechaExplicitaDelDocumento() =
        assertEquals("Agenda inicial" to LocalDate.of(2026, 10, 1), dueFor("Agenda inicial (01 oct)", YearMonth.of(2026, 10)))

    @Test fun agendaInicialSaltaSuspension() =
        assertEquals(LocalDate.of(2026, 11, 3), dueFor("Agenda inicial", nov).second) // 1 dom, 2 suspensión

    @Test fun restoUltimoDiaHabil() =
        assertEquals(LocalDate.of(2026, 11, 30), dueFor("Incapacidades", nov).second)

    @Test fun noCaeEnVacaciones() {
        assertEquals(LocalDate.of(2026, 12, 18), dueFor("Incapacidades", YearMonth.of(2026, 12)).second)
        assertEquals(LocalDate.of(2027, 1, 7), dueFor("Agenda inicial", YearMonth.of(2027, 1)).second)
        assertEquals(LocalDate.of(2027, 3, 19), dueFor("Incapacidades", YearMonth.of(2027, 3)).second)
        assertEquals(LocalDate.of(2027, 7, 9), dueFor("Acto académico", YearMonth.of(2027, 7)).second)
    }

    @Test fun calendarioSep() {
        val all = sepCalendar.values.flatten()
        assertEquals(8, all.count { it.type == Sep.CTE })
        assertEquals(9, all.count { it.type == Sep.SUSPENSION })
        assertEquals(LocalDate.of(2026, 8, 31), all.single { it.type == Sep.INICIO }.date)
        assertEquals(LocalDate.of(2027, 7, 9), all.single { it.type == Sep.FIN }.date)
        assertTrue(isOffDay(LocalDate.of(2026, 12, 23)))
        assertFalse(isOffDay(LocalDate.of(2026, 11, 27))) // CTE: sin clases, pero la Dirección trabaja
    }

    @Test fun recordatorioSoloDentroDeDosDias() {
        val item = Item("x", "Reporte", LocalDate.of(2026, 11, 30))
        assertFalse(needsReminder(item, LocalDate.of(2026, 11, 27)))
        assertTrue(needsReminder(item, LocalDate.of(2026, 11, 28)))
        assertTrue(needsReminder(item, LocalDate.of(2026, 11, 30)))
        assertFalse(needsReminder(item, LocalDate.of(2026, 12, 1)))
        assertFalse(needsReminder(item.copy(done = true), LocalDate.of(2026, 11, 29)))
        assertFalse(needsReminder(item.copy(kind = Kind.EVENT), LocalDate.of(2026, 11, 29)))
    }

    @Test fun alarmaDeEventos() {
        val ev = Item("e", "Festival", LocalDate.of(2026, 12, 11), kind = Kind.EVENT, time = LocalTime.of(10, 0), remind = 60)
        assertEquals(LocalDateTime.of(2026, 12, 11, 9, 0), alarmAt(ev))
        assertEquals(LocalDateTime.of(2026, 12, 10, 8, 0), alarmAt(ev.copy(time = null, remind = 1440)))
        assertNull(alarmAt(ev.copy(remind = null)))
        assertNull(alarmAt(ev.copy(kind = Kind.DOC)))
    }

    @Test fun catalogoSinIdsRepetidosYDentroDelCiclo() {
        val c = catalog()
        assertEquals(c.size, c.map { it.id }.toSet().size)
        assertTrue(c.all { it.due >= LocalDate.of(2026, 8, 1) && it.due <= LocalDate.of(2027, 7, 31) })
    }

    @Test fun jsonIdaYVuelta() {
        val c = catalog().take(3) + Item("u-1", "Extra", LocalDate.of(2027, 2, 3), done = true, custom = true) +
            Item("u-2", "Junta", LocalDate.of(2027, 2, 4), custom = true, kind = Kind.REMINDER, time = LocalTime.of(13, 30), remind = 15, notes = "Sala")
        assertEquals(c, fromJson(toJson(c)))
    }

    @Test fun leeLoGuardadoPorLaVersionAnterior() {
        val old = """[{"id":"u-1","title":"Extra","due":"2027-02-03","done":true,"custom":true}]"""
        assertEquals(listOf(Item("u-1", "Extra", LocalDate.of(2027, 2, 3), done = true, custom = true)), fromJson(old))
    }
}
