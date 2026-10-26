package gt.edu.umg.finanzas.app.ui.comun

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Fechas para mostrar en pantalla y para los selectores de Material, que trabajan en milisegundos UTC. */
object Fechas {

    private val FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    // Nombre del día en español aunque el teléfono esté en otro idioma: la app entera está en español.
    private val FORMATO_DIA = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", Locale.forLanguageTag("es-GT"))

    fun texto(fecha: LocalDate): String = fecha.format(FORMATO)

    /** "viernes 23/10/2026" → "Viernes 23/10/2026", para los encabezados de Operaciones. */
    fun textoDia(fecha: LocalDate): String = fecha.format(FORMATO_DIA).replaceFirstChar { it.uppercase() }

    // MaterialDatePicker representa cada día como la medianoche UTC; así no se corre un día por la zona.
    fun aMilisUtc(fecha: LocalDate): Long = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun desdeMilisUtc(milis: Long): LocalDate = Instant.ofEpochMilli(milis).atZone(ZoneOffset.UTC).toLocalDate()
}
