package gt.edu.umg.finanzas.app.ui.formulario

import gt.edu.umg.finanzas.app.datos.repositorios.EstadoTasa
import gt.edu.umg.finanzas.app.ui.comun.ConversionMonto
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.reglas.ConversorMoneda
import java.math.BigDecimal
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
    /** true cuando se abrió desde el detalle de Operaciones para corregir un movimiento guardado. */
    val enEdicion: Boolean = false,
    /** Mientras se leen de Room los datos del movimiento a editar, Guardar queda deshabilitado. */
    val cargandoMovimiento: Boolean = false,
    val guardando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val moneda: Moneda = Moneda.GTQ,
    val tasaHistorica: TasaCambio? = null,
    val recargoHistorico: BigDecimal? = null,
    val tipoHistorico: TipoMovimiento? = null,
    val estadoTasa: EstadoTasa = EstadoTasa(),
) {
    val tasa: TasaCambio? get() = if (moneda == Moneda.USD) tasaHistorica ?: estadoTasa.dato?.tasa else null
    val usdDisponible: Boolean get() = tasaHistorica != null || estadoTasa.dato != null
    val recargoGtqPorUsd: BigDecimal get() = ConversorMoneda.recargoPara(tipo, tipoHistorico, recargoHistorico)
    val conversionPrevia get() = ConversionMonto.paraMovimiento(montoTexto, tasa, recargoGtqPorUsd)
}

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
    /** Se pidió editar un ID que otra acción ya eliminó. */
    val movimientoNoEncontrado: Boolean = false,
    val falloAlCargar: Boolean = false,
)
