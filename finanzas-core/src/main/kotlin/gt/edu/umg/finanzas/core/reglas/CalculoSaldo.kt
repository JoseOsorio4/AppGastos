package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.resultado.Resultado

/**
 * Cuánto cambia el saldo al crear, editar o eliminar, y el saldo final comprobado.
 * El signo lo decide cada subclase con impactoEnSaldo(); aquí no se pregunta si es gasto.
 */
object CalculoSaldo {

    const val MENSAJE_DESBORDE = "El saldo resultante supera el límite permitido"

    fun deltaCrear(nueva: Transaccion): Long = nueva.impactoEnSaldo()

    // Cubre cambios de monto y de tipo: gasto Q 300 → ingreso Q 300 da +600.
    // Los montos están acotados por Transaccion, así que la resta no desborda.
    fun deltaEditar(anterior: Transaccion, nueva: Transaccion): Long =
        nueva.impactoEnSaldo() - anterior.impactoEnSaldo()

    fun deltaEliminar(anterior: Transaccion): Long = -anterior.impactoEnSaldo()

    /**
     * Saldo final que se escribirá. Math.addExact detecta el desborde antes de cualquier escritura;
     * por eso Room guarda este valor y no un `UPDATE saldo = saldo + delta`.
     */
    fun aplicar(saldoActual: Long, delta: Long): Resultado<Long> =
        try {
            Resultado.Ok(Math.addExact(saldoActual, delta))
        } catch (e: ArithmeticException) {
            Resultado.ErrorNegocio(MENSAJE_DESBORDE)
        }

    /** Saldo desde cero recorriendo el historial; solo para la reconstrucción excepcional. */
    fun saldoDesde(transacciones: Sequence<Transaccion>): Resultado<Long> {
        var saldo = 0L
        for (transaccion in transacciones) {
            saldo = when (val siguiente = aplicar(saldo, transaccion.impactoEnSaldo())) {
                is Resultado.Ok -> siguiente.valor
                is Resultado.Fallo -> return siguiente
            }
        }
        return Resultado.Ok(saldo)
    }
}
