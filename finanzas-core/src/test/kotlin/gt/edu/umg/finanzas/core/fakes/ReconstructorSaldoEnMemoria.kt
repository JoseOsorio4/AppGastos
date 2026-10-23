package gt.edu.umg.finanzas.core.fakes

import gt.edu.umg.finanzas.core.contratos.ReconstructorSaldo
import gt.edu.umg.finanzas.core.reglas.CalculoSaldo
import gt.edu.umg.finanzas.core.resultado.Resultado

class ReconstructorSaldoEnMemoria(private val almacen: AlmacenEnMemoria) : ReconstructorSaldo {

    override suspend fun reconstruirSiPendiente(): Resultado<Long> {
        val actual = almacen.estado.value
        // La condición es la bandera, nunca "saldo == 0".
        if (!actual.usuario.reconstruccionPendiente) return Resultado.Ok(actual.usuario.saldoCentavos)

        val saldo = when (val calculado = CalculoSaldo.saldoDesde(actual.transacciones.values.asSequence())) {
            is Resultado.Ok -> calculado.valor
            is Resultado.Fallo -> return calculado
        }
        // Se asigna el saldo calculado y se apaga la bandera en el mismo reemplazo.
        almacen.estado.value = actual.copy(
            usuario = actual.usuario.copy(saldoCentavos = saldo, reconstruccionPendiente = false),
        )
        return Resultado.Ok(saldo)
    }
}
