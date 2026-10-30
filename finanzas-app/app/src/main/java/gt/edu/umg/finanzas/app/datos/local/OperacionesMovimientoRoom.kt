package gt.edu.umg.finanzas.app.datos.local

import androidx.room.withTransaction
import gt.edu.umg.finanzas.app.datos.local.dao.CategoriaDao
import gt.edu.umg.finanzas.app.datos.local.dao.TransaccionDao
import gt.edu.umg.finanzas.app.datos.local.dao.UsuarioDao
import gt.edu.umg.finanzas.core.contratos.OperacionesMovimiento
import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.dominio.Usuario
import gt.edu.umg.finanzas.core.reglas.CalculoSaldo
import gt.edu.umg.finanzas.core.reglas.ValidadorMovimiento
import gt.edu.umg.finanzas.core.resultado.Resultado
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

/**
 * Único lugar que abre withTransaction para movimientos: el movimiento y el saldo se escriben
 * juntos o no se escribe nada. Orden en los tres métodos: leer → validar → comprobar el saldo
 * final → escribir. Antes de la primera escritura se retorna un Fallo; después, toda anomalía
 * lanza una excepción y Room revierte el bloque completo.
 * Los DAO se reciben aparte para que la prueba del caso c inyecte uno que falla.
 */
class OperacionesMovimientoRoom(
    private val baseDatos: AppDatabase,
    private val usuarios: UsuarioDao = baseDatos.usuarioDao(),
    private val categorias: CategoriaDao = baseDatos.categoriaDao(),
    private val transacciones: TransaccionDao = baseDatos.transaccionDao(),
) : OperacionesMovimiento {

    override suspend fun crear(datos: DatosMovimiento, hoy: LocalDate): Resultado<Long> = enTransaccion {
        val categoria = leerCategoria(datos) ?: return@enTransaccion categoriaInexistente()
        val nueva = when (val validacion = ValidadorMovimiento.validar(datos, categoria, hoy)) {
            is Resultado.Ok -> validacion.valor
            is Resultado.Fallo -> return@enTransaccion validacion
        }
        val saldoNuevo = when (val saldo = CalculoSaldo.aplicar(saldoActual(), CalculoSaldo.deltaCrear(nueva))) {
            is Resultado.Ok -> saldo.valor
            is Resultado.Fallo -> return@enTransaccion saldo
        }

        val id = transacciones.insertar(nueva.aEntity(Transaccion.SIN_ID))
        guardarSaldo(saldoNuevo)
        Resultado.Ok(id)
    }

    override suspend fun editar(id: Long, datos: DatosMovimiento, hoy: LocalDate): Resultado<Unit> = enTransaccion {
        val anterior = transacciones.obtenerPorId(id)?.aDominio() ?: return@enTransaccion Resultado.NoEncontrado
        val categoria = leerCategoria(datos) ?: return@enTransaccion categoriaInexistente()
        // En edición USD se conserva la tasa del registro, incluso si la caché ya tiene otra.
        val tasaAnterior = anterior.conversion?.tasa
        val datosConTasa = if (datos.moneda == Moneda.USD && tasaAnterior != null) {
            datos.copy(tasa = tasaAnterior)
        } else datos
        val nueva = when (val validacion = ValidadorMovimiento.validar(datosConTasa, categoria, hoy, anterior.idCategoria)) {
            is Resultado.Ok -> validacion.valor
            is Resultado.Fallo -> return@enTransaccion validacion
        }
        val delta = CalculoSaldo.deltaEditar(anterior, nueva)
        val saldoNuevo = when (val saldo = CalculoSaldo.aplicar(saldoActual(), delta)) {
            is Resultado.Ok -> saldo.valor
            is Resultado.Fallo -> return@enTransaccion saldo
        }

        check(transacciones.actualizar(nueva.aEntity(id)) == 1) { "No se actualizó el movimiento $id" }
        guardarSaldo(saldoNuevo)
        Resultado.Ok(Unit)
    }

    override suspend fun eliminar(id: Long): Resultado<Unit> = enTransaccion {
        val anterior = transacciones.obtenerPorId(id)?.aDominio() ?: return@enTransaccion Resultado.NoEncontrado
        val saldoNuevo = when (val saldo = CalculoSaldo.aplicar(saldoActual(), CalculoSaldo.deltaEliminar(anterior))) {
            is Resultado.Ok -> saldo.valor
            is Resultado.Fallo -> return@enTransaccion saldo
        }

        check(transacciones.eliminar(id) == 1) { "No se eliminó el movimiento $id" }
        guardarSaldo(saldoNuevo)
        Resultado.Ok(Unit)
    }

    // Una excepción dentro de withTransaction ya produjo el ROLLBACK; aquí solo se traduce a Resultado.
    private suspend fun <T> enTransaccion(bloque: suspend () -> Resultado<T>): Resultado<T> =
        try {
            baseDatos.withTransaction { bloque() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.ErrorAlmacenamiento(e)
        }

    // La categoría se lee dentro de la transacción para validar su tipo contra el movimiento.
    private suspend fun leerCategoria(datos: DatosMovimiento): Categoria? =
        categorias.obtenerPorId(datos.idCategoria)?.aDominio()

    private suspend fun saldoActual(): Long =
        checkNotNull(usuarios.obtenerPorId(Usuario.ID_UNICO)) { "Falta el usuario; InicializadorDatos no corrió" }
            .saldoCentavos

    private suspend fun guardarSaldo(saldoNuevo: Long) {
        check(usuarios.actualizarSaldo(Usuario.ID_UNICO, saldoNuevo) == 1) { "No se actualizó el saldo" }
    }

    private fun categoriaInexistente() = Resultado.ErrorNegocio("Categoría inexistente", CampoMovimiento.CATEGORIA)

}
