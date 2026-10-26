package gt.edu.umg.finanzas.app.ui.inicio

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
import gt.edu.umg.finanzas.core.resultado.Resultado
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Estado de Inicio. Observa el servicio con el período elegido: cada guardado en Room vuelve a
 * emitir y la pantalla se actualiza sola, sin "refrescar" al volver del formulario u Operaciones.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModel(
    private val servicio: ServicioFinanzas,
    estadoGuardado: SavedStateHandle,
    inicializacion: Deferred<Resultado<Unit>>,
    hoy: () -> LocalDate = { Periodo.hoy() },
) : ViewModel() {

    private val periodo = PeriodoGuardado(estadoGuardado, hoy)
    private val errorInicial = MutableStateFlow(false)

    private val datosDelPeriodo = periodo.seleccion.flatMapLatest { elegida ->
        combine(
            servicio.observarResumen(elegida.periodo),
            servicio.observarDistribucionGastos(elegida.periodo),
        ) { resumen, segmentos -> InicioUiState(elegida, resumen, segmentos) }
            // Mientras llega la primera respuesta, la pantalla ya muestra el período elegido.
            .onStart { emit(InicioUiState(elegida)) }
    }

    val estado: StateFlow<InicioUiState> =
        combine(datosDelPeriodo, errorInicial) { datos, error -> datos.copy(errorInicial = error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState(periodo.seleccion.value))

    init {
        // Si la siembra de usuario y categorías falló, el saldo nunca llegaría: se avisa en pantalla.
        viewModelScope.launch {
            if (inicializacion.await() is Resultado.Fallo) errorInicial.value = true
        }
    }

    fun seleccionarFiltro(filtro: FiltroPeriodo) = periodo.elegirFiltro(filtro)

    fun seleccionarRango(desde: LocalDate, hasta: LocalDate) = periodo.elegirRango(desde, hasta)

    companion object {
        /** Toma el servicio de Dependencias y crea el SavedStateHandle de esta pantalla. */
        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FinanzasApp
                InicioViewModel(app.dependencias.servicio, createSavedStateHandle(), app.inicializacion)
            }
        }
    }
}
