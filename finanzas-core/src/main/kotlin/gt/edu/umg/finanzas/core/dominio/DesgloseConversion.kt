package gt.edu.umg.finanzas.core.dominio

/** El recargo mostrado absorbe el ajuste al centavo para que ambas partes sumen el total. */
data class DesgloseConversion(
    val referenciaCentavos: Long,
    val recargoCentavos: Long,
    val totalCentavos: Long,
)
