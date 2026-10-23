package gt.edu.umg.finanzas.core.contratos

import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.TotalCategoria
import gt.edu.umg.finanzas.core.dominio.TotalesPeriodo
import gt.edu.umg.finanzas.core.dominio.Transaccion
import kotlinx.coroutines.flow.Flow

/**
 * Lecturas de movimientos. Todas reciben un [Periodo]: Inicio nunca carga el historial completo.
 * Los Flow vuelven a emitir después de cada guardado, así ninguna pantalla "refresca" a mano.
 */
interface RepositorioTransaccion {
    suspend fun obtenerPorId(id: Long): Transaccion?

    fun observarTotales(periodo: Periodo): Flow<TotalesPeriodo>

    /** Del mayor al menor total. */
    fun observarGastosPorCategoria(periodo: Periodo): Flow<List<TotalCategoria>>

    /** Ordenados por fecha descendente. [tipo] null significa ingresos y gastos. */
    fun observarMovimientos(periodo: Periodo, tipo: TipoMovimiento?): Flow<List<MovimientoVista>>
}
