package gt.edu.umg.finanzas.app.ui.operaciones

import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import java.time.LocalDate

/**
 * Elemento de la lista de Operaciones. Las dos subclases son los dos tipos de vista del adaptador:
 * el encabezado de un día y la fila de un movimiento.
 */
sealed class FilaOperacion {

    data class EncabezadoDia(val fecha: LocalDate) : FilaOperacion()

    data class Movimiento(val vista: MovimientoVista) : FilaOperacion()

    companion object {
        /**
         * El DAO entrega los movimientos de la fecha más reciente a la más antigua; se inserta un
         * encabezado cada vez que cambia el día.
         */
        fun agrupar(movimientos: List<MovimientoVista>): List<FilaOperacion> = buildList {
            var diaActual: LocalDate? = null
            for (movimiento in movimientos) {
                if (movimiento.fecha != diaActual) {
                    add(EncabezadoDia(movimiento.fecha))
                    diaActual = movimiento.fecha
                }
                add(Movimiento(movimiento))
            }
        }
    }
}
