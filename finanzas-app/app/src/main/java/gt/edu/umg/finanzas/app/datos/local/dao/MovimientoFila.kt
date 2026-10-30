package gt.edu.umg.finanzas.app.datos.local.dao

import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import java.math.BigDecimal

/** Resultado del JOIN Transaccion + Categoria de TransaccionDao.observarMovimientos. */
data class MovimientoFila(
    val idTransaccion: Long,
    val tipo: TipoMovimiento,
    val montoCentavos: Long,
    val descripcion: String,
    val fechaEpochDay: Long,
    val claveCategoria: String,
    val nombreCategoria: String,
    val monedaOriginal: Moneda,
    val montoOriginalCentavos: Long,
    val tasaGtqPorUsd: BigDecimal,
    val fechaTasaEpochDay: Long?,
    val fuenteTasa: String?,
)
