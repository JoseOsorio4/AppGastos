package gt.edu.umg.finanzas.app.ui.comun

import android.content.Intent
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.BarraNavegacionBinding
import gt.edu.umg.finanzas.app.ui.formulario.FormularioActivity
import gt.edu.umg.finanzas.app.ui.operaciones.OperacionesActivity

/** Pantallas que tienen la barra inferior. */
enum class DestinoPrincipal { INICIO, OPERACIONES }

/**
 * Único lugar que conecta barra_navegacion.xml. La pila es siempre Inicio → Operaciones → Formulario:
 * desde Operaciones, "Inicio" cierra la pantalla en lugar de abrir otra copia de Inicio.
 * Tocar el destino actual no hace nada.
 */
object NavegacionPrincipal {

    fun conectar(actividad: AppCompatActivity, barra: BarraNavegacionBinding, actual: DestinoPrincipal) {
        marcar(actividad, barra, actual)
        barra.botonIrInicio.setOnClickListener {
            if (actual == DestinoPrincipal.OPERACIONES) actividad.finish()
        }
        barra.botonIrOperaciones.setOnClickListener {
            if (actual == DestinoPrincipal.INICIO) {
                actividad.startActivity(Intent(actividad, OperacionesActivity::class.java))
            }
        }
        barra.botonAgregarMovimiento.setOnClickListener {
            actividad.startActivity(FormularioActivity.paraRegistrar(actividad))
        }
    }

    // El destino actual se marca con el icono amarillo y como seleccionado para TalkBack.
    private fun marcar(actividad: AppCompatActivity, barra: BarraNavegacionBinding, actual: DestinoPrincipal) {
        val acento = ColorStateList.valueOf(ContextCompat.getColor(actividad, R.color.color_acento))
        val normal = ColorStateList.valueOf(ContextCompat.getColor(actividad, R.color.color_sobre_cabecera))
        val enInicio = actual == DestinoPrincipal.INICIO
        ImageViewCompat.setImageTintList(barra.iconoIrInicio, if (enInicio) acento else normal)
        ImageViewCompat.setImageTintList(barra.iconoIrOperaciones, if (enInicio) normal else acento)
        barra.botonIrInicio.isSelected = enInicio
        barra.botonIrOperaciones.isSelected = !enInicio
    }
}
