package gt.edu.umg.finanzas.app.ui.comun

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.app.ui.acceso.AccesoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ActivityProtegida : AppCompatActivity() {
    private var protegerVista = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.visibility = View.INVISIBLE
    }
    override fun onResume() {
        super.onResume()
        window.decorView.visibility = View.INVISIBLE
        lifecycleScope.launch {
            val app = application as FinanzasApp
            app.inicializacion.await()
            val acceso = withContext(Dispatchers.IO) {
                try { app.dependencias.bloqueo.run { EstadoProteccion(bloqueado() || necesitaBienvenida(), tienePin()) } }
                catch (_: Exception) { EstadoProteccion(true, true) }
            }
            if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return@launch
            protegerVista = acceso.tienePin
            if (protegerVista) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (acceso.requiereAcceso) startActivity(Intent(this@ActivityProtegida, AccesoActivity::class.java))
            else window.decorView.visibility = View.VISIBLE
        }
    }
    private data class EstadoProteccion(val requiereAcceso: Boolean, val tienePin: Boolean)

    override fun onPause() {
        if (protegerVista) window.decorView.visibility = View.INVISIBLE
        super.onPause()
    }
}
