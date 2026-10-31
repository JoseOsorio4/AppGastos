package gt.edu.umg.finanzas.app.datos.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import gt.edu.umg.finanzas.app.datos.local.dao.CategoriaDao
import gt.edu.umg.finanzas.app.datos.local.dao.TransaccionDao
import gt.edu.umg.finanzas.app.datos.local.dao.UsuarioDao
import gt.edu.umg.finanzas.app.datos.local.entidades.CategoriaEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.TransaccionEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.UsuarioEntity

/**
 * Base Room sobre SQLite. Los esquemas exportados permiten validar la migración sin borrar datos.
 */
@Database(
    entities = [UsuarioEntity::class, CategoriaEntity::class, TransaccionEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun transaccionDao(): TransaccionDao

    companion object {
        const val NOMBRE_ARCHIVO = "finanzas.db"

        // Sin fallbackToDestructiveMigration: si falta una migración, la app falla en lugar de borrar datos.
        fun crear(context: Context, nombreArchivo: String = NOMBRE_ARCHIVO): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, nombreArchivo)
                .addMigrations(Migracion1a2, Migracion2a3)
                .build()
    }
}
