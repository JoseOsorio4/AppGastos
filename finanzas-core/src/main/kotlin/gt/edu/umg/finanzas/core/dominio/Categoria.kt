package gt.edu.umg.finanzas.core.dominio

/**
 * Fila de la tabla `Categoria`. [clave] es el identificador estable (sin tildes) y [nombre] el texto visible.
 * El [tipo] no cambia nunca; una categoría archivada ([activa] = false) no se ofrece al elegir.
 */
data class Categoria(
    val idCategoria: Long,
    val clave: String,
    val nombre: String,
    val tipo: TipoMovimiento,
    val activa: Boolean = true,
)
