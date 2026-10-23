package gt.edu.umg.finanzas.app.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento

/**
 * Tabla `Categoria`. El índice único en `clave` es lo que permite sembrar con INSERT OR IGNORE
 * sin duplicar. `tipo` se guarda como el texto del enum ("INGRESO" o "GASTO").
 */
@Entity(
    tableName = "Categoria",
    indices = [Index(value = ["clave"], unique = true)],
)
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val idCategoria: Long = 0,
    val clave: String,
    val nombre: String,
    val tipo: TipoMovimiento,
    @ColumnInfo(defaultValue = "1") val activa: Boolean = true,
)
