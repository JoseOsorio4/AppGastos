package gt.edu.umg.finanzas.app.datos.local.entidades

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.reglas.ConversorMoneda
import java.math.BigDecimal

/**
 * Tabla `Transaccion`: Ingreso y Gasto comparten esta tabla y se distinguen por `tipo`.
 * Las FK usan RESTRICT: no se puede borrar un usuario ni una categoría con movimientos.
 * La conversión conserva la tasa histórica; los reportes siguen usando montoCentavos en GTQ.
 */
@Entity(
    tableName = "Transaccion",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["idCategoria"],
            childColumns = ["idCategoria"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("fechaEpochDay"),
        // Totales por tipo dentro de un intervalo sin recorrer toda la tabla.
        Index("tipo", "fechaEpochDay"),
        // Room pide indexar las columnas hijas de una FK.
        Index("idCategoria"),
        Index("idUsuario"),
    ],
)
data class TransaccionEntity(
    @PrimaryKey(autoGenerate = true) val idTransaccion: Long = 0,
    val idUsuario: Long,
    val idCategoria: Long,
    val tipo: TipoMovimiento,
    /** Centavos de GTQ; Transaccion del core garantiza que sea mayor que 0. */
    val montoCentavos: Long,
    val descripcion: String,
    /** LocalDate.toEpochDay(): un entero ordena y filtra por fecha sin texto. */
    val fechaEpochDay: Long,
    @ColumnInfo(defaultValue = "'GTQ'") val monedaOriginal: Moneda = Moneda.GTQ,
    @ColumnInfo(defaultValue = "0") val montoOriginalCentavos: Long = montoCentavos,
    @ColumnInfo(defaultValue = "'1'") val tasaGtqPorUsd: BigDecimal = BigDecimal.ONE,
    val fechaTasaEpochDay: Long? = null,
    val fuenteTasa: String? = null,
    @ColumnInfo(defaultValue = "'0'") val recargoGtqPorUsd: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(defaultValue = "'1'") val tasaAplicadaGtqPorUsd: BigDecimal =
        ConversorMoneda.tasaAplicada(tasaGtqPorUsd, recargoGtqPorUsd),
)
