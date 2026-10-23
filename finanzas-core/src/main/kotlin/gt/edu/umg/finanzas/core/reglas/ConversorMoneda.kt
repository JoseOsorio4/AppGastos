package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Las dos únicas conversiones: USD → GTQ y GTQ → USD.
 * Se opera en BigDecimal y se redondea una sola vez, al final, con HALF_UP.
 * Lanza ArithmeticException si el resultado no cabe en un Long.
 */
object ConversorMoneda {

    /** US$ 12.34 con tasa 7.66285 → 1234 × 7.66285 = 9455.9569 → 9456 (Q 94.56). */
    fun usdAGtq(centavosUsd: Long, tasa: TasaCambio): Long =
        BigDecimal.valueOf(centavosUsd)
            .multiply(tasa.gtqPorUsd)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

    /** Q 100.00 con tasa 7.66285 → 10000 ÷ 7.66285 = 1304.997… → 1305 (US$ 13.05). */
    fun gtqAUsd(centavosGtq: Long, tasa: TasaCambio): Long =
        BigDecimal.valueOf(centavosGtq)
            .divide(tasa.gtqPorUsd, 0, RoundingMode.HALF_UP)
            .longValueExact()
}
