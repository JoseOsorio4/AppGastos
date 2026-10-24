package gt.edu.umg.finanzas.app.datos.local

import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.gasto
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.hoy
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.ingreso
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.saldo
import gt.edu.umg.finanzas.app.datos.local.dao.UsuarioDao
import gt.edu.umg.finanzas.core.dominio.Usuario
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Reconstrucción del saldo (§9.6): saldo erróneo, repetición sin cambios y rollback. */
@RunWith(AndroidJUnit4::class)
class ReconstructorSaldoRoomTest {

    private lateinit var baseDatos: AppDatabase

    // JUnit exige que @Before no devuelva nada: el bloque termina en Unit, no en el ID creado.
    @Before
    fun abrir(): Unit = runBlocking {
        baseDatos = BaseDePrueba.enMemoria()
        InicializadorDatos(baseDatos).inicializar().esperarOk()
        val operaciones = OperacionesMovimientoRoom(baseDatos)
        operaciones.crear(ingreso(baseDatos, "100"), hoy).esperarOk()
        operaciones.crear(gasto(baseDatos, "30"), hoy).esperarOk()
    }

    @After
    fun cerrar() = baseDatos.close()

    // Simula un saldo dañado (por ejemplo, tras una migración) y pide recalcularlo.
    private suspend fun dañarSaldo(saldoErroneo: Long, pendiente: Boolean) {
        baseDatos.usuarioDao().actualizarSaldo(Usuario.ID_UNICO, saldoErroneo)
        baseDatos.usuarioDao().marcarReconstruccion(Usuario.ID_UNICO, pendiente)
    }

    private suspend fun banderaPendiente(): Boolean =
        checkNotNull(baseDatos.usuarioDao().obtenerPorId(Usuario.ID_UNICO)).reconstruccionPendiente

    @Test
    fun con_bandera_corrige_el_saldo_y_la_apaga() = runBlocking {
        dañarSaldo(saldoErroneo = 123, pendiente = true)

        assertEquals(7_000L, ReconstructorSaldoRoom(baseDatos).reconstruirSiPendiente().esperarOk())

        assertEquals(7_000L, saldo(baseDatos))
        assertFalse(banderaPendiente())
    }

    @Test
    fun la_segunda_llamada_no_cambia_nada() = runBlocking {
        dañarSaldo(saldoErroneo = 123, pendiente = true)
        val reconstructor = ReconstructorSaldoRoom(baseDatos)
        reconstructor.reconstruirSiPendiente().esperarOk()

        assertEquals(7_000L, reconstructor.reconstruirSiPendiente().esperarOk())
        assertEquals(7_000L, saldo(baseDatos))
        assertFalse(banderaPendiente())
    }

    @Test
    fun sin_bandera_no_toca_el_saldo_aunque_sea_cero() = runBlocking {
        dañarSaldo(saldoErroneo = 0, pendiente = false)

        assertEquals(0L, ReconstructorSaldoRoom(baseDatos).reconstruirSiPendiente().esperarOk())
        assertEquals(0L, saldo(baseDatos))
    }

    /** Falla después de escribir el saldo nuevo, al apagar la bandera. */
    private class UsuarioDaoQueFallaAlApagar(real: UsuarioDao) : UsuarioDao by real {
        override suspend fun marcarReconstruccion(idUsuario: Long, pendiente: Boolean): Int =
            throw SQLiteException("Falla provocada al apagar la bandera")
    }

    @Test
    fun un_fallo_revierte_saldo_y_bandera() = runBlocking {
        dañarSaldo(saldoErroneo = 123, pendiente = true)
        val conFallo = ReconstructorSaldoRoom(baseDatos, usuarios = UsuarioDaoQueFallaAlApagar(baseDatos.usuarioDao()))

        val resultado = conFallo.reconstruirSiPendiente()

        assertTrue("Llegó $resultado", resultado is Resultado.ErrorAlmacenamiento)
        assertEquals(123L, saldo(baseDatos))
        assertTrue(banderaPendiente())
        // Después del fallo, el reconstructor normal todavía puede corregirlo.
        assertEquals(7_000L, ReconstructorSaldoRoom(baseDatos).reconstruirSiPendiente().esperarOk())
    }
}
