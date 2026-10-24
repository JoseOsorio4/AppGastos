package gt.edu.umg.finanzas.app.ui.formulario

import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ActivityFormularioBinding
import gt.edu.umg.finanzas.app.ui.comun.BarrasSistema
import gt.edu.umg.finanzas.app.ui.comun.Fechas
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Transaccion
import kotlinx.coroutines.launch

/**
 * Recibe los toques y el texto del formulario y se los pasa a FormularioViewModel; dibuja su estado.
 * Al guardar con éxito se cierra y Inicio, que observa Room, ya muestra los datos nuevos.
 */
class FormularioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormularioBinding
    private val viewModel: FormularioViewModel by lazy {
        ViewModelProvider(this, FormularioViewModel.Fabrica)[FormularioViewModel::class.java]
    }
    private val categorias = CategoriasAdapter { id -> viewModel.elegirCategoria(id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Barra superior verde: iconos claros arriba; abajo el fondo cambia con el tema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        binding = ActivityFormularioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BarrasSistema.reservarEspacio(
            binding.root,
            arriba = binding.barraSuperior,
            abajo = binding.contenidoFormulario,
            incluirTeclado = true,
        )

        binding.listaCategorias.layoutManager = GridLayoutManager(this, COLUMNAS_CATEGORIAS)
        binding.listaCategorias.adapter = categorias
        // El límite sale de la regla del core, no se repite el número en el XML.
        binding.campoDescripcion.counterMaxLength = Transaccion.DESCRIPCION_MAXIMA
        conectarAcciones()
        // Tras una rotación, el selector de fecha abierto pierde sus acciones: se le vuelven a conectar.
        // Con esa etiqueta solo se muestra el selector que crea abrirSelectorFecha, por eso el cast es seguro.
        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentByTag(ETIQUETA_SELECTOR) as? SelectorFecha)?.let(::conectarSelector)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect(::pintar)
            }
        }
    }

    private fun conectarAcciones() {
        binding.grupoTipo.addOnButtonCheckedListener { _, idBoton, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            viewModel.elegirTipo(if (idBoton == R.id.botonTipoIngreso) TipoMovimiento.INGRESO else TipoMovimiento.GASTO)
        }
        binding.entradaMonto.doAfterTextChanged { viewModel.onMontoCambiado(it?.toString().orEmpty()) }
        binding.chipSumar5.setOnClickListener { viewModel.sumar(5) }
        binding.chipSumar10.setOnClickListener { viewModel.sumar(10) }
        binding.chipSumar20.setOnClickListener { viewModel.sumar(20) }
        binding.chipSumar50.setOnClickListener { viewModel.sumar(50) }
        binding.entradaFecha.setOnClickListener { abrirSelectorFecha() }
        binding.campoFecha.setEndIconOnClickListener { abrirSelectorFecha() }
        binding.entradaDescripcion.doAfterTextChanged { viewModel.onDescripcionCambiada(it?.toString().orEmpty()) }
        binding.botonGuardar.setOnClickListener { viewModel.guardar() }
        binding.botonCancelar.setOnClickListener { finish() }
        binding.botonVolver.setOnClickListener { finish() }
    }

    private fun pintar(estado: FormularioUiState) {
        if (estado.guardadoExitoso) {
            finish()
            return
        }
        val idTipo = if (estado.tipo == TipoMovimiento.INGRESO) R.id.botonTipoIngreso else R.id.botonTipoGasto
        if (binding.grupoTipo.checkedButtonId != idTipo) binding.grupoTipo.check(idTipo)
        escribirSiCambio(binding.entradaMonto, estado.montoTexto)
        escribirSiCambio(binding.entradaDescripcion, estado.descripcion)
        binding.entradaFecha.setText(Fechas.texto(estado.fecha))

        binding.textoCargandoCategorias.isVisible = estado.categorias.isEmpty()
        categorias.mostrar(estado.categorias, estado.idCategoria)

        val errores = estado.errores
        binding.campoMonto.error = errores.monto
        binding.campoFecha.error = errores.fecha
        binding.campoDescripcion.error = errores.descripcion
        val errorCategoria = errores.categoria ?: if (errores.faltaCategoria) getString(R.string.error_sin_categoria) else null
        binding.textoErrorCategoria.text = errorCategoria
        binding.textoErrorCategoria.isVisible = errorCategoria != null
        val errorGeneral = errores.general ?: if (errores.falloAlGuardar) getString(R.string.error_guardar) else null
        binding.textoErrorGeneral.text = errorGeneral
        binding.textoErrorGeneral.isVisible = errorGeneral != null

        binding.botonGuardar.isEnabled = !estado.guardando
        binding.botonGuardar.setText(if (estado.guardando) R.string.guardando else R.string.guardar)
    }

    // Reescribir un EditText con el mismo texto movería el cursor al inicio mientras el usuario escribe.
    private fun escribirSiCambio(campo: EditText, texto: String) {
        if (campo.text?.toString() == texto) return
        campo.setText(texto)
        campo.setSelection(texto.length)
    }

    private fun abrirSelectorFecha() {
        if (supportFragmentManager.findFragmentByTag(ETIQUETA_SELECTOR) != null) return
        // Solo se ofrecen fechas hasta hoy; el core también rechaza una fecha futura.
        val restricciones = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.before(Fechas.aMilisUtc(Periodo.hoy())))
            .build()
        val selector = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.elegir_fecha)
            .setSelection(Fechas.aMilisUtc(viewModel.estado.value.fecha))
            .setCalendarConstraints(restricciones)
            .build()
        conectarSelector(selector)
        selector.show(supportFragmentManager, ETIQUETA_SELECTOR)
    }

    private fun conectarSelector(selector: SelectorFecha) {
        selector.addOnPositiveButtonClickListener { milis -> viewModel.elegirFecha(Fechas.desdeMilisUtc(milis)) }
    }

    private companion object {
        const val COLUMNAS_CATEGORIAS = 4
        const val ETIQUETA_SELECTOR = "selectorFecha"
    }
}

private typealias SelectorFecha = MaterialDatePicker<Long>
