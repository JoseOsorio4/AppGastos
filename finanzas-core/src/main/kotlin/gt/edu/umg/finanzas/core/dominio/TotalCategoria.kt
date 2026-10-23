package gt.edu.umg.finanzas.core.dominio

/** Gasto total de una categoría en el intervalo, antes de calcular su porcentaje. */
data class TotalCategoria(
    val clave: String,
    val nombre: String,
    val totalCentavos: Long,
)
