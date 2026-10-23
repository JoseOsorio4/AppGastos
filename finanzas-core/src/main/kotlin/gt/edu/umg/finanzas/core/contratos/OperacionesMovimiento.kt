package gt.edu.umg.finanzas.core.contratos

import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.resultado.Resultado
import java.time.LocalDate

/**
 * Límite atómico de las escrituras de movimientos. Cada método lee la categoría y el usuario,
 * valida con ValidadorMovimiento, comprueba el saldo final con CalculoSaldo.aplicar y escribe
 * el movimiento y el saldo juntos, o nada.
 */
interface OperacionesMovimiento {
    /** Devuelve el ID asignado. */
    suspend fun crear(datos: DatosMovimiento, hoy: LocalDate): Resultado<Long>

    /** Un ID inexistente da NoEncontrado; nunca inserta. */
    suspend fun editar(id: Long, datos: DatosMovimiento, hoy: LocalDate): Resultado<Unit>

    suspend fun eliminar(id: Long): Resultado<Unit>
}
