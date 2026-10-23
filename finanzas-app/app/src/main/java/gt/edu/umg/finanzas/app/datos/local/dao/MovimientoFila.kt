package gt.edu.umg.finanzas.app.datos.local.dao

import gt.edu.umg.finanzas.core.dominio.TipoMovimiento

/** Resultado del JOIN Transaccion + Categoria de TransaccionDao.observarMovimientos. */
data class MovimientoFila(
    val idTransaccion: Long,
    val tipo: TipoMovimiento,
    val montoCentavos: Long,
    val descripcion: String,
    val fechaEpochDay: Long,
    val claveCategoria: String,
    val nombreCategoria: String,
)
