package gt.edu.umg.finanzas.app.datos.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.core.dominio.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migracion2a3Test {
    @get:Rule val ayuda = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Before fun protegerInstalacionNormal() { check(BaseDePrueba.contexto.packageName.endsWith(".pruebas")) }

    private fun sembrar(base: SupportSQLiteDatabase, saldo: Long) {
        base.execSQL("INSERT INTO Usuario VALUES (1, 'Maria', $saldo, 1)")
        CategoriasIniciales.lista.forEachIndexed { indice, categoria ->
            base.execSQL("INSERT INTO Categoria VALUES (?,?,?,?,?)",
                arrayOf<Any>(indice + 1, categoria.clave, categoria.nombre, categoria.tipo.name, if (indice == 0) 0 else 1))
        }
    }

    private fun fotografiaV2(base: SupportSQLiteDatabase): List<List<String?>> =
        base.query("SELECT idTransaccion,idUsuario,idCategoria,tipo,montoCentavos,descripcion,fechaEpochDay," +
            "monedaOriginal,montoOriginalCentavos,tasaGtqPorUsd,fechaTasaEpochDay,fuenteTasa FROM Transaccion ORDER BY idTransaccion").use { cursor ->
            buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
        }

    @Test fun conserva_v2_con_usd_antiguos_gtq_saldo_nombre_y_categorias(): Unit = runBlocking {
        val nombre = "migracion-recargo-v2-prueba.db"
        val antes = ayuda.createDatabase(nombre, 2).use { base ->
            sembrar(base, 14642)
            base.execSQL("INSERT INTO Transaccion VALUES (11,1,9,'INGRESO',10000,'GTQ anterior',20748,'GTQ',10000,'1',NULL,NULL)")
            base.execSQL("INSERT INTO Transaccion VALUES (12,1,1,'GASTO',7642,'USD anterior',20748,'USD',1000,'7.64187',20748,'Banguat anterior')")
            base.execSQL("INSERT INTO Transaccion VALUES (13,1,9,'INGRESO',15284,'USD ingreso anterior',20748,'USD',2000,'7.64187',20748,'Banguat anterior')")
            base.execSQL("INSERT INTO Transaccion VALUES (14,1,2,'GASTO',3000,'Otro GTQ',20748,'GTQ',3000,'1',NULL,NULL)")
            fotografiaV2(base)
        }
        ayuda.runMigrationsAndValidate(nombre, 3, true, Migracion2a3).use { base ->
            assertEquals(antes, fotografiaV2(base))
            base.query("SELECT recargoGtqPorUsd,tasaAplicadaGtqPorUsd,tasaGtqPorUsd FROM Transaccion").use {
                var filas = 0
                while (it.moveToNext()) { filas++; assertEquals("0", it.getString(0)); assertEquals(it.getString(2), it.getString(1)) }
                assertEquals(4, filas)
            }
            base.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            base.query("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('Usuario','Categoria','Transaccion')").use {
                assertTrue(it.moveToFirst()); assertEquals(3, it.getInt(0))
            }
        }
        var base = AppDatabase.crear(BaseDePrueba.contexto, nombre)
        try {
            val usuario = base.usuarioDao().obtenerPorId(1)!!
            assertEquals("Maria", usuario.nombrePerfil)
            assertEquals(14642L, usuario.saldoCentavos)
            assertTrue(usuario.reconstruccionPendiente)
            assertEquals(12, base.categoriaDao().obtenerTodas().size)
            assertFalse(base.categoriaDao().obtenerPorId(1)!!.activa)
            InicializadorDatos(base).inicializar().esperarOk()
            assertEquals(usuario, base.usuarioDao().obtenerPorId(1))
            val antiguo = base.transaccionDao().obtenerPorId(12)!!.aDominio()
            assertEquals(0, antiguo.conversion!!.recargoGtqPorUsd.signum())
            val datos = DatosMovimiento(TipoMovimiento.GASTO, "12", Moneda.USD, null, 1, "Editado sin recargo", BaseDePrueba.hoy)
            BaseDePrueba.servicioSobre(base).editar(12, datos, BaseDePrueba.hoy).esperarOk()
            assertEquals(9170L, base.transaccionDao().obtenerPorId(12)!!.montoCentavos)
            assertEquals(13114L, BaseDePrueba.saldo(base))
            base.close()
            base = AppDatabase.crear(BaseDePrueba.contexto, nombre)
            val releido = base.transaccionDao().obtenerPorId(12)!!
            assertEquals("0", releido.recargoGtqPorUsd.toPlainString())
            assertEquals("7.64187", releido.tasaAplicadaGtqPorUsd.toPlainString())
            BaseDePrueba.servicioSobre(base).editar(12, datos, BaseDePrueba.hoy).esperarOk()
            assertEquals(releido, base.transaccionDao().obtenerPorId(12))
            assertEquals(13114L, BaseDePrueba.saldo(base))
        } finally { base.close(); BaseDePrueba.contexto.deleteDatabase(nombre) }
    }

    @Test fun cadena_v1_a_v3_conserva_los_importes_y_saldo(): Unit = runBlocking {
        val nombre = "migracion-recargo-v1-prueba.db"
        ayuda.createDatabase(nombre, 1).use { base ->
            sembrar(base, 7000)
            base.execSQL("INSERT INTO Transaccion VALUES (11,1,9,'INGRESO',10000,'Pago',20748)")
            base.execSQL("INSERT INTO Transaccion VALUES (12,1,1,'GASTO',3000,'Comida',20748)")
        }
        ayuda.runMigrationsAndValidate(nombre, 3, true, Migracion1a2, Migracion2a3).close()
        val base = AppDatabase.crear(BaseDePrueba.contexto, nombre)
        try {
            assertEquals(7000L, BaseDePrueba.saldo(base))
            assertEquals(2, base.transaccionDao().contar())
            base.transaccionDao().obtenerTodas().forEach {
                assertEquals(it.montoCentavos, it.montoOriginalCentavos)
                assertEquals("1", it.tasaAplicadaGtqPorUsd.toPlainString())
                assertEquals(0, it.recargoGtqPorUsd.signum())
                assertNull(it.aDominio().conversion)
            }
        } finally { base.close(); BaseDePrueba.contexto.deleteDatabase(nombre) }
    }
}
