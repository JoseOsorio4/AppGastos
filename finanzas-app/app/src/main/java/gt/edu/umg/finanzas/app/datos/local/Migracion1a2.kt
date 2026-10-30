package gt.edu.umg.finanzas.app.datos.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Añade la fotografía USD sin recalcular ningún importe ni saldo de la v1. */
object Migracion1a2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Transaccion ADD COLUMN monedaOriginal TEXT NOT NULL DEFAULT 'GTQ'")
        db.execSQL("ALTER TABLE Transaccion ADD COLUMN montoOriginalCentavos INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE Transaccion ADD COLUMN tasaGtqPorUsd TEXT NOT NULL DEFAULT '1'")
        db.execSQL("ALTER TABLE Transaccion ADD COLUMN fechaTasaEpochDay INTEGER")
        db.execSQL("ALTER TABLE Transaccion ADD COLUMN fuenteTasa TEXT")
        db.execSQL("UPDATE Transaccion SET montoOriginalCentavos = montoCentavos")
    }
}
