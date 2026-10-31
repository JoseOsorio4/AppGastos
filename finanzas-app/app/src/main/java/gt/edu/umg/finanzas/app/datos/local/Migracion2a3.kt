package gt.edu.umg.finanzas.app.datos.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Los USD anteriores mantienen su referencia histórica como tasa aplicada y recargo cero. */
object Migracion2a3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Transaccion ADD COLUMN recargoGtqPorUsd TEXT NOT NULL DEFAULT '0'")
        db.execSQL("ALTER TABLE Transaccion ADD COLUMN tasaAplicadaGtqPorUsd TEXT NOT NULL DEFAULT '1'")
        db.execSQL("UPDATE Transaccion SET tasaAplicadaGtqPorUsd = tasaGtqPorUsd")
    }
}
