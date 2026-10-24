package gt.edu.umg.finanzas.app.datos.local

import androidx.room.withTransaction
import gt.edu.umg.finanzas.app.datos.local.dao.TransaccionDao
import gt.edu.umg.finanzas.app.datos.local.dao.UsuarioDao
import gt.edu.umg.finanzas.core.contratos.ReconstructorSaldo
import gt.edu.umg.finanzas.core.dominio.Usuario
import gt.edu.umg.finanzas.core.reglas.CalculoSaldo
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.CancellationException

/**
 * Recalcula el saldo desde los movimientos solo cuando `reconstruccionPendiente` está encendida (§9.6).
 * El saldo calculado se asigna, no se suma al anterior, y la bandera se apaga en la misma transacción:
 * si algo falla, saldo y bandera quedan como estaban y repetir la llamada vuelve a intentarlo.
 * Los DAO se reciben aparte para que la prueba de rollback inyecte uno que falla.
 */
class ReconstructorSaldoRoom(
    private val baseDatos: AppDatabase,
    private val usuarios: UsuarioDao = baseDatos.usuarioDao(),
    private val transacciones: TransaccionDao = baseDatos.transaccionDao(),
) : ReconstructorSaldo {

    override suspend fun reconstruirSiPendiente(): Resultado<Long> =
        try {
            baseDatos.withTransaction {
                val usuario = checkNotNull(usuarios.obtenerPorId(Usuario.ID_UNICO)) { "Falta el usuario" }
                // La condición es la bandera, nunca "saldo == 0": un saldo en cero puede ser correcto.
                if (!usuario.reconstruccionPendiente) return@withTransaction Resultado.Ok(usuario.saldoCentavos)

                val historial = transacciones.obtenerTodas().asSequence().map { it.aDominio() }
                val saldo = when (val calculado = CalculoSaldo.saldoDesde(historial)) {
                    is Resultado.Ok -> calculado.valor
                    is Resultado.Fallo -> return@withTransaction calculado
                }
                check(usuarios.actualizarSaldo(Usuario.ID_UNICO, saldo) == 1) { "No se actualizó el saldo" }
                check(usuarios.marcarReconstruccion(Usuario.ID_UNICO, pendiente = false) == 1) { "No se apagó la bandera" }
                Resultado.Ok(saldo)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.ErrorAlmacenamiento(e)
        }
}
