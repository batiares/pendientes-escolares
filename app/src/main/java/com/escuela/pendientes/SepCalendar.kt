package com.escuela.pendientes

import java.time.DayOfWeek
import java.time.LocalDate

/** Tipos de día del calendario oficial SEP. [strong]: se pinta el día lleno con letra blanca. */
enum class Sep(val emoji: String, val label: String, val short: String, val color: Long, val strong: Boolean, val noClasses: Boolean = false) {
    SUSPENSION("🚫", "Suspensión de labores (sin clases)", "Sin clases", 0xFFFF4D5A, true, true),
    CTE("👩‍🏫", "Consejo Técnico Escolar (sin clases)", "Consejo Técnico", 0xFFF06BA8, true, true),
    CTE_INTENSIVO("👩‍🏫", "Consejo Técnico fase intensiva", "CTE intensivo", 0xFF8E244D, true, true),
    VACACIONES("🏖️", "Vacaciones", "Vacaciones", 0xFF26B8D6, true, true),
    RECESO("🌴", "Receso de clases", "Receso", 0xFF43A55A, true, true),
    INICIO("🎒", "Inicio de clases", "Inicio de clases", 0xFF3A2E5C, true),
    FIN("🎓", "Fin de clases", "Fin de clases", 0xFF3A2E5C, true),
    CALIFICACIONES("📝", "Registro de calificaciones", "Calificaciones", 0xFFFF9F1C, false),
    RESULTADOS("📊", "Registro y comunicación de resultados", "Resultados", 0xFFF2B705, false),
    PREINSCRIPCION("🧒", "Preinscripción ciclo 2027-2028", "Preinscripción", 0xFF8BC34A, false),
    JORNADA("💜", "Jornada contra el abuso sexual y maltrato infantil", "Jornada", 0xFF7E57C2, false),
    CONMEMORATIVO("🇲🇽", "Día conmemorativo o de reflexión", "Conmemorativo", 0xFFFFC93C, false),
}

data class SepDay(val date: LocalDate, val type: Sep, val name: String)

private fun d(m: Int, day: Int) = LocalDate.of(if (m >= 8) 2026 else 2027, m, day)

private fun range(from: LocalDate, to: LocalDate, type: Sep, name: String = type.label, weekdays: Boolean = false) =
    generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }
        .filter { !weekdays || it.dayOfWeek < DayOfWeek.SATURDAY }
        .map { SepDay(it, type, name) }.toList()

/**
 * Calendario escolar 2026-2027 de preescolar, primaria y secundaria (185 días),
 * Acuerdo SEP 07/07/26, DOF 15 de julio de 2026.
 */
val sepCalendar: Map<LocalDate, List<SepDay>> = (
    range(d(8, 1), d(8, 23), Sep.RECESO) +
        range(d(8, 24), d(8, 28), Sep.CTE_INTENSIVO) +
        range(d(8, 29), d(8, 30), Sep.RECESO) +
        SepDay(d(8, 31), Sep.INICIO, "Inicio de clases del ciclo 2026-2027") +
        SepDay(d(9, 7), Sep.JORNADA, Sep.JORNADA.label) +
        SepDay(d(9, 15), Sep.CONMEMORATIVO, "Grito de Independencia") +
        SepDay(d(9, 16), Sep.SUSPENSION, "Día de la Independencia") +
        SepDay(d(9, 25), Sep.CTE, "Consejo Técnico Escolar") +
        SepDay(d(10, 30), Sep.CTE, "Consejo Técnico Escolar") +
        SepDay(d(11, 2), Sep.SUSPENSION, "Día de Muertos") +
        SepDay(d(11, 13), Sep.CALIFICACIONES, "Registro de calificaciones (1er periodo)") +
        SepDay(d(11, 16), Sep.SUSPENSION, "Aniversario de la Revolución Mexicana") +
        SepDay(d(11, 20), Sep.CONMEMORATIVO, "Aniversario de la Revolución Mexicana") +
        range(d(11, 23), d(11, 26), Sep.RESULTADOS) +
        SepDay(d(11, 27), Sep.CTE, "Consejo Técnico Escolar") +
        range(d(12, 21), d(12, 24), Sep.VACACIONES, "Vacaciones de invierno") +
        SepDay(d(12, 25), Sep.SUSPENSION, "Navidad") +
        range(d(12, 28), d(12, 31), Sep.VACACIONES, "Vacaciones de invierno") +
        SepDay(d(1, 1), Sep.SUSPENSION, "Año Nuevo") +
        range(d(1, 4), d(1, 5), Sep.VACACIONES, "Vacaciones de invierno") +
        SepDay(d(1, 6), Sep.SUSPENSION, "Suspensión de labores docentes") +
        SepDay(d(1, 29), Sep.CTE, "Consejo Técnico Escolar") +
        SepDay(d(2, 1), Sep.SUSPENSION, "Aniversario de la Constitución") +
        range(d(2, 2), d(2, 12), Sep.PREINSCRIPCION, weekdays = true) +
        SepDay(d(2, 5), Sep.CONMEMORATIVO, "Aniversario de la Constitución") +
        SepDay(d(2, 24), Sep.CONMEMORATIVO, "Día de la Bandera") +
        SepDay(d(2, 26), Sep.CTE, "Consejo Técnico Escolar") +
        SepDay(d(3, 5), Sep.CALIFICACIONES, "Registro de calificaciones (2º periodo)") +
        SepDay(d(3, 15), Sep.SUSPENSION, "Natalicio de Benito Juárez") +
        range(d(3, 16), d(3, 19), Sep.RESULTADOS) +
        SepDay(d(3, 19), Sep.CONMEMORATIVO, Sep.CONMEMORATIVO.label) +
        SepDay(d(3, 21), Sep.CONMEMORATIVO, "Natalicio de Benito Juárez") +
        range(d(3, 22), d(4, 2), Sep.VACACIONES, "Vacaciones de primavera", weekdays = true) +
        SepDay(d(4, 29), Sep.CONMEMORATIVO, Sep.CONMEMORATIVO.label) +
        SepDay(d(4, 30), Sep.CTE, "Consejo Técnico Escolar") +
        SepDay(d(5, 1), Sep.CONMEMORATIVO, "Día del Trabajo") +
        SepDay(d(5, 4), Sep.CONMEMORATIVO, Sep.CONMEMORATIVO.label) +
        SepDay(d(5, 5), Sep.SUSPENSION, "Batalla de Puebla") +
        SepDay(d(5, 15), Sep.CONMEMORATIVO, "Día del Maestro") +
        SepDay(d(5, 28), Sep.CTE, "Consejo Técnico Escolar") +
        SepDay(d(6, 25), Sep.CTE, "Consejo Técnico Escolar") +
        SepDay(d(7, 2), Sep.CALIFICACIONES, "Registro de calificaciones (3er periodo)") +
        SepDay(d(7, 8), Sep.RESULTADOS, Sep.RESULTADOS.label) +
        SepDay(d(7, 9), Sep.FIN, "Fin de clases del ciclo 2026-2027") +
        range(d(7, 10), d(7, 31), Sep.RECESO)
    ).groupBy { it.date }

/** Fin de semana o día en que la SEP no labora (suspensión, vacaciones o receso). */
fun isOffDay(date: LocalDate): Boolean = date.dayOfWeek >= DayOfWeek.SATURDAY ||
    sepCalendar[date].orEmpty().any { it.type == Sep.SUSPENSION || it.type == Sep.VACACIONES || it.type == Sep.RECESO }
