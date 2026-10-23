package gt.edu.umg.finanzas.app.datos.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.core.dominio.CategoriasIniciales
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InicializadorDatosTest {

    private lateinit var baseDatos: AppDatabase

    @Before
    fun abrir() {
        baseDatos = BaseDePrueba.enMemoria()
    }

    @After
    fun cerrar() = baseDatos.close()

    @Test
    fun crea_un_usuario_y_12_categorias_aunque_se_repita() = runBlocking {
        val inicializador = InicializadorDatos(baseDatos)
        inicializador.inicializar().esperarOk()
        inicializador.inicializar().esperarOk()

        assertEquals(1, baseDatos.usuarioDao().contar())
        val categorias = baseDatos.categoriaDao().obtenerTodas()
        assertEquals(12, categorias.size)
        assertEquals(CategoriasIniciales.lista.map { it.clave }, categorias.map { it.clave })
        assertEquals((1L..12L).toList(), categorias.map { it.idCategoria })
    }

    @Test
    fun repetir_la_siembra_no_reinicia_el_saldo() = runBlocking {
        val inicializador = InicializadorDatos(baseDatos)
        inicializador.inicializar().esperarOk()
        OperacionesMovimientoRoom(baseDatos).crear(BaseDePrueba.ingreso(baseDatos, "100"), BaseDePrueba.hoy).esperarOk()

        inicializador.inicializar().esperarOk()

        assertEquals(10_000L, BaseDePrueba.saldo(baseDatos))
    }
}
