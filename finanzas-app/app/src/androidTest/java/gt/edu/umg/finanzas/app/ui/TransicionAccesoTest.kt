package gt.edu.umg.finanzas.app.ui

import android.animation.ValueAnimator
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.acceso.TransicionAcceso
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransicionAccesoTest {
    private fun comprobar(habilitadas: Boolean) {
        check(app.packageName.endsWith(".pruebas"))
        val instrumento = InstrumentationRegistry.getInstrumentation()
        val anterior = Settings.Global.getString(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
        fun ajustar(valor: String?) {
            val orden = if (valor == null) "settings delete global animator_duration_scale"
                else "settings put global animator_duration_scale $valor"
            ParcelFileDescriptor.AutoCloseInputStream(instrumento.uiAutomation.executeShellCommand(orden)).use { it.readBytes() }
        }
        try {
            ajustar(if (habilitadas) "1" else "0")
            reintentar {
                val activas = if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
                    else Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
                assertEquals(habilitadas, activas)
            }
            instrumento.runOnMainSync {
                val vista = View(app)
                var acciones = 0
                vista.setOnClickListener { acciones++ }
                val transicion = TransicionAcceso(listOf(vista))
                transicion.mostrar()
                vista.performClick()
                assertEquals(1, acciones)
                if (!habilitadas) {
                    assertEquals(1f, vista.alpha, 0f)
                    assertEquals(0f, vista.translationY, 0f)
                }
                // Al abandonar o sustituir un estado, no queda contenido transparente o desplazado.
                transicion.detener()
                assertEquals(1f, vista.alpha, 0f)
                assertEquals(0f, vista.translationY, 0f)
            }
        } finally { ajustar(anterior) }
    }

    @Test fun animacion_no_impide_acciones_y_cancelar_restaurar_vista() = comprobar(true)
    @Test fun animaciones_desactivadas_muestran_contenido_inmediato() = comprobar(false)
}
