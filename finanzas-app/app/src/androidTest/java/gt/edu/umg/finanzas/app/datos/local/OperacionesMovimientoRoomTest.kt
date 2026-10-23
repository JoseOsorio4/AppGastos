package gt.edu.umg.finanzas.app.datos.local

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.gasto
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.hoy
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.ingreso
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.saldo
import gt.edu.umg.finanzas.app.datos.local.dao.UsuarioDao
import gt.edu.umg.finanzas.app.datos.local.entidades.TransaccionEntity
import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Usuario
import gt.edu.umg.finanzas.core.reglas.CalculoSaldo
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Escrituras atómicas de OperacionesMovimientoRoom: casos c y j de §14.1 y las claves foráneas. */
@RunWith(AndroidJUnit4::class)
class OperacionesMovimientoRoomTest {

    private lateinit var baseDatos: AppDatabase
    private lateinit var operaciones: OperacionesMovimientoRoom

    @Before
    fun abrir() = runBlocking {
        baseDatos = BaseDePrueba.enMemoria()
        InicializadorDatos(baseDatos).inicializar().esperarOk()
        operaciones = OperacionesMovimientoRoom(baseDatos)
    }

    @After
    fun cerrar() = baseDatos.close()

    /** DAO de prueba: falla justo después del INSERT, UPDATE o DELETE del movimiento. */
    private class UsuarioDaoQueFalla(real: UsuarioDao) : UsuarioDao by real {
        override suspend fun actualizarSaldo(idUsuario: Long, saldoCentavos: Long): Int =
            throw SQLiteException("Falla provocada al guardar el saldo")
    }

    private fun operacionesQueFallan() =
        OperacionesMovimientoRoom(baseDatos, usuarios = UsuarioDaoQueFalla(baseDatos.usuarioDao()))

    @Test
    fun caso_c_falla_al_guardar_el_saldo_revierte_la_insercion() = runBlocking {
        operaciones.crear(ingreso(baseDatos, "100"), hoy).esperarOk()

        val resultado = operacionesQueFallan().crear(gasto(baseDatos, "30"), hoy)

        assertTrue("Llegó $resultado", resultado is Resultado.ErrorAlmacenamiento)
        assertEquals(1, baseDatos.transaccionDao().contar())
        assertEquals(10_000L, saldo(baseDatos))
    }

    @Test
    fun caso_c_falla_al_guardar_el_saldo_revierte_edicion_y_eliminacion() = runBlocking {
        val idGasto = operaciones.crear(gasto(baseDatos, "30"), hoy).esperarOk()

        val edicion = operacionesQueFallan().editar(idGasto, gasto(baseDatos, "20"), hoy)
        val eliminacion = operacionesQueFallan().eliminar(idGasto)

        assertTrue(edicion is Resultado.ErrorAlmacenamiento)
        assertTrue(eliminacion is Resultado.ErrorAlmacenamiento)
        assertEquals(3_000L, checkNotNull(baseDatos.transaccionDao().obtenerPorId(idGasto)).montoCentavos)
        assertEquals(-3_000L, saldo(baseDatos))
    }

    @Test
    fun caso_j_desborde_del_saldo_no_escribe_nada() = runBlocking {
        baseDatos.usuarioDao().actualizarSaldo(Usuario.ID_UNICO, Long.MAX_VALUE - 50)

        val resultado = operaciones.crear(ingreso(baseDatos, "1.00"), hoy)

        assertEquals(CalculoSaldo.MENSAJE_DESBORDE, (resultado as Resultado.ErrorNegocio).mensaje)
        assertEquals(0, baseDatos.transaccionDao().contar())
        assertEquals(Long.MAX_VALUE - 50, saldo(baseDatos))
    }

    @Test
    fun dato_invalido_o_categoria_de_otro_tipo_no_escribe() = runBlocking {
        val montoInvalido = operaciones.crear(gasto(baseDatos, "12,50"), hoy) as Resultado.ErrorNegocio
        val otroTipo = operaciones.crear(
            BaseDePrueba.datos(baseDatos, TipoMovimiento.INGRESO, "10", "ALIMENTACION"),
            hoy,
        ) as Resultado.ErrorNegocio
        val inexistente = operaciones.crear(gasto(baseDatos, "10").copy(idCategoria = 999), hoy) as Resultado.ErrorNegocio

        assertEquals(CampoMovimiento.MONTO, montoInvalido.campo)
        assertEquals(CampoMovimiento.CATEGORIA, otroTipo.campo)
        assertEquals(CampoMovimiento.CATEGORIA, inexistente.campo)
        assertEquals(0, baseDatos.transaccionDao().contar())
        assertEquals(0L, saldo(baseDatos))
    }

    @Test
    fun editar_o_eliminar_un_id_inexistente_da_NoEncontrado_sin_insertar() = runBlocking {
        assertEquals(Resultado.NoEncontrado, operaciones.editar(99, gasto(baseDatos, "10"), hoy))
        assertEquals(Resultado.NoEncontrado, operaciones.eliminar(99))
        assertEquals(0, baseDatos.transaccionDao().contar())
    }

    @Test
    fun las_claves_foraneas_rechazan_categoria_inexistente_y_borrado_en_uso() = runBlocking {
        try {
            baseDatos.transaccionDao().insertar(
                TransaccionEntity(
                    idUsuario = Usuario.ID_UNICO, idCategoria = 999, tipo = TipoMovimiento.GASTO,
                    montoCentavos = 100, descripcion = "Sin categoría", fechaEpochDay = hoy.toEpochDay(),
                ),
            )
            fail("La FK debió rechazar idCategoria = 999")
        } catch (esperada: SQLiteConstraintException) {
        }

        val idAlimentacion = BaseDePrueba.idCategoria(baseDatos, "ALIMENTACION")
        operaciones.crear(gasto(baseDatos, "10"), hoy).esperarOk()
        try {
            baseDatos.openHelper.writableDatabase.execSQL("DELETE FROM Categoria WHERE idCategoria = $idAlimentacion")
            fail("RESTRICT debió impedir borrar una categoría con movimientos")
        } catch (esperada: SQLiteConstraintException) {
        }
        assertEquals(12, baseDatos.categoriaDao().obtenerTodas().size)
    }
}
