package gt.edu.umg.finanzas.core.dominio

/**
 * Único usuario local. [saldoCentavos] es el saldo acumulado en GTQ: se guarda para que Inicio
 * no recorra el historial. [reconstruccionPendiente] pide recalcularlo desde los movimientos.
 */
data class Usuario(
    val idUsuario: Long,
    val nombrePerfil: String,
    val saldoCentavos: Long,
    val reconstruccionPendiente: Boolean = false,
) {
    companion object {
        const val ID_UNICO = 1L
    }
}
