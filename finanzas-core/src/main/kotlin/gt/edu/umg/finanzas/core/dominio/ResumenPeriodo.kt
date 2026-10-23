package gt.edu.umg.finanzas.core.dominio

/**
 * Datos de las tarjetas de Inicio. El saldo disponible es histórico y no depende del período;
 * el resultado sí (ingresos − gastos del intervalo).
 */
data class ResumenPeriodo(
    val saldoDisponibleCentavos: Long,
    val ingresosCentavos: Long,
    val gastosCentavos: Long,
) {
    val resultadoCentavos: Long get() = ingresosCentavos - gastosCentavos
}
