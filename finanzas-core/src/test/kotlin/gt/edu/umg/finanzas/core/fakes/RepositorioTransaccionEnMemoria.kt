package gt.edu.umg.finanzas.core.fakes

import gt.edu.umg.finanzas.core.contratos.RepositorioTransaccion
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.TotalCategoria
import gt.edu.umg.finanzas.core.dominio.TotalesPeriodo
import gt.edu.umg.finanzas.core.dominio.Transaccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Hace en memoria lo que en Room harán las consultas SUM y GROUP BY del DAO. */
class RepositorioTransaccionEnMemoria(private val almacen: AlmacenEnMemoria) : RepositorioTransaccion {

    override suspend fun obtenerPorId(id: Long): Transaccion? = almacen.estado.value.transacciones[id]

    override fun observarTotales(periodo: Periodo): Flow<TotalesPeriodo> =
        almacen.estado.map { estado ->
            val delPeriodo = estado.transacciones.values.filter { periodo.contiene(it.fecha) }
            TotalesPeriodo(
                ingresosCentavos = delPeriodo.sumaDe(TipoMovimiento.INGRESO),
                gastosCentavos = delPeriodo.sumaDe(TipoMovimiento.GASTO),
            )
        }

    override fun observarGastosPorCategoria(periodo: Periodo): Flow<List<TotalCategoria>> =
        almacen.estado.map { estado ->
            estado.transacciones.values
                .filter { it.tipo == TipoMovimiento.GASTO && periodo.contiene(it.fecha) }
                .groupBy { it.idCategoria }
                .map { (idCategoria, gastos) ->
                    val categoria = checkNotNull(estado.categoria(idCategoria))
                    TotalCategoria(categoria.clave, categoria.nombre, gastos.sumOf { it.montoCentavos })
                }
                .sortedByDescending { it.totalCentavos }
        }

    override fun observarMovimientos(periodo: Periodo, tipo: TipoMovimiento?): Flow<List<MovimientoVista>> =
        almacen.estado.map { estado ->
            estado.transacciones.values
                .filter { periodo.contiene(it.fecha) && (tipo == null || it.tipo == tipo) }
                .sortedWith(compareByDescending<Transaccion> { it.fecha }.thenByDescending { it.idTransaccion })
                .map { transaccion ->
                    val categoria = checkNotNull(estado.categoria(transaccion.idCategoria))
                    MovimientoVista(
                        idTransaccion = transaccion.idTransaccion,
                        tipo = transaccion.tipo,
                        montoCentavos = transaccion.montoCentavos,
                        descripcion = transaccion.descripcion,
                        fecha = transaccion.fecha,
                        claveCategoria = categoria.clave,
                        nombreCategoria = categoria.nombre,
                        conversion = transaccion.conversion,
                    )
                }
        }

    private fun List<Transaccion>.sumaDe(tipo: TipoMovimiento): Long =
        filter { it.tipo == tipo }.fold(0L) { suma, transaccion -> Math.addExact(suma, transaccion.montoCentavos) }
}
