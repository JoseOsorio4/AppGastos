package gt.edu.umg.finanzas.app.datos.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.gasto
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.hoy
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.ingreso
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.saldo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Caso d de §14.1: los datos sobreviven a cerrar y volver a abrir el archivo SQLite. */
@RunWith(AndroidJUnit4::class)
class PersistenciaTest {

    // Archivo propio: no toca finanzas.db, la base real de la app.
    private val archivo = "prueba-persistencia.db"

    @Before
    @After
    fun borrarArchivo() {
        BaseDePrueba.contexto.deleteDatabase(archivo)
    }

    @Test
    fun caso_d_movimientos_y_saldo_se_conservan_al_reabrir() = runBlocking {
        val primera = AppDatabase.crear(BaseDePrueba.contexto, archivo)
        InicializadorDatos(primera).inicializar().esperarOk()
        val operaciones = OperacionesMovimientoRoom(primera)
        operaciones.crear(ingreso(primera, "100"), hoy).esperarOk()
        operaciones.crear(gasto(primera, "30"), hoy).esperarOk()
        primera.close()

        val segunda = AppDatabase.crear(BaseDePrueba.contexto, archivo)
        // Abrir de nuevo vuelve a sembrar, como en cada arranque de la app: no debe duplicar nada.
        InicializadorDatos(segunda).inicializar().esperarOk()

        assertEquals(2, segunda.transaccionDao().contar())
        assertEquals(7_000L, saldo(segunda))
        assertEquals(1, segunda.usuarioDao().contar())
        assertEquals(12, segunda.categoriaDao().obtenerTodas().size)
        segunda.close()
    }
}
