package gt.edu.umg.finanzas.app.datos.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import gt.edu.umg.finanzas.app.datos.local.entidades.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    /** Room vuelve a emitir cada vez que cambia la tabla Usuario (por ejemplo, el saldo). */
    @Query("SELECT * FROM Usuario WHERE idUsuario = :idUsuario")
    fun observarPorId(idUsuario: Long): Flow<UsuarioEntity?>

    @Query("SELECT * FROM Usuario WHERE idUsuario = :idUsuario")
    suspend fun obtenerPorId(idUsuario: Long): UsuarioEntity?

    /** IGNORE: si el usuario ya existe no se toca su saldo. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(usuario: UsuarioEntity): Long

    /** Asigna el saldo final ya comprobado con CalculoSaldo.aplicar; nunca suma en SQL. */
    @Query("UPDATE Usuario SET saldoCentavos = :saldoCentavos WHERE idUsuario = :idUsuario")
    suspend fun actualizarSaldo(idUsuario: Long, saldoCentavos: Long): Int

    /** Enciende o apaga la bandera que pide recalcular el saldo desde el historial. */
    @Query("UPDATE Usuario SET reconstruccionPendiente = :pendiente WHERE idUsuario = :idUsuario")
    suspend fun marcarReconstruccion(idUsuario: Long, pendiente: Boolean): Int

    @Query("SELECT COUNT(*) FROM Usuario")
    suspend fun contar(): Int
}
