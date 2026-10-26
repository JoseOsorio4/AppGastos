package gt.edu.umg.finanzas.app.ui.inicio

import android.graphics.Color
import android.os.Bundle
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
import gt.edu.umg.finanzas.app.databinding.ActivityInicioBinding
import gt.edu.umg.finanzas.app.ui.comun.BarrasSistema
import gt.edu.umg.finanzas.app.ui.comun.ChipsPeriodo
import gt.edu.umg.finanzas.app.ui.comun.DestinoPrincipal
import gt.edu.umg.finanzas.app.ui.comun.NavegacionPrincipal
import gt.edu.umg.finanzas.core.InfoCore
import gt.edu.umg.finanzas.core.reglas.Dinero
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
                viewModel.estado.collect(::pintar)
            }
        }
    }

    private fun pintar(estado: InicioUiState) {
        chipsPeriodo.pintar(estado.seleccion)
        binding.textoErrorInicial.isVisible = estado.errorInicial

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
