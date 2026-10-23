package gt.edu.umg.finanzas.core.dominio

/**
 * Fotografía de la conversión de un movimiento USD: monto original y tasa histórica.
 * Una tasa nueva no la modifica, así el saldo nunca cambia por el tipo de cambio.
 */
data class ConversionAplicada(
    val montoOriginalCentavos: Long,
    val tasa: TasaCambio,
) {
    init {
        require(montoOriginalCentavos > 0) { "El monto original debe ser positivo" }
    }
}
