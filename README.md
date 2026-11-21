# Pendientes de la Dirección (preescolar, ciclo 2026-2027)

App Android tipo calendario con el calendario escolar SEP 2026-2027 a colores, los documentos por entregar cada mes
y eventos o recordatorios propios. Los documentos avisan a diario (8 a. m. aprox.)
desde 2 días antes de cada entrega hasta el día de entrega, mientras no esté palomeado.

- Descarga: https://batiares.github.io/pendientes-escolares/
- Fechas: la que trae el documento; "Agenda inicial" el primer día hábil del mes; lo demás el último día hábil
  (se saltan suspensiones, vacaciones y receso del calendario SEP, `SepCalendar.kt`, DOF 15/07/2026).
  Se pueden cambiar en la app.
- Eventos y recordatorios avisan a su hora con AlarmManager y se reprograman al reiniciar el celular.
- Cada push a `main` compila, prueba y publica el APK en Releases (`.github/workflows/build.yml`).
