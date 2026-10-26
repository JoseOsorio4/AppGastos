package gt.edu.umg.finanzas.app.ui.comun

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ChipsPeriodoBinding
import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import java.time.LocalDate

/**
 * Conecta chips_periodo.xml con un ViewModel: cada chip llama a [alElegirFiltro] y "Período" abre el
 * selector de rango. Lo usan InicioActivity y OperacionesActivity para no repetir la misma lógica.
 */
class ChipsPeriodo(
    private val actividad: AppCompatActivity,
    private val vista: ChipsPeriodoBinding,
    private val alElegirFiltro: (FiltroPeriodo) -> Unit,
    private val alElegirRango: (LocalDate, LocalDate) -> Unit,
    private val seleccionActual: () -> SeleccionPeriodo,
) {

    init {
        vista.chipDia.setOnClickListener { alElegirFiltro(FiltroPeriodo.DIA) }
        vista.chipSemana.setOnClickListener { alElegirFiltro(FiltroPeriodo.SEMANA) }
        vista.chipMes.setOnClickListener { alElegirFiltro(FiltroPeriodo.MES) }
        vista.chipAnio.setOnClickListener { alElegirFiltro(FiltroPeriodo.ANIO) }
        vista.chipPersonalizado.setOnClickListener { abrirSelectorRango() }
        // Tras una rotación, el selector de rango sigue abierto: se le vuelven a conectar las acciones.
        // Con esa etiqueta solo se muestra el selector que crea abrirSelectorRango, por eso el cast es seguro.
        @Suppress("UNCHECKED_CAST")
        (actividad.supportFragmentManager.findFragmentByTag(ETIQUETA_SELECTOR) as? SelectorRango)?.let(::conectar)
    }

    fun pintar(seleccion: SeleccionPeriodo) {
        marcarChip(seleccion.filtro)
        vista.textoRangoPeriodo.text = actividad.getString(
            R.string.rango_periodo,
            Fechas.texto(seleccion.periodo.desde),
            Fechas.texto(seleccion.periodo.hasta),
        )
    }

    private fun marcarChip(filtro: FiltroPeriodo?) {
        val id = when (filtro) {
            FiltroPeriodo.DIA -> R.id.chipDia
            FiltroPeriodo.SEMANA -> R.id.chipSemana
            FiltroPeriodo.MES -> R.id.chipMes
            FiltroPeriodo.ANIO -> R.id.chipAnio
            null -> R.id.chipPersonalizado
        }
        if (vista.grupoPeriodo.checkedChipId != id) vista.grupoPeriodo.check(id)
    }

    private fun abrirSelectorRango() {
        if (actividad.supportFragmentManager.findFragmentByTag(ETIQUETA_SELECTOR) != null) return
        val actual = seleccionActual().periodo
        val selector = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.elegir_periodo)
            .setSelection(androidx.core.util.Pair(Fechas.aMilisUtc(actual.desde), Fechas.aMilisUtc(actual.hasta)))
            .build()
        conectar(selector)
        selector.show(actividad.supportFragmentManager, ETIQUETA_SELECTOR)
    }

    private fun conectar(selector: SelectorRango) {
        selector.addOnPositiveButtonClickListener { rango ->
            alElegirRango(Fechas.desdeMilisUtc(rango.first), Fechas.desdeMilisUtc(rango.second))
        }
        // Al cancelar, el chip tocado vuelve a marcar el período que sigue vigente.
        selector.addOnDismissListener { marcarChip(seleccionActual().filtro) }
    }

    private companion object {
        const val ETIQUETA_SELECTOR = "selectorPeriodo"
    }
}

private typealias SelectorRango = MaterialDatePicker<androidx.core.util.Pair<Long, Long>>
