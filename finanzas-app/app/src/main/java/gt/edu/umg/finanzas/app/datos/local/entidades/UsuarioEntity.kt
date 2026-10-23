package gt.edu.umg.finanzas.app.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabla `Usuario`: una sola fila (idUsuario = 1) con el saldo acumulado en centavos de GTQ. */
@Entity(tableName = "Usuario")
data class UsuarioEntity(
    @PrimaryKey val idUsuario: Long,
    val nombrePerfil: String,
    val saldoCentavos: Long,
    @ColumnInfo(defaultValue = "0") val reconstruccionPendiente: Boolean = false,
)
