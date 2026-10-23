package gt.edu.umg.finanzas.core.contratos

import gt.edu.umg.finanzas.core.resultado.Resultado

/**
 * Recalcula el saldo desde el historial solo si `reconstruccionPendiente` está encendida.
 * Asigna el saldo calculado (no lo suma) y apaga la bandera en la misma operación atómica.
 */
interface ReconstructorSaldo {
    /** Devuelve el saldo vigente; repetir la llamada después del éxito no cambia nada. */
    suspend fun reconstruirSiPendiente(): Resultado<Long>
}
