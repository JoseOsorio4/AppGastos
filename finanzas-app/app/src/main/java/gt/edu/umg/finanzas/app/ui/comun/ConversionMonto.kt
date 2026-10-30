package gt.edu.umg.finanzas.app.ui.comun

import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.reglas.ConversorMoneda
import gt.edu.umg.finanzas.core.reglas.Dinero
import gt.edu.umg.finanzas.core.resultado.Resultado

enum class SentidoConversion(val entrada: Moneda, val salida: Moneda) {
    USD_A_GTQ(Moneda.USD, Moneda.GTQ),
    GTQ_A_USD(Moneda.GTQ, Moneda.USD),
}

/** Adaptación del texto de pantalla al core; aquí no hay fórmulas ni redondeos. */
object ConversionMonto {
    fun calcular(texto: String, sentido: SentidoConversion, tasa: TasaCambio?): Resultado<Long>? {
        if (texto.isBlank() || tasa == null) return null
        val monto = when (val leido = Dinero.desdeTexto(texto)) {
            is Resultado.Ok -> leido.valor
            is Resultado.Fallo -> return leido
        }
        return try {
            Resultado.Ok(when (sentido) {
                SentidoConversion.USD_A_GTQ -> ConversorMoneda.usdAGtq(monto, tasa)
                SentidoConversion.GTQ_A_USD -> ConversorMoneda.gtqAUsd(monto, tasa)
            })
        } catch (_: ArithmeticException) {
            Resultado.ErrorNegocio("El resultado es demasiado grande")
        }
    }
}
