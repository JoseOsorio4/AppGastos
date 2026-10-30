package gt.edu.umg.finanzas.app.datos.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.core.dominio.CategoriasIniciales
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migracion1a2Test {
    @get:Rule
    val ayuda = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun conserva_las_tres_tablas_saldo_bandera_y_movimientos_de_v1(): Unit = runBlocking {
        val nombre = "migracion-inc6-prueba.db"
        ayuda.createDatabase(nombre, 1).use { anterior ->
            anterior.execSQL("INSERT INTO Usuario VALUES (1, 'Principal', 7000, 1)")
            CategoriasIniciales.lista.forEachIndexed { posicion, categoria ->
                anterior.execSQL(
                    "INSERT INTO Categoria (idCategoria,clave,nombre,tipo,activa) VALUES (?,?,?,?,?)",
                    arrayOf<Any>(posicion + 1, categoria.clave, categoria.nombre, categoria.tipo.name, if (posicion == 0) 0 else 1),
                )
            }
            anterior.execSQL("INSERT INTO Transaccion VALUES (11,1,9,'INGRESO',10000,'Pago',20748)")
            anterior.execSQL("INSERT INTO Transaccion VALUES (12,1,1,'GASTO',3000,'Almuerzo con café',20748)")
        }
        ayuda.runMigrationsAndValidate(nombre, 2, true, Migracion1a2).use { migrada ->
            migrada.query("SELECT montoCentavos,montoOriginalCentavos,monedaOriginal,tasaGtqPorUsd,fechaTasaEpochDay,fuenteTasa FROM Transaccion ORDER BY idTransaccion").use {
                for (monto in listOf(10_000L, 3_000L)) {
                    assertTrue(it.moveToNext())
                    assertEquals(monto, it.getLong(0))
                    assertEquals(monto, it.getLong(1))
                    assertEquals("GTQ", it.getString(2))
                    assertEquals("1", it.getString(3))
                    assertTrue(it.isNull(4) && it.isNull(5))
                }
                assertFalse(it.moveToNext())
            }
            migrada.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        }
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val base = AppDatabase.crear(contexto, nombre)
        try {
            assertEquals(7_000L, BaseDePrueba.saldo(base))
            assertTrue(checkNotNull(base.usuarioDao().obtenerPorId(1)).reconstruccionPendiente)
            assertEquals(12, base.categoriaDao().obtenerTodas().size)
            assertFalse(checkNotNull(base.categoriaDao().obtenerPorId(1)).activa)
            assertEquals("Almuerzo con café", checkNotNull(base.transaccionDao().obtenerPorId(12)).descripcion)
            val ingreso = checkNotNull(base.transaccionDao().obtenerPorId(11))
            assertEquals(9L, ingreso.idCategoria)
            assertEquals(20748L, ingreso.fechaEpochDay)
            assertEquals("INGRESO", ingreso.tipo.name)
            assertEquals(1L, ingreso.idUsuario)
            assertEquals("Principal", checkNotNull(base.usuarioDao().obtenerPorId(1)).nombrePerfil)
            assertNull(checkNotNull(base.transaccionDao().obtenerPorId(11)).aDominio().conversion)
            InicializadorDatos(base).inicializar()
            assertEquals(2, base.transaccionDao().contar())
            assertEquals(7_000L, BaseDePrueba.saldo(base))
        } finally { base.close() }
        contexto.deleteDatabase(nombre)
    }
}
