package gt.edu.umg.finanzas.app.ui.inicio

import android.content.Intent
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
import com.google.android.material.datepicker.MaterialDatePicker
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ActivityInicioBinding
import gt.edu.umg.finanzas.app.ui.comun.BarrasSistema
import gt.edu.umg.finanzas.app.ui.comun.Fechas
import gt.edu.umg.finanzas.app.ui.formulario.FormularioActivity
import gt.edu.umg.finanzas.core.InfoCore
import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import gt.edu.umg.finanzas.core.reglas.Dinero
import kotlinx.coroutines.launch

/** Dibuja el estado de InicioViewModel y le envía las acciones del usuario. No calcula montos. */
class InicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioBinding
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
        conectarAcciones()
        // Tras una rotación, el selector de rango sigue abierto: se le vuelven a conectar las acciones.
        // Con esa etiqueta solo se muestra el selector que crea abrirSelectorRango, por eso el cast es seguro.
        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentByTag(ETIQUETA_SELECTOR) as? SelectorRango)?.let(::conectarSelector)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect(::pintar)
            }
        }
    }

    private fun conectarAcciones() {
        binding.chipDia.setOnClickListener { viewModel.seleccionarFiltro(FiltroPeriodo.DIA) }
        binding.chipSemana.setOnClickListener { viewModel.seleccionarFiltro(FiltroPeriodo.SEMANA) }
        binding.chipMes.setOnClickListener { viewModel.seleccionarFiltro(FiltroPeriodo.MES) }
        binding.chipAnio.setOnClickListener { viewModel.seleccionarFiltro(FiltroPeriodo.ANIO) }
        binding.chipPersonalizado.setOnClickListener { abrirSelectorRango() }
        binding.barraNavegacion.botonAgregarMovimiento.setOnClickListener {
            startActivity(Intent(this, FormularioActivity::class.java))
        }
        // Ya estamos en Inicio: tocar su botón no hace nada.
        binding.barraNavegacion.botonIrInicio.setOnClickListener { }
    }

    private fun pintar(estado: InicioUiState) {
        marcarChip(estado.seleccion.filtro)
        val periodo = estado.seleccion.periodo
        binding.textoRangoPeriodo.text =
            getString(R.string.rango_periodo, Fechas.texto(periodo.desde), Fechas.texto(periodo.hasta))
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

    private fun marcarChip(filtro: FiltroPeriodo?) {
        val id = when (filtro) {
            FiltroPeriodo.DIA -> R.id.chipDia
            FiltroPeriodo.SEMANA -> R.id.chipSemana
            FiltroPeriodo.MES -> R.id.chipMes
            FiltroPeriodo.ANIO -> R.id.chipAnio
            null -> R.id.chipPersonalizado
        }
        if (binding.grupoPeriodo.checkedChipId != id) binding.grupoPeriodo.check(id)
    }

    private fun abrirSelectorRango() {
        val actual = viewModel.estado.value.seleccion.periodo
        val selector = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.elegir_periodo)
            .setSelection(androidx.core.util.Pair(Fechas.aMilisUtc(actual.desde), Fechas.aMilisUtc(actual.hasta)))
            .build()
        conectarSelector(selector)
        selector.show(supportFragmentManager, ETIQUETA_SELECTOR)
    }

    private fun conectarSelector(selector: SelectorRango) {
        selector.addOnPositiveButtonClickListener { rango ->
            viewModel.seleccionarRango(Fechas.desdeMilisUtc(rango.first), Fechas.desdeMilisUtc(rango.second))
        }
        // Al cancelar, el chip tocado vuelve a marcar el período que sigue vigente.
        selector.addOnDismissListener { marcarChip(viewModel.estado.value.seleccion.filtro) }
    }

    private companion object {
        const val ETIQUETA_SELECTOR = "selectorPeriodo"
    }
}

private typealias SelectorRango = MaterialDatePicker<androidx.core.util.Pair<Long, Long>>
