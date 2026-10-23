package gt.edu.umg.finanzas.app.datos.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import gt.edu.umg.finanzas.app.datos.local.entidades.CategoriaEntity
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM Categoria WHERE activa = 1 AND tipo = :tipo ORDER BY idCategoria")
    fun observarActivasPorTipo(tipo: TipoMovimiento): Flow<List<CategoriaEntity>>

    /** Incluye las archivadas: un movimiento antiguo puede seguir usándolas. */
    @Query("SELECT * FROM Categoria WHERE idCategoria = :idCategoria")
    suspend fun obtenerPorId(idCategoria: Long): CategoriaEntity?

    /** IGNORE sobre el índice único de `clave`: repetir la siembra no duplica categorías. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExisten(categorias: List<CategoriaEntity>): List<Long>

    @Query("SELECT * FROM Categoria ORDER BY idCategoria")
    suspend fun obtenerTodas(): List<CategoriaEntity>
}
