package gt.edu.umg.finanzas.core.dominio

/** Sumas del intervalo con campos nombrados, para no confundir ingresos con gastos. */
data class TotalesPeriodo(
    val ingresosCentavos: Long,
    val gastosCentavos: Long,
)
