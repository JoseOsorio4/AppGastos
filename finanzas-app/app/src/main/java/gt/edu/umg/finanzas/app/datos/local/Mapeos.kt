package gt.edu.umg.finanzas.app.datos.local

import gt.edu.umg.finanzas.app.datos.local.dao.MovimientoFila
import gt.edu.umg.finanzas.app.datos.local.entidades.CategoriaEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.TransaccionEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.UsuarioEntity
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.CategoriaInicial
import gt.edu.umg.finanzas.core.dominio.Gasto
import gt.edu.umg.finanzas.core.dominio.Ingreso
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.dominio.Usuario
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
    return when (tipo) {
        TipoMovimiento.INGRESO -> Ingreso(idTransaccion, idCategoria, montoCentavos, descripcion, fecha)
        TipoMovimiento.GASTO -> Gasto(idTransaccion, idCategoria, montoCentavos, descripcion, fecha)
    }
}

/** [idTransaccion] es SIN_ID al insertar (Room asigna el ID) y el ID existente al editar. */
fun Transaccion.aEntity(idTransaccion: Long): TransaccionEntity {
    // La versión 1 de la base no tiene columnas para la conversión USD.
    require(conversion == null) { "La base v1 solo guarda movimientos en GTQ" }
    return TransaccionEntity(
        idTransaccion = idTransaccion,
        idUsuario = Usuario.ID_UNICO,
        idCategoria = idCategoria,
        tipo = tipo,
        montoCentavos = montoCentavos,
        descripcion = descripcion,
        fechaEpochDay = fecha.toEpochDay(),
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
        conversion = null,
    )
