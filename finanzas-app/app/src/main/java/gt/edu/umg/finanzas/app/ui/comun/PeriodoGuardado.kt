package gt.edu.umg.finanzas.app.ui.comun

import androidx.lifecycle.SavedStateHandle
import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** Período elegido en los chips. [filtro] es null cuando el usuario eligió un rango personalizado. */
data class SeleccionPeriodo(
    val filtro: FiltroPeriodo?,
    val periodo: Periodo,
)

/**
 * Estado del período que comparten InicioViewModel y OperacionesViewModel. Cada pantalla tiene
 * su propio objeto y lo copia a su SavedStateHandle; los límites los calcula Periodo del core.
 */
class PeriodoGuardado(
    private val estadoGuardado: SavedStateHandle,
    private val hoy: () -> LocalDate,
) {
    private val _seleccion = MutableStateFlow(leer())
    val seleccion: StateFlow<SeleccionPeriodo> = _seleccion.asStateFlow()

    fun elegirFiltro(filtro: FiltroPeriodo) {
        cambiar(SeleccionPeriodo(filtro, Periodo.de(filtro, hoy())))
    }

    fun elegirRango(desde: LocalDate, hasta: LocalDate) {
        // El selector de rango ya entrega desde ≤ hasta; un rango inválido simplemente no se aplica.
        val periodo = Periodo.personalizado(desde, hasta)
        if (periodo is Resultado.Ok) cambiar(SeleccionPeriodo(null, periodo.valor))
    }

    private fun cambiar(nueva: SeleccionPeriodo) {
        _seleccion.value = nueva
        estadoGuardado[CLAVE_FILTRO] = nueva.filtro?.name ?: PERSONALIZADO
        estadoGuardado[CLAVE_DESDE] = nueva.periodo.desde.toEpochDay()
        estadoGuardado[CLAVE_HASTA] = nueva.periodo.hasta.toEpochDay()
    }

    private fun leer(): SeleccionPeriodo {
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

    private companion object {
        const val CLAVE_FILTRO = "filtro"
        const val CLAVE_DESDE = "desdeEpochDay"
        const val CLAVE_HASTA = "hastaEpochDay"
        const val PERSONALIZADO = "PERSONALIZADO"
    }
}
