package gt.edu.umg.finanzas.core.fakes

import gt.edu.umg.finanzas.core.contratos.OperacionesMovimiento
import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.reglas.CalculoSaldo
import gt.edu.umg.finanzas.core.reglas.ValidadorMovimiento
import gt.edu.umg.finanzas.core.resultado.Resultado
import java.time.LocalDate

/**
 * Sigue el mismo orden que tendrá OperacionesMovimientoRoom: leer, validar, comprobar el saldo
 * final y escribir todo junto. Usa las reglas reales; no repite ningún cálculo.
 */
class OperacionesMovimientoEnMemoria(private val almacen: AlmacenEnMemoria) : OperacionesMovimiento {

    override suspend fun crear(datos: DatosMovimiento, hoy: LocalDate): Resultado<Long> {
        val actual = almacen.estado.value
        val categoria = actual.categoria(datos.idCategoria) ?: return categoriaInexistente()
        val nueva = when (val validacion = ValidadorMovimiento.validar(datos, categoria, hoy)) {
            is Resultado.Ok -> validacion.valor
            is Resultado.Fallo -> return validacion
        }
        val saldoNuevo = when (val saldo = CalculoSaldo.aplicar(actual.usuario.saldoCentavos, CalculoSaldo.deltaCrear(nueva))) {
            is Resultado.Ok -> saldo.valor
            is Resultado.Fallo -> return saldo
        }

        val id = actual.siguienteId
        almacen.estado.value = actual.copy(
            usuario = actual.usuario.copy(saldoCentavos = saldoNuevo),
            transacciones = actual.transacciones + (id to nueva.conId(id)),
            siguienteId = id + 1,
        )
        return Resultado.Ok(id)
    }

    override suspend fun editar(id: Long, datos: DatosMovimiento, hoy: LocalDate): Resultado<Unit> {
        val actual = almacen.estado.value
        val anterior = actual.transacciones[id] ?: return Resultado.NoEncontrado
        val categoria = actual.categoria(datos.idCategoria) ?: return categoriaInexistente()
        val nueva = when (val validacion = ValidadorMovimiento.validar(datos, categoria, hoy, anterior.idCategoria)) {
            is Resultado.Ok -> validacion.valor
            is Resultado.Fallo -> return validacion
        }
        val delta = CalculoSaldo.deltaEditar(anterior, nueva)
        val saldoNuevo = when (val saldo = CalculoSaldo.aplicar(actual.usuario.saldoCentavos, delta)) {
            is Resultado.Ok -> saldo.valor
            is Resultado.Fallo -> return saldo
        }

        almacen.estado.value = actual.copy(
            usuario = actual.usuario.copy(saldoCentavos = saldoNuevo),
            transacciones = actual.transacciones + (id to nueva.conId(id)),
        )
        return Resultado.Ok(Unit)
    }

    override suspend fun eliminar(id: Long): Resultado<Unit> {
        val actual = almacen.estado.value
        val anterior = actual.transacciones[id] ?: return Resultado.NoEncontrado
        val saldoNuevo = when (val saldo = CalculoSaldo.aplicar(actual.usuario.saldoCentavos, CalculoSaldo.deltaEliminar(anterior))) {
            is Resultado.Ok -> saldo.valor
            is Resultado.Fallo -> return saldo
        }

        almacen.estado.value = actual.copy(
            usuario = actual.usuario.copy(saldoCentavos = saldoNuevo),
            transacciones = actual.transacciones - id,
        )
        return Resultado.Ok(Unit)
    }

    private fun categoriaInexistente() = Resultado.ErrorNegocio("Categoría inexistente", CampoMovimiento.CATEGORIA)
}
