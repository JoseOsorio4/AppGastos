package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.ConversionAplicada
import gt.edu.umg.finanzas.core.dominio.DesgloseConversion
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Las dos únicas conversiones: USD → GTQ y GTQ → USD.
 * Se opera en BigDecimal y se redondea una sola vez, al final, con HALF_UP.
 * Lanza ArithmeticException si el resultado no cabe en un Long.
 */
object ConversorMoneda {

    val RECARGO_ESTIMADO_GASTO_USD = BigDecimal("0.30")

    fun recargoPara(tipo: TipoMovimiento, tipoAnterior: TipoMovimiento? = null,
        recargoAnterior: BigDecimal? = null): BigDecimal = when {
        tipo == TipoMovimiento.INGRESO -> BigDecimal.ZERO
        tipo == tipoAnterior && recargoAnterior != null -> recargoAnterior
        else -> RECARGO_ESTIMADO_GASTO_USD
    }

    fun tasaAplicada(oficial: BigDecimal, recargo: BigDecimal): BigDecimal = oficial.add(recargo)

    fun usdAGtq(conversion: ConversionAplicada): Long = convertirUsd(conversion.montoOriginalCentavos,
        tasaAplicada(conversion.tasa.gtqPorUsd, conversion.recargoGtqPorUsd))

    fun desglosar(conversion: ConversionAplicada, totalGuardado: Long? = null): DesgloseConversion {
        val total = totalGuardado ?: usdAGtq(conversion)
        val referencia = usdAGtq(conversion.montoOriginalCentavos, conversion.tasa)
        return DesgloseConversion(referencia, Math.subtractExact(total, referencia), total)
    }

    /** US$ 12.34 con tasa 7.66285 → 1234 × 7.66285 = 9455.9569 → 9456 (Q 94.56). */
    fun usdAGtq(centavosUsd: Long, tasa: TasaCambio): Long = convertirUsd(centavosUsd, tasa.gtqPorUsd)

    private fun convertirUsd(centavosUsd: Long, tasa: BigDecimal): Long =
        BigDecimal.valueOf(centavosUsd)
            .multiply(tasa)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()

    /** Q 100.00 con tasa 7.66285 → 10000 ÷ 7.66285 = 1304.997… → 1305 (US$ 13.05). */
    fun gtqAUsd(centavosGtq: Long, tasa: TasaCambio): Long =
        BigDecimal.valueOf(centavosGtq)
            .divide(tasa.gtqPorUsd, 0, RoundingMode.HALF_UP)
            .longValueExact()
}
