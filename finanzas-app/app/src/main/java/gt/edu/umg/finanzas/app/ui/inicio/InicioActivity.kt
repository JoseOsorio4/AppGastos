package gt.edu.umg.finanzas.app.ui.inicio

import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.repositorios.EstadoClima
import gt.edu.umg.finanzas.app.datos.repositorios.EstadoReflexion
import gt.edu.umg.finanzas.app.databinding.ActivityInicioBinding
import gt.edu.umg.finanzas.app.ui.comun.BarrasSistema
import gt.edu.umg.finanzas.app.ui.comun.ChipsPeriodo
import gt.edu.umg.finanzas.app.ui.comun.DestinoPrincipal
import gt.edu.umg.finanzas.app.ui.comun.NavegacionPrincipal
import gt.edu.umg.finanzas.app.ui.comun.TextoTasa
import gt.edu.umg.finanzas.core.InfoCore
import gt.edu.umg.finanzas.core.reglas.Dinero
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/** Dibuja el estado de InicioViewModel y le envía las acciones del usuario. No calcula montos. */
class InicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioBinding
    private lateinit var chipsPeriodo: ChipsPeriodo
    private val viewModel: InicioViewModel by lazy {
        ViewModelProvider(this, InicioViewModel.Fabrica)[InicioViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Cabecera y barra inferior son verdes en ambos temas: iconos claros en las dos barras.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BarrasSistema.reservarEspacio(binding.root, arriba = binding.cabecera, abajo = binding.barraNavegacion.root)

        binding.textoConexionCore.text = getString(R.string.version_core, InfoCore.version)
        binding.tarjetaTasa.botonReintentarTasa.setOnClickListener { viewModel.actualizarTasa(forzar = true) }
        binding.tarjetaClima.botonReintentarClima.setOnClickListener { viewModel.actualizarClima(forzar = true) }
        binding.tarjetaClima.enlaceOpenMeteo.movementMethod = LinkMovementMethod.getInstance()
        binding.tarjetaReflexion.botonVisibilidadReflexion.setOnClickListener { viewModel.cambiarVisibilidadReflexion() }
        binding.tarjetaReflexion.botonReintentarReflexion.setOnClickListener { viewModel.actualizarReflexion(forzar = true) }
        binding.tarjetaTasa.botonConversor.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(ConversorDialogo.ETIQUETA) == null) {
                ConversorDialogo().show(supportFragmentManager, ConversorDialogo.ETIQUETA)
            }
        }
        binding.botonRecalcularSaldo.setOnClickListener { viewModel.recalcularSaldo() }
        binding.opcionTema.setOnClickListener {
            if (supportFragmentManager.findFragmentByTag(TemaDialogo.ETIQUETA) == null) {
                TemaDialogo().show(supportFragmentManager, TemaDialogo.ETIQUETA)
            }
        }
        NavegacionPrincipal.conectar(this, binding.barraNavegacion, DestinoPrincipal.INICIO)
        chipsPeriodo = ChipsPeriodo(
            actividad = this,
            vista = binding.chipsPeriodo,
            alElegirFiltro = viewModel::seleccionarFiltro,
            alElegirRango = viewModel::seleccionarRango,
            seleccionActual = { viewModel.estado.value.seleccion },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actualizarClima()
                viewModel.actualizarReflexion()
                launch { viewModel.estado.collect(::pintar) }
                launch { viewModel.estadoClima.collect(::pintarClima) }
                launch { viewModel.estadoReflexion.collect(::pintarReflexion) }
                launch {
                    viewModel.estadoTasa.collect { tasa ->
                        binding.tarjetaTasa.textoTasa.text = TextoTasa.estado(this@InicioActivity, tasa)
                        binding.tarjetaTasa.botonReintentarTasa.isEnabled = !tasa.cargando
                        binding.tarjetaTasa.botonConversor.isEnabled = tasa.dato != null
                    }
                }
            }
        }
    }

    private fun pintarClima(estado: EstadoClima) = with(binding.tarjetaClima) {
        textoClima.text = TextoClima.valor(this@InicioActivity, estado)
        textoEstadoClima.text = TextoClima.detalle(this@InicioActivity, estado)
        botonReintentarClima.isEnabled = !estado.cargando
    }

    private fun pintarReflexion(estado: EstadoReflexion) = with(binding.tarjetaReflexion) {
        contenidoReflexion.isVisible = estado.visible
        botonVisibilidadReflexion.setText(if (estado.visible) R.string.ocultar_reflexion else R.string.mostrar_reflexion)
        textoReflexion.text = estado.dato?.reflexion?.texto ?: getString(R.string.reflexion_cargando)
        textoFuenteReflexion.text = listOfNotNull(
            estado.dato?.let {
                getString(
                    R.string.fuente_reflexion,
                    it.reflexion.referencia,
                    it.reflexion.traduccion,
                    it.reflexion.fuente,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy").format(it.fecha),
                )
            },
            if (estado.error) getString(R.string.reflexion_error_guardar) else null,
            if (estado.errorConsulta) getString(R.string.reflexion_error_consulta) else null,
            if (estado.cargando && estado.dato != null) getString(R.string.reflexion_cargando) else null,
        ).joinToString("\n")
        botonReintentarReflexion.setText(
            if (estado.error || estado.errorConsulta) R.string.reintentar_reflexion else R.string.actualizar_reflexion,
        )
        botonReintentarReflexion.isEnabled = !estado.cargando
    }

    private fun pintar(estado: InicioUiState) {
        chipsPeriodo.pintar(estado.seleccion)
        binding.textoErrorInicial.isVisible = estado.errorInicial
        binding.avisoRecalculo.isVisible = estado.reconstruccionPendiente
        binding.botonRecalcularSaldo.isEnabled = !estado.recalculando
        binding.botonRecalcularSaldo.setText(if (estado.recalculando) R.string.recalculando_saldo else R.string.recalcular_saldo)
        binding.textoErrorRecalculo.isVisible = estado.errorRecalculo

        val resumen = estado.resumen ?: return
        binding.textoSaldoDisponible.text = Dinero.formatear(resumen.saldoDisponibleCentavos)
        binding.textoIngresos.text = Dinero.formatear(resumen.ingresosCentavos)
        binding.textoGastos.text = Dinero.formatear(resumen.gastosCentavos)
        // El resultado lleva signo y color propios; el color nunca es la única pista.
        binding.textoResultado.text = conSigno(resumen.resultadoCentavos)
        binding.textoResultado.setTextColor(
            ContextCompat.getColor(this, if (resumen.resultadoCentavos < 0) R.color.color_gasto else R.color.color_ingreso),
        )

        val hayGastos = estado.segmentos.isNotEmpty()
        binding.contenedorAnillo.isVisible = hayGastos
        binding.textoSinGastos.isVisible = !hayGastos
        binding.textoTotalGastosAnillo.text = Dinero.formatear(resumen.gastosCentavos)
        binding.graficoGastos.mostrar(estado.segmentos)
        binding.graficoGastos.contentDescription = getString(
            R.string.descripcion_anillo,
            estado.segmentos.joinToString { "${it.nombre} ${LeyendaGastos.porcentaje(this, it.puntosBasicos)}" },
        )
        LeyendaGastos.mostrar(binding.contenedorLeyenda, estado.segmentos)
    }

    private fun conSigno(centavos: Long): String =
        if (centavos > 0) "+${Dinero.formatear(centavos)}" else Dinero.formatear(centavos)
}
