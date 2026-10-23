package gt.edu.umg.finanzas.core.dominio

import java.time.LocalDate

/**
 * Lo que escribió el usuario en el formulario, sin validar. El monto llega como texto para que
 * solo Dinero lo interprete; la pantalla nunca envía el equivalente en Q ya calculado.
 */
data class DatosMovimiento(
    val tipo: TipoMovimiento,
    val montoTexto: String,
    val moneda: Moneda,
    /** Obligatoria si [moneda] es USD; en GTQ se ignora. */
    val tasa: TasaCambio?,
    val idCategoria: Long,
    val descripcion: String,
    val fecha: LocalDate,
)
