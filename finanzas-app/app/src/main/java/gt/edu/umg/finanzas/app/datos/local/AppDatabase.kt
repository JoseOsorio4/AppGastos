package gt.edu.umg.finanzas.app.datos.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import gt.edu.umg.finanzas.app.datos.local.dao.CategoriaDao
import gt.edu.umg.finanzas.app.datos.local.dao.TransaccionDao
import gt.edu.umg.finanzas.app.datos.local.dao.UsuarioDao
import gt.edu.umg.finanzas.app.datos.local.entidades.CategoriaEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.TransaccionEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.UsuarioEntity

/**
 * Base Room sobre SQLite, versión 1. exportSchema deja el esquema en app/schemas para
 * comparar contra él en la migración 1 → 2 del Inc. 6.
 */
@Database(
    entities = [UsuarioEntity::class, CategoriaEntity::class, TransaccionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun transaccionDao(): TransaccionDao

    companion object {
        const val NOMBRE_ARCHIVO = "finanzas.db"

        // Sin fallbackToDestructiveMigration: si falta una migración, la app falla en lugar de borrar datos.
        fun crear(context: Context, nombreArchivo: String = NOMBRE_ARCHIVO): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, nombreArchivo).build()
    }
}
