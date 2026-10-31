package gt.edu.umg.finanzas.app.ui.acceso

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ActivityAccesoBinding
import gt.edu.umg.finanzas.app.ui.comun.BarrasSistema
import kotlinx.coroutines.launch

class AccesoActivity : AppCompatActivity() {
    private lateinit var vista: ActivityAccesoBinding
    private val modelo by lazy { ViewModelProvider(this)[AccesoViewModel::class.java] }
    private val configurar get() = intent.getBooleanExtra("configurarPin", false)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        vista = ActivityAccesoBinding.inflate(layoutInflater)
        setContentView(vista.root)
        BarrasSistema.reservarEspacio(vista.root, vista.root, vista.root, incluirTeclado = true)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (modelo.estado.value.paso == PasoAcceso.CONFIGURAR) modelo.continuar()
                else moveTaskToBack(true)
            }
        })
        vista.botonAcceso.setOnClickListener {
            when (modelo.estado.value.paso) {
                PasoAcceso.NOMBRE -> modelo.nombre(vista.entradaNombre.text.toString())
                PasoAcceso.PIN -> modelo.ingresar(vista.entradaPinActual.text.toString(), configurar)
                PasoAcceso.CONFIGURAR -> modelo.guardar(vista.entradaPinActual.text.toString(),
                    vista.entradaPinNuevo.text.toString(), vista.entradaPinConfirmacion.text.toString())
                PasoAcceso.CARGANDO -> modelo.actualizar(configurar)
                else -> Unit
            }
            limpiarPin()
        }
        vista.botonConfigurarPin.setOnClickListener { modelo.configurar() }
        vista.botonContinuar.setOnClickListener { modelo.continuar() }
        vista.botonDesactivarPin.setOnClickListener { modelo.desactivar(vista.entradaPinActual.text.toString()); limpiarPin() }
        lifecycleScope.launch { modelo.estado.collect(::pintar) }
    }
    override fun onResume() { super.onResume(); modelo.actualizar(configurar) }
    override fun onStop() { limpiarPin(); super.onStop() }
    private fun limpiarPin() {
        vista.entradaPinActual.text?.clear(); vista.entradaPinNuevo.text?.clear(); vista.entradaPinConfirmacion.text?.clear()
    }
    private fun pintar(estado: AccesoUiState) {
        if (estado.tienePin || estado.paso == PasoAcceso.CARGANDO) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (estado.paso == PasoAcceso.LISTO && !estado.ocupado) { finish(); return }
        val nombre = estado.paso == PasoAcceso.NOMBRE
        val bienvenida = estado.paso == PasoAcceso.BIENVENIDA
        val pin = estado.paso == PasoAcceso.PIN
        val ajustes = estado.paso == PasoAcceso.CONFIGURAR
        vista.tituloAcceso.text = if (bienvenida) getString(R.string.bienvenida_nombre, estado.nombre) else getString(when {
            nombre -> R.string.tu_nombre; pin -> R.string.ingresar_pin; ajustes -> R.string.seguridad_local; else -> R.string.preparando_acceso
        })
        vista.entradaNombre.isVisible = nombre
        vista.entradaPinActual.isVisible = pin || ajustes && estado.tienePin
        vista.entradaPinNuevo.isVisible = ajustes
        vista.entradaPinConfirmacion.isVisible = ajustes
        vista.avisoPin.isVisible = pin || ajustes
        vista.botonAcceso.isVisible = nombre || pin || ajustes || estado.error != null
        vista.botonAcceso.setText(if (ajustes) R.string.guardar_pin else R.string.continuar_acceso)
        vista.botonConfigurarPin.isVisible = bienvenida
        vista.botonContinuar.isVisible = bienvenida || ajustes
        vista.botonContinuar.setText(if (estado.tienePin) R.string.volver else R.string.continuar_sin_pin)
        vista.botonDesactivarPin.isVisible = ajustes && estado.tienePin
        vista.errorAcceso.text = estado.error
        vista.errorAcceso.isVisible = estado.error != null
        for (boton in listOf(vista.botonAcceso, vista.botonContinuar, vista.botonConfigurarPin, vista.botonDesactivarPin)) boton.isEnabled = !estado.ocupado
    }
}
