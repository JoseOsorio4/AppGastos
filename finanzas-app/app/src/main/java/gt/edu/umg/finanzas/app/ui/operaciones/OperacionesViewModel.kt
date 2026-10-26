package gt.edu.umg.finanzas.app.ui.operaciones

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.app.ui.comun.PeriodoGuardado
import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.resultado.Resultado
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Estado de Operaciones. La lista sale de ServicioFinanzas.observarMovimientos con los filtros
 * elegidos; al editar o eliminar, Room vuelve a emitir y la lista se actualiza sola.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OperacionesViewModel(
    private val servicio: ServicioFinanzas,
    private val estadoGuardado: SavedStateHandle,
    hoy: () -> LocalDate = { Periodo.hoy() },
) : ViewModel() {

    private val periodo = PeriodoGuardado(estadoGuardado, hoy)
    private val tipo = MutableStateFlow(estadoGuardado.get<String>(CLAVE_TIPO)?.let { TipoMovimiento.valueOf(it) })
    private val aviso = MutableStateFlow<AvisoOperaciones?>(null)

    private val filtros = combine(periodo.seleccion, tipo, ::FiltrosOperaciones)

    private val lista = filtros.flatMapLatest { activos ->
        servicio.observarMovimientos(activos.seleccion.periodo, activos.tipo)
            .map { movimientos -> OperacionesUiState(activos, FilaOperacion.agrupar(movimientos), cargando = false) }
            .onStart { emit(OperacionesUiState(activos)) }
    }

    val estado: StateFlow<OperacionesUiState> =
        combine(lista, aviso) { datos, avisoActual -> datos.copy(aviso = avisoActual) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                OperacionesUiState(FiltrosOperaciones(periodo.seleccion.value, tipo.value)),
            )

    fun seleccionarFiltro(filtro: FiltroPeriodo) = periodo.elegirFiltro(filtro)

    fun seleccionarRango(desde: LocalDate, hasta: LocalDate) = periodo.elegirRango(desde, hasta)

    fun filtrarTipo(nuevo: TipoMovimiento?) {
        tipo.value = nuevo
        estadoGuardado[CLAVE_TIPO] = nuevo?.name
    }

    /** Solo se llama desde la confirmación; OperacionesMovimientoRoom borra el movimiento y ajusta el saldo juntos. */
    fun eliminar(idTransaccion: Long) {
        viewModelScope.launch {
            aviso.value = when (servicio.eliminar(idTransaccion)) {
                is Resultado.Ok -> AvisoOperaciones.ELIMINADO
                // Si ya no existía, la lista ya no lo muestra: no hay nada que avisar.
                Resultado.NoEncontrado -> null
                is Resultado.Fallo -> AvisoOperaciones.NO_SE_PUDO_ELIMINAR
            }
        }
    }

    fun avisoMostrado() {
        aviso.value = null
    }

    companion object {
        private const val CLAVE_TIPO = "tipoFiltro"

        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FinanzasApp
                OperacionesViewModel(app.dependencias.servicio, createSavedStateHandle())
            }
        }
    }
}
