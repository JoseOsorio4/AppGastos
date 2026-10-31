package gt.edu.umg.finanzas.app.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.local.entidades.TransaccionEntity
import gt.edu.umg.finanzas.app.datos.local.entidades.UsuarioEntity
import gt.edu.umg.finanzas.app.datos.preferencias.BloqueoLocal
import gt.edu.umg.finanzas.app.datos.preferencias.Tema
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarAccesoListo
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.core.dominio.*
import gt.edu.umg.finanzas.core.resultado.Resultado
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonalizacionActualizacionTest {
    private lateinit var temaAnterior: Tema
    private var reflexionAnterior = true
    private data class DatosAnteriores(val usuario: UsuarioEntity, val movimientos: List<TransaccionEntity>)

    @Before fun preparar() {
        check(app.packageName.endsWith(".pruebas"))
        runBlocking { app.inicializacion.await() }
        temaAnterior = app.dependencias.preferenciaTema.tema.value
        reflexionAnterior = app.dependencias.cacheServicios.reflexionVisible()
        AyudaInterfaz.baseLimpia()
    }
    @After fun restaurarPreferenciasDePrueba() {
        check(app.packageName.endsWith(".pruebas"))
        app.dependencias.preferenciaTema.guardar(temaAnterior)
        app.dependencias.cacheServicios.guardarReflexionVisible(reflexionAnterior)
        File(app.noBackupFilesDir, "acceso.json").delete()
        app.dependencias.bloqueo.preparar("Perfil de prueba")
        app.dependencias.bloqueo.bloquear()
    }
    private fun prepararActualizacion(nombre: String): DatosAnteriores = runBlocking {
        val datos = app.dependencias.baseDatos
        datos.usuarioDao().cambiarNombre(nombre)
        val categoria = datos.categoriaDao().obtenerTodas().first { it.clave == "SALARIO" }
        assertTrue(app.dependencias.servicio.registrar(DatosMovimiento(TipoMovimiento.INGRESO,
            "123.45", Moneda.GTQ, null, categoria.idCategoria, "Movimiento anterior", Periodo.hoy()),
            Periodo.hoy()) is Resultado.Ok)
        app.dependencias.preferenciaTema.guardar(Tema.OSCURO)
        app.dependencias.cacheServicios.guardarReflexionVisible(false)
        val anteriores = DatosAnteriores(datos.usuarioDao().obtenerPorId(1)!!, datos.transaccionDao().obtenerTodas())
        val archivo = File(app.noBackupFilesDir, "acceso.json")
        check(archivo.delete() || !archivo.exists())
        app.dependencias.bloqueo.preparar(datos.usuarioDao().obtenerPorId(1)?.nombrePerfil)
        app.dependencias.inicializador.inicializar()
        anteriores
    }
    private fun comprobarDatos(anteriores: DatosAnteriores, nombre: String) = runBlocking {
        val datos = app.dependencias.baseDatos
        assertEquals(anteriores.usuario.copy(nombrePerfil = nombre), datos.usuarioDao().obtenerPorId(1))
        assertEquals(anteriores.movimientos, datos.transaccionDao().obtenerTodas())
        assertEquals(Tema.OSCURO, app.dependencias.preferenciaTema.tema.value)
        assertEquals("OSCURO", app.getSharedPreferences("apariencia", 0).getString("tema", null))
        assertFalse(app.dependencias.cacheServicios.reflexionVisible())
        assertFalse(app.dependencias.bloqueo.tienePin())
    }
    private fun esperarInicio() {
        esperarAccesoListo()
        reintentar { onView(withId(R.id.textoSaldoDisponible)).check(matches(withText("Q 123.45"))) }
    }

    @Test fun principal_permite_saltar_sin_pin_y_no_repite_la_pregunta() {
        val datos = prepararActualizacion("Principal")
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            reintentar { onView(withId(R.id.entradaNombre)).check(matches(isDisplayed())) }
            esperarAccesoListo()
            onView(withId(R.id.botonContinuar)).check(matches(withText("Saltar y entrar sin PIN"))).perform(scrollTo(), click())
            esperarInicio()
            comprobarDatos(datos, "Principal")
            val reabierto = BloqueoLocal(app.noBackupFilesDir)
            reabierto.preparar("Principal")
            assertFalse(reabierto.necesitaBienvenida())
            escenario.recreate()
            esperarInicio()
            onView(withId(R.id.entradaNombre)).check(doesNotExist())
        }
    }
    @Test fun principal_permite_personalizar_y_ver_bienvenida_sin_exigir_pin() {
        val datos = prepararActualizacion("Principal")
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            reintentar { onView(withId(R.id.entradaNombre)).check(matches(isDisplayed())) }
            esperarAccesoListo()
            onView(withId(R.id.entradaNombre)).perform(replaceText("Ana"), closeSoftKeyboard())
            onView(withId(R.id.botonAcceso)).perform(scrollTo(), click())
            esperarAccesoListo()
            onView(withId(R.id.tituloAcceso)).check(matches(withText("¡Bienvenido, Ana!")))
            onView(withId(R.id.botonContinuar)).check(matches(withText("Continuar sin PIN"))).perform(scrollTo(), click())
            esperarInicio()
            comprobarDatos(datos, "Ana")
            escenario.recreate()
            esperarInicio()
            onView(withId(R.id.entradaNombre)).check(doesNotExist())
        }
    }
    @Test fun nombre_personalizado_se_conserva_y_entra_directamente() {
        val datos = prepararActualizacion("Nombre anterior")
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            esperarInicio()
            onView(withId(R.id.entradaNombre)).check(doesNotExist())
            comprobarDatos(datos, "Nombre anterior")
            escenario.recreate()
            esperarInicio()
            comprobarDatos(datos, "Nombre anterior")
        }
    }

}
