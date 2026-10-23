package gt.edu.umg.finanzas.core.dominio

/**
 * Un arco del anillo de gastos. [puntosBasicos] va de 0 a 10 000 (100.00 %);
 * se usan enteros para que la suma de todos los segmentos sea exacta.
 */
data class SegmentoGasto(
    val clave: String,
    val nombre: String,
    val totalCentavos: Long,
    val puntosBasicos: Int,
)
