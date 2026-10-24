package gt.edu.umg.finanzas.app.ui.comun

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Fechas para mostrar en pantalla y para los selectores de Material, que trabajan en milisegundos UTC. */
object Fechas {

    private val FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun texto(fecha: LocalDate): String = fecha.format(FORMATO)

    // MaterialDatePicker representa cada día como la medianoche UTC; así no se corre un día por la zona.
    fun aMilisUtc(fecha: LocalDate): Long = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun desdeMilisUtc(milis: Long): LocalDate = Instant.ofEpochMilli(milis).atZone(ZoneOffset.UTC).toLocalDate()
}
