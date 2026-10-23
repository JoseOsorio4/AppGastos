package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.resultado.Resultado
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Único lugar que convierte texto ↔ centavos. Los montos viajan como Long de centavos, nunca Double. */
object Dinero {

    // \d en Java solo acepta dígitos ASCII; hasta dos decimales después de un punto.
    private val FORMATO_VALIDO = Regex("""\d+(\.\d{1,2})?""")
    private val DECIMALES_DE_MAS = Regex("""\d+\.\d{3,}""")

    /** "12.5" → 1250. Rechaza la coma para que "12,50" nunca se lea como 1250 centavos. */
    fun desdeTexto(texto: String): Resultado<Long> {
        val limpio = texto.trim()
        return when {
            limpio.isEmpty() -> rechazo("Escribe un monto")
            ',' in limpio -> rechazo("Usa punto para los decimales")
            limpio.startsWith('-') || limpio.startsWith('+') -> rechazo("Escribe el monto sin signo")
            limpio.any { it.isWhitespace() } -> rechazo("Quita los espacios del monto")
            DECIMALES_DE_MAS.matches(limpio) -> rechazo("Usa como máximo dos decimales")
            !FORMATO_VALIDO.matches(limpio) -> rechazo("Escribe solo números y un punto decimal")
            else -> aCentavos(limpio)
        }
    }

    /** 100000 → "Q 1,000.00"; −3000 → "-Q 30.00". Siempre con punto decimal y coma de miles. */
    fun formatear(centavos: Long, moneda: Moneda = Moneda.GTQ): String {
        val signo = if (centavos < 0) "-" else ""
        return "$signo${moneda.simbolo} ${numero(centavos)}"
    }

    private fun aCentavos(limpio: String): Resultado<Long> {
        val centavos = BigDecimal(limpio).movePointRight(2)
        return when {
            centavos.signum() == 0 -> rechazo("El monto debe ser mayor que cero")
            centavos > BigDecimal.valueOf(Transaccion.MONTO_MAXIMO_CENTAVOS) ->
                rechazo("El monto máximo es ${numero(Transaccion.MONTO_MAXIMO_CENTAVOS)}")
            else -> Resultado.Ok(centavos.longValueExact())
        }
    }

    // BigDecimal.valueOf(centavos, 2) evita el abs() de Long, que desborda con Long.MIN_VALUE.
    private fun numero(centavos: Long): String =
        DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US)).format(BigDecimal.valueOf(centavos, 2).abs())

    private fun rechazo(mensaje: String) = Resultado.ErrorNegocio(mensaje)
}
