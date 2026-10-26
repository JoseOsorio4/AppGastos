package gt.edu.umg.finanzas.app.ui.inicio

import gt.edu.umg.finanzas.app.ui.comun.SeleccionPeriodo
import gt.edu.umg.finanzas.core.dominio.ResumenPeriodo
import gt.edu.umg.finanzas.core.dominio.SegmentoGasto

/** Todo lo que InicioActivity dibuja. [resumen] es null mientras Room todavía no responde. */
data class InicioUiState(
    val seleccion: SeleccionPeriodo,
    val resumen: ResumenPeriodo? = null,
    val segmentos: List<SegmentoGasto> = emptyList(),
    val errorInicial: Boolean = false,
)
