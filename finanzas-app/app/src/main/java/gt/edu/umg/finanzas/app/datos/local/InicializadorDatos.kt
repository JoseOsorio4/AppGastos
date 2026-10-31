package gt.edu.umg.finanzas.app.datos.local

import androidx.room.withTransaction
import gt.edu.umg.finanzas.app.datos.local.entidades.UsuarioEntity
import gt.edu.umg.finanzas.core.dominio.CategoriasIniciales
import gt.edu.umg.finanzas.core.dominio.Usuario
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.CancellationException

/**
 * Crea el usuario 1 y las 12 categorías del core. Se ejecuta en cada arranque: INSERT OR IGNORE
 * por idUsuario y por `clave` hace que repetirlo no duplique filas ni reinicie el saldo.
 */
class InicializadorDatos(private val baseDatos: AppDatabase) {

    suspend fun inicializar(): Resultado<Unit> =
        try {
            baseDatos.withTransaction {
                baseDatos.usuarioDao().insertarSiNoExiste(
                    UsuarioEntity(idUsuario = Usuario.ID_UNICO, nombrePerfil = NOMBRE_PERFIL, saldoCentavos = 0),
                )
                baseDatos.categoriaDao().insertarSiNoExisten(CategoriasIniciales.lista.map { it.aEntity() })
            }
            Resultado.Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resultado.ErrorAlmacenamiento(e)
        }

    private companion object {
        // La bienvenida completa el nombre nuevo; IGNORE conserva el perfil de una actualización.
        const val NOMBRE_PERFIL = ""
    }
}
