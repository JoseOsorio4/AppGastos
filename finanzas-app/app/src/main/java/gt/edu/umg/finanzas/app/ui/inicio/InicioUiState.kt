package gt.edu.umg.finanzas.app.ui.inicio

import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.ResumenPeriodo
import gt.edu.umg.finanzas.core.dominio.SegmentoGasto

/** Período elegido en los chips. [filtro] es null cuando el usuario eligió un rango personalizado. */
data class SeleccionPeriodo(
    val filtro: FiltroPeriodo?,
    val periodo: Periodo,
)

/** Todo lo que InicioActivity dibuja. [resumen] es null mientras Room todavía no responde. */
data class InicioUiState(
    val seleccion: SeleccionPeriodo,
    val resumen: ResumenPeriodo? = null,
    val segmentos: List<SegmentoGasto> = emptyList(),
    val errorInicial: Boolean = false,
)
