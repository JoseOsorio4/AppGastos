package gt.edu.umg.finanzas.app.ui.formulario

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.widget.EditText
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import gt.edu.umg.finanzas.app.ui.comun.ActivityProtegida
import gt.edu.umg.finanzas.app.ui.comun.BarrasSistema
import gt.edu.umg.finanzas.app.ui.comun.Fechas
import gt.edu.umg.finanzas.app.ui.comun.TextoTasa
import gt.edu.umg.finanzas.app.ui.comun.TextoConversionUsd
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.launch

/**
 * Recibe los toques y el texto del formulario y se los pasa a FormularioViewModel; dibuja su estado.
 * Al guardar con éxito se cierra y Inicio, que observa Room, ya muestra los datos nuevos.
 */
class FormularioActivity : ActivityProtegida() {

    companion object {
        private const val COLUMNAS_CATEGORIAS = 4
        // Ancho que necesita "Entretenimiento" con letra normal; con letra grande crece en proporción.
        private const val ANCHO_CELDA_DP = 104f
        private const val ETIQUETA_SELECTOR = "selectorFecha"

        /** Desde el botón Añadir de la barra inferior. */
        fun paraRegistrar(contexto: Context) = Intent(contexto, FormularioActivity::class.java)

        /** Desde Editar en el detalle de Operaciones: el mismo formulario, con los datos guardados. */
        fun paraEditar(contexto: Context, idTransaccion: Long) =
            Intent(contexto, FormularioActivity::class.java)
                .putExtra(FormularioViewModel.CLAVE_ID_EN_EDICION, idTransaccion)
    }

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
            // Reducir el viewport permite seguir el campo y su cursor sobre el IME.
            abajo = binding.root,
            incluirTeclado = true,
        )
        var altoAnterior = -1
        var posicionAnterior = -1
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val alto = binding.contenidoFormulario.height
            val posicion = binding.campoDescripcion.top
            if (alto != altoAnterior || posicion != posicionAnterior) {
                altoAnterior = alto
                posicionAnterior = posicion
                // Esperar todo el layout incluye el teclado y las categorías que llegan de Room.
                mostrarCampoEnfocado()
            }
        }
        binding.entradaDescripcion.setOnFocusChangeListener { vista, tieneFoco ->
            if (tieneFoco) vista.post(::mostrarCampoEnfocado)
        }

        binding.listaCategorias.layoutManager = GridLayoutManager(this, columnasCategorias())
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

    private fun mostrarCampoEnfocado() {
        val enfocado = currentFocus as? EditText ?: return
        val campo = if (enfocado == binding.entradaDescripcion) binding.campoDescripcion else enfocado
        campo.requestRectangleOnScreen(Rect(0, 0, campo.width, campo.height), true)
    }

    private fun conectarAcciones() {
        binding.grupoMoneda.addOnButtonCheckedListener { _, id, marcado ->
            if (marcado) viewModel.elegirMoneda(if (id == R.id.botonMonedaUsd) Moneda.USD else Moneda.GTQ)
        }
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
        binding.textoTituloFormulario.setText(if (estado.enEdicion) R.string.editar_movimiento else R.string.nuevo_movimiento)
        val idTipo = if (estado.tipo == TipoMovimiento.INGRESO) R.id.botonTipoIngreso else R.id.botonTipoGasto
        if (binding.grupoTipo.checkedButtonId != idTipo) binding.grupoTipo.check(idTipo)
        escribirSiCambio(binding.entradaMonto, estado.montoTexto)
        escribirSiCambio(binding.entradaDescripcion, estado.descripcion)
        binding.entradaFecha.setText(Fechas.texto(estado.fecha))
        val idMoneda = if (estado.moneda == Moneda.USD) R.id.botonMonedaUsd else R.id.botonMonedaGtq
        if (binding.grupoMoneda.checkedButtonId != idMoneda) binding.grupoMoneda.check(idMoneda)
        binding.botonMonedaUsd.isEnabled = estado.usdDisponible
        binding.campoMonto.prefixText = estado.moneda.simbolo
        binding.textoTasaFormulario.isVisible = estado.moneda == Moneda.USD || !estado.usdDisponible
        binding.textoTasaFormulario.text = estado.tasa?.let { tasa ->
            val referencia = if (estado.tasaHistorica == null) TextoTasa.estado(this, estado.estadoTasa) else TextoTasa.referencia(this, tasa)
            val condiciones = TextoConversionUsd.condiciones(this, tasa, estado.recargoGtqPorUsd, referencia)
            if (estado.tasaHistorica == null) condiciones else {
                val avisoRecargo = if (estado.tipo == estado.tipoHistorico) R.string.recargo_historico else R.string.recargo_cambio_tipo
                "$condiciones\n${getString(R.string.tasa_historica, Fechas.texto(tasa.fechaReferencia))}\n${getString(avisoRecargo)}"
            }
        } ?: TextoTasa.estado(this, estado.estadoTasa)
        val previa = if (estado.moneda == Moneda.USD) estado.conversionPrevia else null
        binding.textoConversionPrevia.isVisible = previa != null
        binding.textoConversionPrevia.text = when (previa) {
            is Resultado.Ok -> TextoConversionUsd.resumen(this, previa.valor, estado.recargoGtqPorUsd.signum() > 0)
            is Resultado.ErrorNegocio -> previa.mensaje
            else -> ""
        }

        binding.textoCargandoCategorias.isVisible = estado.categorias.isEmpty()
        categorias.mostrar(estado.categorias, estado.idCategoria)

        val errores = estado.errores
        binding.campoMonto.error = errores.monto
        binding.campoFecha.error = errores.fecha
        binding.campoDescripcion.error = errores.descripcion
        val errorCategoria = errores.categoria ?: if (errores.faltaCategoria) getString(R.string.error_sin_categoria) else null
        binding.textoErrorCategoria.text = errorCategoria
        binding.textoErrorCategoria.isVisible = errorCategoria != null
        val errorGeneral = errores.general ?: when {
            errores.falloAlGuardar -> getString(R.string.error_guardar)
            errores.movimientoNoEncontrado -> getString(R.string.error_movimiento_no_encontrado)
            errores.falloAlCargar -> getString(R.string.error_cargar_movimiento)
            else -> null
        }
        binding.textoErrorGeneral.text = errorGeneral
        binding.textoErrorGeneral.isVisible = errorGeneral != null

        binding.botonGuardar.isEnabled = !estado.guardando && !estado.cargandoMovimiento && !errores.movimientoNoEncontrado
        binding.botonGuardar.setText(if (estado.guardando) R.string.guardando else R.string.guardar)
    }

    // El ancho crece con la letra para que los nombres sigan completos.
    private fun columnasCategorias(): Int {
        val configuracion = resources.configuration
        val anchoCelda = ANCHO_CELDA_DP * configuracion.fontScale
        return (configuracion.screenWidthDp / anchoCelda).toInt().coerceIn(1, COLUMNAS_CATEGORIAS)
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

}

private typealias SelectorFecha = MaterialDatePicker<Long>
