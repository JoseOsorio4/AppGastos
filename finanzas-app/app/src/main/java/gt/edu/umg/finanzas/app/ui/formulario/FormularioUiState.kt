package gt.edu.umg.finanzas.app.ui.formulario

import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import java.time.LocalDate

/**
 * Campos tal como los escribió el usuario, más el estado de guardado. El monto se conserva como
 * texto para no mover el cursor ni "corregir" lo que el usuario escribe.
 */
data class FormularioUiState(
    val tipo: TipoMovimiento = TipoMovimiento.GASTO,
    val montoTexto: String = "",
    val idCategoria: Long? = null,
    val fecha: LocalDate,
    val descripcion: String = "",
    val categorias: List<Categoria> = emptyList(),
    val errores: ErroresFormulario = ErroresFormulario(),
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
)

/**
 * Un mensaje por campo. Los textos de monto, fecha y descripción vienen del core; [faltaCategoria]
 * y [falloAlGuardar] los traduce la Activity con strings.xml.
 */
data class ErroresFormulario(
    val monto: String? = null,
    val categoria: String? = null,
    val fecha: String? = null,
    val descripcion: String? = null,
    val general: String? = null,
    val faltaCategoria: Boolean = false,
    val falloAlGuardar: Boolean = false,
)
