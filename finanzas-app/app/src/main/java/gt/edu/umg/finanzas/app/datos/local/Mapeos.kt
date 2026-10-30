package gt.edu.umg.finanzas.app.datos.local

import gt.edu.umg.finanzas.app.datos.local.dao.MovimientoFila
import gt.edu.umg.finanzas.app.datos.local.entidades.CategoriaEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.TransaccionEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.UsuarioEntity
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.CategoriaInicial
import gt.edu.umg.finanzas.core.dominio.ConversionAplicada
import gt.edu.umg.finanzas.core.dominio.Gasto
import gt.edu.umg.finanzas.core.dominio.Ingreso
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.dominio.Usuario
import java.math.BigDecimal
import java.time.LocalDate

// Conversiones entity ↔ dominio. Es el único archivo que conoce ambos modelos.

fun UsuarioEntity.aDominio(): Usuario =
    Usuario(idUsuario, nombrePerfil, saldoCentavos, reconstruccionPendiente)

fun CategoriaEntity.aDominio(): Categoria =
    Categoria(idCategoria, clave, nombre, tipo, activa)

fun CategoriaInicial.aEntity(): CategoriaEntity =
    CategoriaEntity(clave = clave, nombre = nombre, tipo = tipo)

/** La columna `tipo` decide qué subclase se reconstruye; sus invariantes se comprueban al construirla. */
fun TransaccionEntity.aDominio(): Transaccion {
    val fecha = LocalDate.ofEpochDay(fechaEpochDay)
    val conversion = conversionDe(monedaOriginal, montoOriginalCentavos, tasaGtqPorUsd, fechaTasaEpochDay, fuenteTasa)
    return when (tipo) {
        TipoMovimiento.INGRESO -> Ingreso(idTransaccion, idCategoria, montoCentavos, descripcion, fecha, conversion)
        TipoMovimiento.GASTO -> Gasto(idTransaccion, idCategoria, montoCentavos, descripcion, fecha, conversion)
    }
}

/** [idTransaccion] es SIN_ID al insertar (Room asigna el ID) y el ID existente al editar. */
fun Transaccion.aEntity(idTransaccion: Long): TransaccionEntity {
    return TransaccionEntity(
        idTransaccion = idTransaccion,
        idUsuario = Usuario.ID_UNICO,
        idCategoria = idCategoria,
        tipo = tipo,
        montoCentavos = montoCentavos,
        descripcion = descripcion,
        fechaEpochDay = fecha.toEpochDay(),
        monedaOriginal = if (conversion == null) Moneda.GTQ else Moneda.USD,
        montoOriginalCentavos = conversion?.montoOriginalCentavos ?: montoCentavos,
        tasaGtqPorUsd = conversion?.tasa?.gtqPorUsd ?: BigDecimal.ONE,
        fechaTasaEpochDay = conversion?.tasa?.fechaReferencia?.toEpochDay(),
        fuenteTasa = conversion?.tasa?.fuente,
    )
}

fun MovimientoFila.aVista(): MovimientoVista =
    MovimientoVista(
        idTransaccion = idTransaccion,
        tipo = tipo,
        montoCentavos = montoCentavos,
        descripcion = descripcion,
        fecha = LocalDate.ofEpochDay(fechaEpochDay),
        claveCategoria = claveCategoria,
        nombreCategoria = nombreCategoria,
        conversion = conversionDe(monedaOriginal, montoOriginalCentavos, tasaGtqPorUsd, fechaTasaEpochDay, fuenteTasa),
    )

private fun conversionDe(moneda: Moneda, original: Long, tasa: BigDecimal, fecha: Long?, fuente: String?): ConversionAplicada? =
    if (moneda == Moneda.GTQ) null else ConversionAplicada(
        original,
        TasaCambio(tasa, LocalDate.ofEpochDay(checkNotNull(fecha)), checkNotNull(fuente)),
    )
