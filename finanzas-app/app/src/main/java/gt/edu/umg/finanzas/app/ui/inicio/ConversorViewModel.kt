package gt.edu.umg.finanzas.app.ui.inicio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.app.datos.repositorios.EstadoTasa
import gt.edu.umg.finanzas.app.datos.repositorios.RepositorioTipoCambio
import gt.edu.umg.finanzas.app.ui.comun.ConversionMonto
import gt.edu.umg.finanzas.app.ui.comun.SentidoConversion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ConversorUiState(val monto: String, val sentido: SentidoConversion, val tasa: EstadoTasa) {
    val resultado get() = ConversionMonto.calcular(monto, sentido, tasa.dato?.tasa)
}

class ConversorViewModel(
    private val guardado: SavedStateHandle,
    tipoCambio: RepositorioTipoCambio,
) : ViewModel() {
    private val monto = guardado.getStateFlow("monto", "")
    private val sentido = guardado.getStateFlow("sentido", SentidoConversion.USD_A_GTQ.name)
    val estado = combine(monto, sentido, tipoCambio.estado) { texto, direccion, tasa ->
        ConversorUiState(texto, SentidoConversion.valueOf(direccion), tasa)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConversorUiState(monto.value, SentidoConversion.valueOf(sentido.value), tipoCambio.estado.value),
    )

    fun cambiarMonto(texto: String) { guardado["monto"] = texto }
    fun elegirSentido(sentido: SentidoConversion) { guardado["sentido"] = sentido.name }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FinanzasApp
                ConversorViewModel(createSavedStateHandle(), app.dependencias.tipoCambio)
            }
        }
    }
}
