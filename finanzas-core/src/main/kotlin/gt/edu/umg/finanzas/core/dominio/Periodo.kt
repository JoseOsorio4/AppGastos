package gt.edu.umg.finanzas.core.dominio

import gt.edu.umg.finanzas.core.resultado.Resultado
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Intervalo de fechas, con [desde] y [hasta] incluidos. */
data class Periodo(val desde: LocalDate, val hasta: LocalDate) {

    init {
        require(!desde.isAfter(hasta)) { "Periodo invertido: $desde > $hasta" }
    }

    fun contiene(fecha: LocalDate): Boolean = !fecha.isBefore(desde) && !fecha.isAfter(hasta)

    companion object {
        // Zona y primer día fijos: "hoy" y la semana no dependen de la configuración del teléfono.
        val ZONA_GUATEMALA: ZoneId = ZoneId.of("America/Guatemala")
        val PRIMER_DIA_SEMANA: DayOfWeek = DayOfWeek.SUNDAY

        /** Fecha actual en Guatemala. El reloj se recibe para poder probarlo. */
        fun hoy(reloj: Clock = Clock.systemUTC()): LocalDate =
            LocalDate.now(reloj.withZone(ZONA_GUATEMALA))

        fun de(filtro: FiltroPeriodo, hoy: LocalDate): Periodo = when (filtro) {
            FiltroPeriodo.DIA -> Periodo(hoy, hoy)
            FiltroPeriodo.SEMANA -> {
                val domingo = hoy.with(TemporalAdjusters.previousOrSame(PRIMER_DIA_SEMANA))
                Periodo(domingo, domingo.plusDays(6))
            }
            FiltroPeriodo.MES -> Periodo(hoy.withDayOfMonth(1), hoy.with(TemporalAdjusters.lastDayOfMonth()))
            FiltroPeriodo.ANIO -> Periodo(hoy.withDayOfYear(1), hoy.with(TemporalAdjusters.lastDayOfYear()))
        }

        /** Rango elegido por el usuario; se rechaza con mensaje en lugar de lanzar. */
        fun personalizado(desde: LocalDate, hasta: LocalDate): Resultado<Periodo> =
            if (desde.isAfter(hasta)) {
                Resultado.ErrorNegocio("La fecha inicial no puede ser posterior a la final")
            } else {
                Resultado.Ok(Periodo(desde, hasta))
            }
    }
}
