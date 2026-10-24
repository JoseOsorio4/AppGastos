package gt.edu.umg.finanzas.app.ui.inicio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gt.edu.umg.finanzas.app.FinanzasApp
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
 * emitir y la pantalla se actualiza sola, sin "refrescar" al volver del formulario.
 * El período se guarda en SavedStateHandle para sobrevivir a la rotación y a la muerte del proceso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModel(
    private val servicio: ServicioFinanzas,
    private val estadoGuardado: SavedStateHandle,
    inicializacion: Deferred<Resultado<Unit>>,
    private val hoy: () -> LocalDate = { Periodo.hoy() },
) : ViewModel() {

    private val seleccion = MutableStateFlow(leerSeleccion())
    private val errorInicial = MutableStateFlow(false)

    private val datosDelPeriodo = seleccion.flatMapLatest { elegida ->
        combine(
            servicio.observarResumen(elegida.periodo),
            servicio.observarDistribucionGastos(elegida.periodo),
        ) { resumen, segmentos -> InicioUiState(elegida, resumen, segmentos) }
            // Mientras llega la primera respuesta, la pantalla ya muestra el período elegido.
            .onStart { emit(InicioUiState(elegida)) }
    }

    val estado: StateFlow<InicioUiState> =
        combine(datosDelPeriodo, errorInicial) { datos, error -> datos.copy(errorInicial = error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InicioUiState(seleccion.value))

    init {
        // Si la siembra de usuario y categorías falló, el saldo nunca llegaría: se avisa en pantalla.
        viewModelScope.launch {
            if (inicializacion.await() is Resultado.Fallo) errorInicial.value = true
        }
    }

    fun seleccionarFiltro(filtro: FiltroPeriodo) {
        cambiarSeleccion(SeleccionPeriodo(filtro, Periodo.de(filtro, hoy())))
    }

    fun seleccionarRango(desde: LocalDate, hasta: LocalDate) {
        // El selector de rango ya entrega desde ≤ hasta; un rango inválido simplemente no se aplica.
        val periodo = Periodo.personalizado(desde, hasta)
        if (periodo is Resultado.Ok) cambiarSeleccion(SeleccionPeriodo(null, periodo.valor))
    }

    private fun cambiarSeleccion(nueva: SeleccionPeriodo) {
        seleccion.value = nueva
        estadoGuardado[CLAVE_FILTRO] = nueva.filtro?.name ?: PERSONALIZADO
        estadoGuardado[CLAVE_DESDE] = nueva.periodo.desde.toEpochDay()
        estadoGuardado[CLAVE_HASTA] = nueva.periodo.hasta.toEpochDay()
    }

    private fun leerSeleccion(): SeleccionPeriodo {
        val nombre = estadoGuardado.get<String>(CLAVE_FILTRO)
        val desde = estadoGuardado.get<Long>(CLAVE_DESDE)
        val hasta = estadoGuardado.get<Long>(CLAVE_HASTA)
        if (nombre == PERSONALIZADO && desde != null && hasta != null) {
            return SeleccionPeriodo(null, Periodo(LocalDate.ofEpochDay(desde), LocalDate.ofEpochDay(hasta)))
        }
        // Los filtros fijos se recalculan con la fecha de hoy: "Mes" sigue siendo el mes actual.
        val filtro = nombre?.let { FiltroPeriodo.valueOf(it) } ?: FiltroPeriodo.MES
        return SeleccionPeriodo(filtro, Periodo.de(filtro, hoy()))
    }

    companion object {
        private const val CLAVE_FILTRO = "filtro"
        private const val CLAVE_DESDE = "desdeEpochDay"
        private const val CLAVE_HASTA = "hastaEpochDay"
        private const val PERSONALIZADO = "PERSONALIZADO"

        /** Toma el servicio de Dependencias y crea el SavedStateHandle de esta pantalla. */
        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FinanzasApp
                InicioViewModel(app.dependencias.servicio, createSavedStateHandle(), app.inicializacion)
            }
        }
    }
}
