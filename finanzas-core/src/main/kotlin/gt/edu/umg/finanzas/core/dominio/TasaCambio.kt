package gt.edu.umg.finanzas.core.dominio

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Quetzales por 1 USD. Se guarda como BigDecimal sin recortar para no perder decimales del proveedor.
 * [fechaReferencia] es la fecha que informa el proveedor, no el momento de la consulta.
 */
data class TasaCambio(
    val gtqPorUsd: BigDecimal,
    val fechaReferencia: LocalDate,
    val fuente: String,
) {
    init {
        require(gtqPorUsd.signum() > 0) { "La tasa debe ser mayor que 0: $gtqPorUsd" }
        require(fuente.isNotBlank()) { "La tasa necesita una fuente" }
    }
}
