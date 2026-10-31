package gt.edu.umg.finanzas.app.ui.comun

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.repositorios.EstadoTasa
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TextoTasa {
    fun paraTarjeta(contexto: Context, estado: EstadoTasa): CharSequence {
        val texto = estado(contexto, estado)
        if (estado.dato == null) return texto
        return SpannableString(texto).apply {
            val fin = texto.indexOf('\n').takeIf { it >= 0 } ?: texto.length
            setSpan(AbsoluteSizeSpan(contexto.resources.getDimensionPixelSize(R.dimen.texto_valor_servicio)),
                0, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), 0, fin, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun referencia(contexto: Context, tasa: TasaCambio): String = contexto.getString(
        R.string.referencia_tasa, tasa.gtqPorUsd.toPlainString(), Fechas.texto(tasa.fechaReferencia), tasa.fuente,
    )

    fun estado(contexto: Context, estado: EstadoTasa): String {
        val dato = estado.dato ?: return contexto.getString(
            if (estado.cargando) R.string.consultando_tasa else R.string.sin_tasa,
        )
        val consulta = dato.consultadoEn.atZone(ZoneId.of("America/Guatemala"))
            .format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm"))
        val origen = contexto.getString(if (estado.esGuardada) R.string.tasa_guardada else R.string.tasa_consultada)
        val ultima = if (dato.tasa.fechaReferencia != Periodo.hoy()) "\n${contexto.getString(R.string.ultima_tasa)}" else ""
        val error = if (estado.error) "\n${contexto.getString(R.string.error_actualizar_tasa)}" else ""
        return "${referencia(contexto, dato.tasa)}\n$origen · $consulta$ultima$error"
    }
}
