package com.escuela.pendientes

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

enum class Kind(val emoji: String, val label: String) {
    DOC("📄", "Documento"), EVENT("🎉", "Evento"), REMINDER("🔔", "Recordatorio"),
}

/**
 * Algo en el calendario. Los documentos avisan 2 días antes; eventos y recordatorios avisan
 * [remind] minutos antes de su hora (null = sin aviso).
 */
data class Item(
    val id: String, val title: String, val due: LocalDate, val done: Boolean = false, val custom: Boolean = false,
    val kind: Kind = Kind.DOC, val time: LocalTime? = null, val remind: Int? = null, val notes: String = "",
)

/** Días antes de la entrega en que empiezan los recordatorios (2, 1 y el mismo día). */
const val REMIND_DAYS = 2

/** Hora del aviso para lo que no tiene hora (todo el día). */
val ALL_DAY_HOUR: LocalTime = LocalTime.of(8, 0)

fun daysLeft(item: Item, today: LocalDate): Long = item.due.toEpochDay() - today.toEpochDay()

fun needsReminder(item: Item, today: LocalDate): Boolean =
    item.kind == Kind.DOC && !item.done && daysLeft(item, today) in 0..REMIND_DAYS

/** Momento exacto del aviso de un evento o recordatorio; null si no lleva. */
fun alarmAt(item: Item): LocalDateTime? =
    if (item.kind == Kind.DOC || item.remind == null) null
    else item.due.atTime(item.time ?: ALL_DAY_HOUR).minusMinutes(item.remind.toLong())

// Días hábiles según el calendario SEP: se saltan fines de semana, suspensiones, vacaciones y receso.
private fun firstWeekday(ym: YearMonth): LocalDate =
    generateSequence(ym.atDay(1)) { it.plusDays(1) }.first { !isOffDay(it) }

private fun lastWeekday(ym: YearMonth): LocalDate =
    generateSequence(ym.atEndOfMonth()) { it.minusDays(1) }.first { !isOffDay(it) }

private val explicitDay = Regex("""\s*\((\d{1,2}) [a-zé]+\)""")

/**
 * Fecha de entrega: la que trae el documento entre paréntesis, "Agenda inicial" el primer día hábil
 * del mes y todo lo demás el último día hábil del mes (según el calendario SEP).
 */
fun dueFor(rawTitle: String, ym: YearMonth): Pair<String, LocalDate> {
    explicitDay.find(rawTitle)?.let { m ->
        return rawTitle.replace(m.value, "").trim() to ym.atDay(m.groupValues[1].toInt())
    }
    val title = rawTitle.trim()
    return title to if (title.startsWith("Agenda inicial", ignoreCase = true)) firstWeekday(ym) else lastWeekday(ym)
}

/** Lista "Relación de documentos por entregar cada mes", ciclo escolar 2026-2027. */
private val months: List<Pair<YearMonth, List<String>>> = listOf(
    // El documento junta agosto y septiembre: se entregan al cierre de septiembre.
    YearMonth.of(2026, 9) to listOf(
        "Agenda inicial (fase intensiva)", "Plantilla docente", "Diagnóstico de infraestructura", "Comité de planeación",
        "Convocatoria 1ª asamblea general", "Constitución de AMPF", "Plan de trabajo AMPF", "Traspaso económico AMPF",
        "Constitución comité de Seguridad y Emergencia Escolar", "Diagnóstico de riesgos", "Directorios de emergencia",
        "Constitución comité de convivencia", "Acuerdos escolares de convivencia (escuela, aula y personal)",
        "Cartas compromiso (padres y personal de la escuela)", "Capacitación a mesa directiva", "Reporte de incidencias",
        "Agenda realizada", "Campaña de concientización sobre el abuso sexual", "Llenado del instrumento de evaluación",
    ),
    YearMonth.of(2026, 10) to listOf(
        "Agenda inicial (01 oct)", "Presentación de los resultados del diagnóstico", "Narrativa del diagnóstico socioeducativo",
        "Informes financieros ciclo anterior sellados por DRSE – mesa directiva",
        "Informes financieros ciclo anterior sellados por DRSE – CAS", "Constitución del comité de CAS (ciclo actual)",
        "Acta de la 1ª reunión del comité de CAS (elección del prestador de servicios)",
        "1ª reunión comité de convivencia: plan de trabajo (acciones preventivas contra la violencia)",
        "Programa analítico", "Programa de mejora continua", "911, inmueble y CETAAD",
        "Relación de alumnos canalizados y motivo", "Relación de alumnos con bajo desempeño",
        "Relación de alumnos con ausentismo", "Constitución CEPS", "Reporte de incidencias", "Incapacidades",
        "Actualización del programa interno de protección civil", "Plan de acompañamiento a docentes (primeras acciones)",
        "Agenda final",
    ),
    YearMonth.of(2026, 11) to listOf(
        "Agenda inicial", "Reporte de visitas a las aulas (2 mínimo)", "Seguimiento al plan de trabajo de la mesa directiva",
        "Plan interno de protección civil (por brigada y por siniestros)",
        "Resultados de aprendizaje de los alumnos (Excel, primer corte)", "Seguimiento a los alumnos canalizados",
        "Atención a los alumnos con ausentismo", "Seguimiento al PMC", "Seguimiento al PA",
        "Avances de acciones de acompañamiento a docentes", "Revisión de cuentas APF", "Revisión de cuentas CAS",
        "Reporte de incidencias", "Avances en la atención a las necesidades de la escuela", "Incapacidades",
        "Agenda final", "1ª acta de CEPS",
    ),
    YearMonth.of(2026, 12) to listOf(
        "Agenda inicial (02 dic)", "Rendición de cuentas (mesa directiva, CAS y comité de planeación)",
        "Reporte de visitas (mínimo 2)", "Seguimiento al plan de trabajo mesa directiva",
        "Seguimiento a las acciones de acompañamiento", "Revisión del estado físico del plantel",
        "Acciones preventivas contra la violencia", "Acciones inclusivas", "Reporte de incidencias", "Incapacidades",
        "Agenda realizada",
    ),
    YearMonth.of(2027, 1) to listOf(
        "Agenda inicial", "2ª participación social", "Reporte de visitas al aula",
        "Seguimiento programa de acompañamiento a docentes", "Seguimiento a los alumnos canalizados",
        "Seguimiento al programa interno", "Seguimiento al plan de trabajo mesa directiva", "Reporte de incidencias",
        "Incapacidades", "Agenda final", "Revisión de los libros de CAS y APF",
    ),
    YearMonth.of(2027, 2) to listOf(
        "Agenda inicial", "Reporte de preinscripción final", "Seguimiento alumnos con ausentismo",
        "Revisión en DRSE 1er semestre AMPF", "Revisión en DRSE 1er semestre CAS", "Reporte de visita al aula",
        "Reporte de incidencias", "Incapacidades", "Agenda final",
    ),
    YearMonth.of(2027, 3) to listOf(
        "Agenda inicial (03 mar)", "Reporte de visitas a las aulas", "Seguimiento al PA", "Seguimiento al PMC",
        "Balance del programa de acompañamiento a docentes", "Rendición de cuentas 2º corte",
        "Rendición de cuentas CAS, AMPF y comité de planeación", "Llenado del instrumento de evaluación 2º corte",
        "Reporte de incidencias", "Incapacidades", "Agenda final",
    ),
    YearMonth.of(2027, 4) to listOf("Agenda inicial (01 abril)", "Reporte de incidencias", "Incapacidades", "Agenda"),
    YearMonth.of(2027, 5) to listOf(
        "Agenda inicial", "2 reportes de visitas al aula", "Reporte de incidencias", "Agenda final (30 mayo)",
        "Incapacidades", "Agenda realizada",
    ),
    YearMonth.of(2027, 6) to listOf(
        "Agenda inicial", "3er corte de evaluación", "Reporte de visitas al aula",
        "Evaluación de acciones preventivas contra la violencia", "Evaluación de acciones inclusivas",
        "Reporte de incidencias", "Incapacidades", "Agenda realizada", "Llenado del instrumento de evaluación 3er corte",
    ),
    YearMonth.of(2027, 7) to listOf(
        "Agenda del mes", "Rendición de cuentas", "Guardias intendentes", "Resguardo de expedientes (docentes)",
        "Resguardo de material", "Actas de entrega-recepción", "Evaluación final de zona", "Acto académico",
        "Taller intensivo",
    ),
)

fun catalog(): List<Item> = months.flatMap { (ym, titles) ->
    titles.map { raw -> dueFor(raw, ym).let { (t, d) -> Item("c-$ym-$raw", t, d) } }
}

fun toJson(items: List<Item>): String = JSONArray(items.map {
    JSONObject().put("id", it.id).put("title", it.title).put("due", it.due.toString())
        .put("done", it.done).put("custom", it.custom).put("kind", it.kind.name)
        .put("time", it.time?.toString()).put("remind", it.remind).put("notes", it.notes)
}).toString()

// Los campos nuevos son opcionales para no perder lo que ya estaba guardado en el celular.
fun fromJson(s: String): List<Item> = JSONArray(s).let { a ->
    (0 until a.length()).map { i ->
        a.getJSONObject(i).run {
            Item(
                getString("id"), getString("title"), LocalDate.parse(getString("due")), getBoolean("done"), optBoolean("custom"),
                Kind.valueOf(optString("kind", "DOC")), optString("time").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                if (has("remind")) getInt("remind") else null, optString("notes"),
            )
        }
    }
}
