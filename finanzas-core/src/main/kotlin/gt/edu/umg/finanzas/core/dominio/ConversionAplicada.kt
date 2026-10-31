package gt.edu.umg.finanzas.core.dominio

import java.math.BigDecimal

/**
 * Fotografía USD: monto original, referencia oficial y recargo estimado por USD de ese movimiento.
 * Una tasa nueva no la modifica, así el saldo nunca cambia por el tipo de cambio.
 */
data class ConversionAplicada(
    val montoOriginalCentavos: Long,
    val tasa: TasaCambio,
    val recargoGtqPorUsd: BigDecimal = BigDecimal.ZERO,
) {
    init {
        require(montoOriginalCentavos > 0) { "El monto original debe ser positivo" }
        require(recargoGtqPorUsd.signum() >= 0) { "El recargo no puede ser negativo" }
    }
}
