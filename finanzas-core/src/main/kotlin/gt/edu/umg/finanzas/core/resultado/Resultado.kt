package gt.edu.umg.finanzas.core.resultado

import gt.edu.umg.finanzas.core.dominio.CampoMovimiento

/**
 * Respuesta de una regla o de una operación de almacenamiento.
 * Los fallos heredan de [Fallo] para que quien llama los propague con un solo `is Fallo -> return`.
 */
sealed class Resultado<out T> {

    data class Ok<out T>(val valor: T) : Resultado<T>()

    sealed class Fallo : Resultado<Nothing>()

    /** Dato rechazado por una regla. [campo] indica qué control debe marcar la pantalla. */
    data class ErrorNegocio(
        val mensaje: String,
        val campo: CampoMovimiento? = null,
    ) : Fallo()

    /** El movimiento pedido no existe; nunca se inserta uno nuevo en su lugar. */
    data object NoEncontrado : Fallo()

    /** Falló la base de datos; la transacción ya se revirtió. */
    data class ErrorAlmacenamiento(val causa: Exception) : Fallo()
}
