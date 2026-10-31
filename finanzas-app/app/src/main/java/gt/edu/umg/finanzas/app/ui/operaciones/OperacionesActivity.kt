package gt.edu.umg.finanzas.app.ui.operaciones

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ActivityOperacionesBinding
import gt.edu.umg.finanzas.app.ui.comun.ActivityProtegida
import gt.edu.umg.finanzas.app.ui.comun.BarrasSistema
import gt.edu.umg.finanzas.app.ui.comun.ChipsPeriodo
import gt.edu.umg.finanzas.app.ui.comun.DestinoPrincipal
import gt.edu.umg.finanzas.app.ui.comun.NavegacionPrincipal
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import kotlinx.coroutines.launch

/** Recibe los filtros y el toque en una fila; dibuja la lista que entrega OperacionesViewModel. */
class OperacionesActivity : ActivityProtegida() {

    private lateinit var binding: ActivityOperacionesBinding
    private lateinit var chipsPeriodo: ChipsPeriodo
    private val viewModel: OperacionesViewModel by lazy {
        ViewModelProvider(this, OperacionesViewModel.Fabrica)[OperacionesViewModel::class.java]
    }
    private val adaptador = OperacionesAdapter { id -> abrirDetalle(id) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        binding = ActivityOperacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BarrasSistema.reservarEspacio(binding.root, arriba = binding.cabecera, abajo = binding.barraNavegacion.root)

        NavegacionPrincipal.conectar(this, binding.barraNavegacion, DestinoPrincipal.OPERACIONES)
        chipsPeriodo = ChipsPeriodo(
            actividad = this,
            vista = binding.chipsPeriodo,
            alElegirFiltro = viewModel::seleccionarFiltro,
            alElegirRango = viewModel::seleccionarRango,
            seleccionActual = { viewModel.estado.value.filtros.seleccion },
        )
        binding.chipTodos.setOnClickListener { viewModel.filtrarTipo(null) }
        binding.chipIngresos.setOnClickListener { viewModel.filtrarTipo(TipoMovimiento.INGRESO) }
        binding.chipGastos.setOnClickListener { viewModel.filtrarTipo(TipoMovimiento.GASTO) }
        binding.listaOperaciones.layoutManager = LinearLayoutManager(this)
        binding.listaOperaciones.adapter = adaptador

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect(::pintar)
            }
        }
    }

    private fun pintar(estado: OperacionesUiState) {
        chipsPeriodo.pintar(estado.filtros.seleccion)
        val idTipo = when (estado.filtros.tipo) {
            null -> R.id.chipTodos
            TipoMovimiento.INGRESO -> R.id.chipIngresos
            TipoMovimiento.GASTO -> R.id.chipGastos
        }
        if (binding.grupoTipoFiltro.checkedChipId != idTipo) binding.grupoTipoFiltro.check(idTipo)

        adaptador.submitList(estado.filas)
        binding.textoSinOperaciones.isVisible = !estado.cargando && estado.filas.isEmpty()

        estado.aviso?.let { aviso ->
            val texto = when (aviso) {
                AvisoOperaciones.ELIMINADO -> R.string.movimiento_eliminado
                AvisoOperaciones.NO_SE_PUDO_ELIMINAR -> R.string.error_eliminar
            }
            Snackbar.make(binding.root, texto, Snackbar.LENGTH_SHORT).setAnchorView(binding.barraNavegacion.root).show()
            viewModel.avisoMostrado()
        }
    }

    private fun abrirDetalle(idTransaccion: Long) {
        // Un segundo toque rápido no abre otro detalle encima del primero.
        if (supportFragmentManager.findFragmentByTag(DetalleMovimientoDialogo.ETIQUETA) != null) return
        DetalleMovimientoDialogo.nuevo(idTransaccion).show(supportFragmentManager, DetalleMovimientoDialogo.ETIQUETA)
    }
}
