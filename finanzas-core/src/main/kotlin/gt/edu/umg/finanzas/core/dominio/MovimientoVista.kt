package gt.edu.umg.finanzas.core.dominio

import java.time.LocalDate

/** Fila de Operaciones: el movimiento con el nombre y la clave de su categoría ya resueltos. */
data class MovimientoVista(
    val idTransaccion: Long,
    val tipo: TipoMovimiento,
    val montoCentavos: Long,
    val descripcion: String,
    val fecha: LocalDate,
    val claveCategoria: String,
    val nombreCategoria: String,
    /** Para mostrar el original USD y la tasa en el detalle. */
    val conversion: ConversionAplicada?,
)
