package gt.edu.umg.finanzas.core.dominio

import java.time.LocalDate

/**
 * Movimiento ya validado. [Ingreso] y [Gasto] heredan los mismos datos; solo cambia el signo
 * de [impactoEnSaldo], y por eso CalculoSaldo nunca pregunta "¿es gasto?".
 * Desde el formulario se crea con ValidadorMovimiento; al leer de la base, con sus constructores.
 */
sealed class Transaccion(
    val idTransaccion: Long,
    val idCategoria: Long,
    /** Centavos de GTQ. Saldo, totales y anillo usan solo este monto. */
    val montoCentavos: Long,
    val descripcion: String,
    val fecha: LocalDate,
    /** Solo en movimientos USD; null en GTQ. */
    val conversion: ConversionAplicada?,
) {
    // Invariantes que también protegen los datos reconstruidos desde la base.
    init {
        require(idTransaccion >= 0) { "ID negativo: $idTransaccion" }
        require(montoCentavos in 1..MONTO_MAXIMO_CENTAVOS) { "Monto fuera de rango: $montoCentavos" }
        require(descripcion.isNotBlank() && descripcion.length <= DESCRIPCION_MAXIMA) {
            "Descripción vacía o mayor de $DESCRIPCION_MAXIMA caracteres"
        }
    }

    abstract val tipo: TipoMovimiento

    /** Centavos que este movimiento suma (+) o resta (−) al saldo. */
    abstract fun impactoEnSaldo(): Long

    /** La misma transacción con el ID que asignó el almacenamiento al insertarla. */
    abstract fun conId(id: Long): Transaccion

    companion object {
        /** 9,999,999.99 por movimiento, en la moneda original y en su equivalente en Q. */
        const val MONTO_MAXIMO_CENTAVOS = 999_999_999L
        const val DESCRIPCION_MAXIMA = 60

        /** ID de una transacción que todavía no se guarda. */
        const val SIN_ID = 0L
    }
}

class Ingreso(
    idTransaccion: Long,
    idCategoria: Long,
    montoCentavos: Long,
    descripcion: String,
    fecha: LocalDate,
    conversion: ConversionAplicada? = null,
) : Transaccion(idTransaccion, idCategoria, montoCentavos, descripcion, fecha, conversion) {

    override val tipo: TipoMovimiento get() = TipoMovimiento.INGRESO

    override fun impactoEnSaldo(): Long = montoCentavos

    override fun conId(id: Long): Transaccion =
        Ingreso(id, idCategoria, montoCentavos, descripcion, fecha, conversion)
}

class Gasto(
    idTransaccion: Long,
    idCategoria: Long,
    montoCentavos: Long,
    descripcion: String,
    fecha: LocalDate,
    conversion: ConversionAplicada? = null,
) : Transaccion(idTransaccion, idCategoria, montoCentavos, descripcion, fecha, conversion) {

    override val tipo: TipoMovimiento get() = TipoMovimiento.GASTO

    override fun impactoEnSaldo(): Long = -montoCentavos

    override fun conId(id: Long): Transaccion =
        Gasto(id, idCategoria, montoCentavos, descripcion, fecha, conversion)
}
