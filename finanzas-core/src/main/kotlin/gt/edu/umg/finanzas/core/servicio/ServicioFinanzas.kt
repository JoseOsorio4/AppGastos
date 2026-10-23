package gt.edu.umg.finanzas.core.servicio

import gt.edu.umg.finanzas.core.contratos.OperacionesMovimiento
import gt.edu.umg.finanzas.core.contratos.ReconstructorSaldo
import gt.edu.umg.finanzas.core.contratos.RepositorioCategoria
import gt.edu.umg.finanzas.core.contratos.RepositorioTransaccion
import gt.edu.umg.finanzas.core.contratos.RepositorioUsuario
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.ResumenPeriodo
import gt.edu.umg.finanzas.core.dominio.SegmentoGasto
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.reglas.DistribucionGastos
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Única entrada de los ViewModels al core. Recibe los contratos ya construidos (Room en la app,
 * dobles en memoria en las pruebas) y no sabe cómo se guardan los datos.
 */
class ServicioFinanzas(
    private val usuarios: RepositorioUsuario,
    private val categorias: RepositorioCategoria,
    private val transacciones: RepositorioTransaccion,
    private val operaciones: OperacionesMovimiento,
    private val reconstructor: ReconstructorSaldo,
) {

    /** Saldo histórico del usuario combinado con las sumas del intervalo. */
    fun observarResumen(periodo: Periodo): Flow<ResumenPeriodo> =
        combine(usuarios.observarUsuario(), transacciones.observarTotales(periodo)) { usuario, totales ->
            ResumenPeriodo(
                saldoDisponibleCentavos = usuario.saldoCentavos,
                ingresosCentavos = totales.ingresosCentavos,
                gastosCentavos = totales.gastosCentavos,
            )
        }

    fun observarDistribucionGastos(periodo: Periodo): Flow<List<SegmentoGasto>> =
        transacciones.observarGastosPorCategoria(periodo).map(DistribucionGastos::calcular)

    fun observarMovimientos(periodo: Periodo, tipo: TipoMovimiento?): Flow<List<MovimientoVista>> =
        transacciones.observarMovimientos(periodo, tipo)

    fun observarCategorias(tipo: TipoMovimiento): Flow<List<Categoria>> =
        categorias.observarActivasPorTipo(tipo)

    suspend fun obtenerParaEditar(id: Long): Resultado<Transaccion> =
        try {
            transacciones.obtenerPorId(id)?.let { Resultado.Ok(it) } ?: Resultado.NoEncontrado
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.ErrorAlmacenamiento(e)
        }

    suspend fun registrar(datos: DatosMovimiento, hoy: LocalDate): Resultado<Long> =
        operaciones.crear(datos, hoy)

    suspend fun editar(id: Long, datos: DatosMovimiento, hoy: LocalDate): Resultado<Unit> =
        operaciones.editar(id, datos, hoy)

    suspend fun eliminar(id: Long): Resultado<Unit> = operaciones.eliminar(id)

    suspend fun reconstruirSaldo(): Resultado<Long> = reconstructor.reconstruirSiPendiente()
}
