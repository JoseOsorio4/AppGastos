package gt.edu.umg.finanzas.app.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.preferencias.BloqueoLocal
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.acceso.AccesoActivity
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.app.R
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AccesoTest {
    private fun esperarAccesoListo() {
        val instrumento = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val limite = System.currentTimeMillis() + 15_000
        while (true) {
            var ocupado = false
            instrumento.runOnMainSync {
                val actividades = androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                ocupado = actividades.filterIsInstance<AccesoActivity>().any {
                    val estado = androidx.lifecycle.ViewModelProvider(it)[gt.edu.umg.finanzas.app.ui.acceso.AccesoViewModel::class.java].estado.value
                    estado.ocupado || estado.paso == gt.edu.umg.finanzas.app.ui.acceso.PasoAcceso.CARGANDO
                }
            }
            if (!ocupado) return
            check(System.currentTimeMillis() < limite) { "El acceso no terminó de responder" }
            Thread.sleep(50)
        }
    }

    @org.junit.Before fun prepararAccesoAislado() {
        check(app.packageName.endsWith(".pruebas"))
        runBlocking { app.inicializacion.await() }
        File(app.noBackupFilesDir, "acceso.json").delete()
        app.dependencias.bloqueo.preparar(true)
        app.dependencias.bloqueo.bloquear()
    }
    @org.junit.After fun restaurarAccesoDePrueba() {
        File(app.noBackupFilesDir, "acceso.json").delete()
        app.dependencias.bloqueo.preparar(true)
        app.dependencias.bloqueo.bloquear()
    }
    @Test fun usuario_y_saldo_existentes_no_se_reinician_al_preparar_acceso() = runBlocking {
        AyudaInterfaz.baseLimpia()
        val dao = app.dependencias.baseDatos.usuarioDao()
        dao.cambiarNombre("Perfil existente")
        val categoria = app.dependencias.baseDatos.categoriaDao().obtenerTodas().first { it.clave == "SALARIO" }
        assertTrue(app.dependencias.servicio.registrar(gt.edu.umg.finanzas.core.dominio.DatosMovimiento(
            gt.edu.umg.finanzas.core.dominio.TipoMovimiento.INGRESO, "123.45", gt.edu.umg.finanzas.core.dominio.Moneda.GTQ,
            null, categoria.idCategoria, "Dato anterior", gt.edu.umg.finanzas.core.dominio.Periodo.hoy()),
            gt.edu.umg.finanzas.core.dominio.Periodo.hoy()) is gt.edu.umg.finanzas.core.resultado.Resultado.Ok)
        val movimientos = app.dependencias.baseDatos.transaccionDao().obtenerTodas()
        val anterior = dao.obtenerPorId(1)
        File(app.noBackupFilesDir, "acceso.json").delete()
        app.dependencias.bloqueo.preparar(dao.obtenerPorId(1) != null)
        app.dependencias.inicializador.inicializar()
        assertEquals(anterior, dao.obtenerPorId(1))
        assertEquals(movimientos, app.dependencias.baseDatos.transaccionDao().obtenerTodas())
        assertFalse(app.dependencias.bloqueo.necesitaBienvenida())
        assertFalse(app.dependencias.bloqueo.bloqueado())
    }

    private fun almacen(): BloqueoLocal {
        check(app.packageName.endsWith(".pruebas"))
        return BloqueoLocal(File(app.noBackupFilesDir, UUID.randomUUID().toString()).apply { mkdirs() })
    }
    @Test fun nueva_y_actualizacion_se_distinguen() {
        val nueva = almacen(); nueva.preparar(false); assertTrue(nueva.necesitaBienvenida())
        nueva.completarBienvenida(); assertFalse(nueva.necesitaBienvenida())
        val anterior = almacen(); anterior.preparar(true); assertFalse(anterior.necesitaBienvenida()); assertFalse(anterior.bloqueado())
    }
    @Test fun pin_confirmacion_cambio_y_desactivacion_exigen_actual() {
        val b = almacen(); b.preparar(true)
        assertThrows(IllegalArgumentException::class.java) { b.cambiar("", "123456", "654321") }
        assertFalse(b.tienePin()); b.cambiar("", "123456", "123456"); b.bloquear()
        assertFalse(b.abrir("000000", b.revision())); assertTrue(b.bloqueado())
        assertTrue(b.abrir("123456", b.revision()))
        assertThrows(IllegalArgumentException::class.java) { b.desactivar("000000") }
        b.cambiar("123456", "654321", "654321"); b.bloquear()
        assertFalse(b.abrir("123456", b.revision())); assertTrue(b.abrir("654321", b.revision()))
        b.desactivar("654321"); assertFalse(b.tienePin())
    }
    @Test fun otro_proceso_y_sesion_interrumpida_no_reutilizan_autorizacion() {
        val carpeta = File(app.noBackupFilesDir, UUID.randomUUID().toString()).apply { mkdirs() }
        val b = BloqueoLocal(carpeta); b.preparar(true); b.cambiar("", "123456", "123456")
        val revision = b.revision(); b.bloquear(); assertFalse(b.abrir("123456", revision))
        val reabierto = BloqueoLocal(carpeta); assertTrue(reabierto.bloqueado())
        assertTrue(reabierto.abrir("123456", reabierto.revision()))
        assertFalse(File(carpeta, "acceso.json").readText().contains("123456"))
    }
    @Test fun cinco_errores_imponen_espera_persistida() {
        val b = almacen(); b.preparar(true); b.cambiar("", "123456", "123456"); b.bloquear()
        repeat(5) { assertFalse(b.abrir("000000", b.revision())) }
        assertThrows(IllegalStateException::class.java) { b.abrir("123456", b.revision()) }
    }
    @Test fun rotar_el_acceso_no_autoriza_la_sesion() {
        AyudaInterfaz.baseLimpia()
        val b = app.dependencias.bloqueo
        b.cambiar("", "123456", "123456"); b.bloquear()
        try {
            ActivityScenario.launch(AccesoActivity::class.java).use { escenario ->
                reintentar { onView(withId(R.id.entradaPinActual)).check(matches(isDisplayed())) }
                onView(withId(R.id.entradaPinActual)).perform(replaceText("123"), closeSoftKeyboard())
                escenario.recreate()
                reintentar { onView(withId(R.id.entradaPinActual)).check(matches(withText(""))) }
                assertTrue(b.bloqueado())
            }
        } finally { b.abrir("123456", b.revision()); b.desactivar("123456") }
    }

    @Test fun nombre_guardado_no_vuelve_a_pedirse_antes_de_continuar() {
        AyudaInterfaz.baseLimpia()
        File(app.noBackupFilesDir, "acceso.json").delete()
        app.dependencias.bloqueo.preparar(false)
        runBlocking { app.dependencias.baseDatos.usuarioDao().cambiarNombre("Ana") }
        ActivityScenario.launch(AccesoActivity::class.java).use {
            esperarAccesoListo()
            onView(withId(R.id.tituloAcceso)).check(matches(withText("¡Bienvenido, Ana!")))
            onView(withId(R.id.entradaNombre)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test fun bienvenida_nombre_y_pin_opcional_en_pantalla() {
        AyudaInterfaz.baseLimpia()
        val archivo = File(app.noBackupFilesDir, "acceso.json")
        check(archivo.delete() || !archivo.exists())
        app.dependencias.bloqueo.preparar(false)
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            reintentar { onView(withId(R.id.entradaNombre)).check(matches(isDisplayed())) }
            onView(withId(R.id.entradaNombre)).perform(replaceText("Ana"), closeSoftKeyboard())
            reintentar { onView(withId(R.id.botonAcceso)).check(matches(isEnabled())).perform(scrollTo(), click()) }; esperarAccesoListo()
            reintentar { onView(withId(R.id.tituloAcceso)).check(matches(withText("¡Bienvenido, Ana!"))) }
            onView(withId(R.id.botonContinuar)).perform(scrollTo(), click())
            reintentar { onView(withId(R.id.textoSaldoDisponible)).check(matches(isDisplayed())) }
            escenario.recreate()
            reintentar { onView(withId(R.id.textoSaldoDisponible)).check(matches(isDisplayed())) }
            assertEquals("Ana", runBlocking { app.dependencias.baseDatos.usuarioDao().obtenerPorId(1)!!.nombrePerfil })
        }
    }
    @Test fun configurar_bloquear_rotar_ingresar_cambiar_y_desactivar_desde_ui() {
        AyudaInterfaz.baseLimpia()
        val b = app.dependencias.bloqueo
        b.completarBienvenida()
        ActivityScenario.launch(InicioActivity::class.java).use {
            onView(withId(R.id.botonSeguridad)).perform(scrollTo(), click())
            reintentar { onView(withId(R.id.entradaPinNuevo)).check(matches(isDisplayed())) }
            onView(withId(R.id.entradaPinNuevo)).perform(replaceText("123456"))
            onView(withId(R.id.entradaPinConfirmacion)).perform(replaceText("123456"), closeSoftKeyboard())
            reintentar { onView(withId(R.id.botonAcceso)).check(matches(isEnabled())).perform(scrollTo(), click()) }; esperarAccesoListo()
            reintentar { onView(withId(R.id.botonBloquear)).perform(scrollTo(), click()) }
            reintentar { onView(withId(R.id.entradaPinActual)).check(matches(isDisplayed())) }
            onView(withId(R.id.entradaPinActual)).perform(replaceText("000000"), closeSoftKeyboard())
            reintentar { onView(withId(R.id.botonAcceso)).check(matches(isEnabled())).perform(scrollTo(), click()) }; esperarAccesoListo()
            onView(withId(R.id.errorAcceso)).check(matches(withText(containsString("PIN incorrecto"))))
            onView(withId(R.id.entradaPinActual)).perform(replaceText("123456"), closeSoftKeyboard())
            reintentar { onView(withId(R.id.botonAcceso)).check(matches(isEnabled())).perform(scrollTo(), click()) }; esperarAccesoListo()
            reintentar { onView(withId(R.id.botonSeguridad)).perform(scrollTo(), click()) }
            reintentar { onView(withId(R.id.entradaPinActual)).perform(replaceText("123456")) }
            onView(withId(R.id.entradaPinNuevo)).perform(replaceText("654321"))
            onView(withId(R.id.entradaPinConfirmacion)).perform(replaceText("654321"), closeSoftKeyboard())
            reintentar { onView(withId(R.id.botonAcceso)).check(matches(isEnabled())).perform(scrollTo(), click()) }; esperarAccesoListo()
            reintentar { onView(withId(R.id.botonSeguridad)).perform(scrollTo(), click()) }
            reintentar { onView(withId(R.id.entradaPinActual)).perform(replaceText("654321"), closeSoftKeyboard()) }
            onView(withId(R.id.botonDesactivarPin)).perform(scrollTo(), click())
            reintentar { assertFalse(b.tienePin()) }
        }
    }
}
