package gt.edu.umg.finanzas.app.ui.comun

import android.content.Context
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.core.dominio.DesgloseConversion
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.reglas.ConversorMoneda
import gt.edu.umg.finanzas.core.reglas.Dinero
import java.math.BigDecimal

/** Presenta el desglose calculado por el core; no suma tasas ni redondea importes. */
object TextoConversionUsd {
    fun condiciones(contexto: Context, tasa: TasaCambio, recargo: BigDecimal,
        referencia: String = TextoTasa.referencia(contexto, tasa)): String =
        contexto.getString(R.string.referencia_oficial_usd, referencia) + "\n" +
            contexto.getString(R.string.condiciones_recargo_usd, recargo.toPlainString(),
                ConversorMoneda.tasaAplicada(tasa.gtqPorUsd, recargo).toPlainString())

    fun resumen(contexto: Context, desglose: DesgloseConversion, conRecargo: Boolean): String =
        if (conRecargo) contexto.getString(R.string.desglose_recargo_usd,
            Dinero.formatear(desglose.totalCentavos), Dinero.formatear(desglose.referenciaCentavos),
            Dinero.formatear(desglose.recargoCentavos))
        else contexto.getString(R.string.conversion_previa, Dinero.formatear(desglose.totalCentavos))

    fun detalle(contexto: Context, movimiento: MovimientoVista): String? = movimiento.conversion?.let {
        contexto.getString(R.string.original_usd, Dinero.formatear(it.montoOriginalCentavos, Moneda.USD)) + "\n" +
            condiciones(contexto, it.tasa, it.recargoGtqPorUsd) + "\n" +
            resumen(contexto, ConversorMoneda.desglosar(it, movimiento.montoCentavos), it.recargoGtqPorUsd.signum() > 0)
    }
}
