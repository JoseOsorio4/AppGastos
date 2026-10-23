package gt.edu.umg.finanzas.app.datos.local

import gt.edu.umg.finanzas.app.datos.local.dao.TransaccionDao
import gt.edu.umg.finanzas.core.contratos.RepositorioTransaccion
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.TotalCategoria
import gt.edu.umg.finanzas.core.dominio.TotalesPeriodo
import gt.edu.umg.finanzas.core.dominio.Transaccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Traduce el Periodo del core a días epoch y deja las sumas y agrupaciones al SQL del DAO. */
class RepositorioTransaccionRoom(private val dao: TransaccionDao) : RepositorioTransaccion {

    override suspend fun obtenerPorId(id: Long): Transaccion? = dao.obtenerPorId(id)?.aDominio()

    override fun observarTotales(periodo: Periodo): Flow<TotalesPeriodo> =
        dao.observarTotales(periodo.desde.toEpochDay(), periodo.hasta.toEpochDay())

    override fun observarGastosPorCategoria(periodo: Periodo): Flow<List<TotalCategoria>> =
        dao.observarGastosPorCategoria(periodo.desde.toEpochDay(), periodo.hasta.toEpochDay())

    override fun observarMovimientos(periodo: Periodo, tipo: TipoMovimiento?): Flow<List<MovimientoVista>> =
        dao.observarMovimientos(periodo.desde.toEpochDay(), periodo.hasta.toEpochDay(), tipo)
            .map { filas -> filas.map { it.aVista() } }
}
