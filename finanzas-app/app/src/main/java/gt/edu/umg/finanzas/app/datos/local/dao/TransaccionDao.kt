package gt.edu.umg.finanzas.app.datos.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import gt.edu.umg.finanzas.app.datos.local.entidades.TransaccionEntity
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.TotalCategoria
import gt.edu.umg.finanzas.core.dominio.TotalesPeriodo
import kotlinx.coroutines.flow.Flow

/**
 * SQL de los movimientos. Las escrituras solo se llaman desde OperacionesMovimientoRoom,
 * dentro de su withTransaction. Los textos 'INGRESO' y 'GASTO' son los nombres del enum TipoMovimiento.
 */
@Dao
interface TransaccionDao {

    @Insert
    suspend fun insertar(transaccion: TransaccionEntity): Long

    @Update
    suspend fun actualizar(transaccion: TransaccionEntity): Int

    @Query("DELETE FROM Transaccion WHERE idTransaccion = :idTransaccion")
    suspend fun eliminar(idTransaccion: Long): Int

    @Query("SELECT * FROM Transaccion WHERE idTransaccion = :idTransaccion")
    suspend fun obtenerPorId(idTransaccion: Long): TransaccionEntity?

    @Query("SELECT COUNT(*) FROM Transaccion")
    suspend fun contar(): Int

    /** Historial completo: solo para la reconstrucción excepcional del saldo, nunca para Inicio. */
    @Query("SELECT * FROM Transaccion")
    suspend fun obtenerTodas(): List<TransaccionEntity>

    // Una sola fila siempre; COALESCE da 0 cuando el intervalo no tiene movimientos.
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN tipo = 'INGRESO' THEN montoCentavos END), 0) AS ingresosCentavos,
               COALESCE(SUM(CASE WHEN tipo = 'GASTO' THEN montoCentavos END), 0) AS gastosCentavos
        FROM Transaccion
        WHERE fechaEpochDay BETWEEN :desde AND :hasta
        """
    )
    fun observarTotales(desde: Long, hasta: Long): Flow<TotalesPeriodo>

    @Query(
        """
        SELECT c.clave AS clave, c.nombre AS nombre, SUM(t.montoCentavos) AS totalCentavos
        FROM Transaccion t JOIN Categoria c ON c.idCategoria = t.idCategoria
        WHERE t.tipo = 'GASTO' AND t.fechaEpochDay BETWEEN :desde AND :hasta
        GROUP BY c.idCategoria
        ORDER BY totalCentavos DESC
        """
    )
    fun observarGastosPorCategoria(desde: Long, hasta: Long): Flow<List<TotalCategoria>>

    /** [tipo] null trae ingresos y gastos. */
    @Query(
        """
        SELECT t.idTransaccion, t.tipo, t.montoCentavos, t.descripcion, t.fechaEpochDay,
               c.clave AS claveCategoria, c.nombre AS nombreCategoria
        FROM Transaccion t JOIN Categoria c ON c.idCategoria = t.idCategoria
        WHERE t.fechaEpochDay BETWEEN :desde AND :hasta
          AND (:tipo IS NULL OR t.tipo = :tipo)
        ORDER BY t.fechaEpochDay DESC, t.idTransaccion DESC
        """
    )
    fun observarMovimientos(desde: Long, hasta: Long, tipo: TipoMovimiento?): Flow<List<MovimientoFila>>
}
