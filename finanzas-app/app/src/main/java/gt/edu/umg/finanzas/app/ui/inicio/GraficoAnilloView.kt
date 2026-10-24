package gt.edu.umg.finanzas.app.ui.inicio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import gt.edu.umg.finanzas.app.ui.comun.RecursosCategoria
import gt.edu.umg.finanzas.core.dominio.SegmentoGasto
import gt.edu.umg.finanzas.core.reglas.DistribucionGastos

/**
 * Solo dibuja: recibe los segmentos ya calculados por DistribucionGastos y toma los colores de
 * los recursos. No consulta datos ni suma importes.
 */
class GraficoAnilloView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var segmentos: List<SegmentoGasto> = emptyList()
    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val area = RectF()

    fun mostrar(segmentos: List<SegmentoGasto>) {
        if (segmentos == this.segmentos) return
        this.segmentos = segmentos
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val lado = minOf(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).toFloat()
        val grosor = lado * GROSOR_RELATIVO
        pincel.strokeWidth = grosor
        val izquierda = paddingLeft + (width - paddingLeft - paddingRight - lado) / 2f + grosor / 2f
        val arriba = paddingTop + (height - paddingTop - paddingBottom - lado) / 2f + grosor / 2f
        area.set(izquierda, arriba, izquierda + lado - grosor, arriba + lado - grosor)

        // Empieza arriba (−90°) y avanza en sentido horario; los puntos básicos suman 10 000 = 360°.
        var inicio = -90f
        for (segmento in segmentos) {
            val barrido = 360f * segmento.puntosBasicos / DistribucionGastos.PUNTOS_TOTALES
            pincel.color = ContextCompat.getColor(context, RecursosCategoria.color(segmento.clave))
            canvas.drawArc(area, inicio, barrido, false, pincel)
            inicio += barrido
        }
    }

    private companion object {
        const val GROSOR_RELATIVO = 0.16f
    }
}
