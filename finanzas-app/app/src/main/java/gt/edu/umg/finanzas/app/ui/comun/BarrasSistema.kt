package gt.edu.umg.finanzas.app.ui.comun

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Con targetSdk 36 la ventana se dibuja bajo las barras del sistema. La cabecera verde se extiende
 * bajo la barra de estado y [abajo] reserva el alto de la barra de navegación, y del teclado si se pide.
 */
object BarrasSistema {

    fun reservarEspacio(raiz: View, arriba: View, abajo: View, incluirTeclado: Boolean = false) {
        val arribaOriginal = arriba.paddingTop
        val abajoOriginal = abajo.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->
            var tipos = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            if (incluirTeclado) tipos = tipos or WindowInsetsCompat.Type.ime()
            val bordes = insets.getInsets(tipos)
            vista.updatePadding(left = bordes.left, right = bordes.right)
            arriba.updatePadding(top = arribaOriginal + bordes.top)
            abajo.updatePadding(bottom = abajoOriginal + bordes.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
