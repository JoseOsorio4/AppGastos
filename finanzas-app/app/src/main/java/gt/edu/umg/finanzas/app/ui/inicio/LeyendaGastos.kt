package gt.edu.umg.finanzas.app.ui.inicio

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ItemLeyendaGastoBinding
import gt.edu.umg.finanzas.app.ui.comun.RecursosCategoria
import gt.edu.umg.finanzas.core.dominio.SegmentoGasto
import gt.edu.umg.finanzas.core.reglas.Dinero
import java.math.BigDecimal

/** Filas bajo el anillo: el anillo no se lee solo con colores, así que cada categoría tiene su texto. */
object LeyendaGastos {

    fun mostrar(contenedor: LinearLayout, segmentos: List<SegmentoGasto>) {
        contenedor.removeAllViews()
        val inflador = LayoutInflater.from(contenedor.context)
        for (segmento in segmentos) {
            val fila = ItemLeyendaGastoBinding.inflate(inflador, contenedor, true)
            val color = ContextCompat.getColor(contenedor.context, RecursosCategoria.color(segmento.clave))
            fila.iconoCategoria.setImageResource(RecursosCategoria.icono(segmento.clave))
            ViewCompat.setBackgroundTintList(fila.iconoCategoria, ColorStateList.valueOf(color))
            fila.textoNombre.text = segmento.nombre
            fila.textoPorcentaje.text = porcentaje(contenedor.context, segmento.puntosBasicos)
            fila.textoMonto.text = Dinero.formatear(segmento.totalCentavos)
        }
    }

    /** 6000 puntos básicos → "60 %"; 3333 → "33.33 %". */
    fun porcentaje(context: Context, puntosBasicos: Int): String =
        context.getString(
            R.string.porcentaje,
            BigDecimal.valueOf(puntosBasicos.toLong(), 2).stripTrailingZeros().toPlainString(),
        )
}
