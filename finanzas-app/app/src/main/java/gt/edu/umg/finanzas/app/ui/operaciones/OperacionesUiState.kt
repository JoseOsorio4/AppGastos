package gt.edu.umg.finanzas.app.ui.operaciones

import gt.edu.umg.finanzas.app.ui.comun.SeleccionPeriodo
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento

/** Filtros activos de la lista. [tipo] null significa ingresos y gastos. */
data class FiltrosOperaciones(
    val seleccion: SeleccionPeriodo,
    val tipo: TipoMovimiento?,
)

/** Avisos de una sola vez tras eliminar; la Activity los muestra y llama a avisoMostrado(). */
enum class AvisoOperaciones { ELIMINADO, NO_SE_PUDO_ELIMINAR }

/** Todo lo que OperacionesActivity dibuja. */
data class OperacionesUiState(
    val filtros: FiltrosOperaciones,
    val filas: List<FilaOperacion> = emptyList(),
    val cargando: Boolean = true,
    val aviso: AvisoOperaciones? = null,
) {
    /** El movimiento que muestran el detalle y la confirmación; null si ya no está en la lista. */
    fun movimiento(idTransaccion: Long): MovimientoVista? =
        filas.filterIsInstance<FilaOperacion.Movimiento>()
            .firstOrNull { it.vista.idTransaccion == idTransaccion }
            ?.vista
}
