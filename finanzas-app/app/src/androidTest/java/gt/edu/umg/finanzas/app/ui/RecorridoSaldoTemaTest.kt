package gt.edu.umg.finanzas.app.ui

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.preferencias.PreferenciaTema
import gt.edu.umg.finanzas.app.datos.preferencias.Tema
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarTexto
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorridoSaldoTemaTest {
    private val app get() = AyudaInterfaz.app
    private val base get() = app.dependencias.baseDatos

    @Before
    fun preparar() {
        AyudaInterfaz.baseLimpia()
        restaurarTema()
    }

    @After
    fun limpiar() {
        base.openHelper.writableDatabase.execSQL("DROP TRIGGER IF EXISTS impedir_recalculo")
        restaurarTema()
    }

    private fun restaurarTema() {
        app.dependencias.preferenciaTema.guardar(Tema.SISTEMA)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(Tema.SISTEMA.modoNocturno)
        }
    }

    private fun prepararSaldoPendiente() = runBlocking {
        val hoy = Periodo.hoy()
        val categorias = base.categoriaDao().obtenerTodas()
        for ((tipo, monto, clave) in listOf(
            Triple(TipoMovimiento.INGRESO, "100", "SALARIO"),
            Triple(TipoMovimiento.GASTO, "30", "ALIMENTACION"),
        )) {
            val categoria = categorias.first { it.clave == clave }
            assertTrue(app.dependencias.servicio.registrar(
                DatosMovimiento(tipo, monto, Moneda.GTQ, null, categoria.idCategoria, clave, hoy), hoy,
            ) is Resultado.Ok)
        }
        base.usuarioDao().actualizarSaldo(1, 123)
        base.usuarioDao().marcarReconstruccion(1, true)
    }

    @Test
    fun aviso_sobrevive_recreacion_y_recalcula_desde_el_historial() {
        prepararSaldoPendiente()
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            esperarTexto(R.id.textoSaldoDisponible, "Q 1.23")
            reintentar { onView(withId(R.id.avisoRecalculo)).check(matches(isDisplayed())) }
            escenario.recreate()
            reintentar { onView(withId(R.id.avisoRecalculo)).check(matches(isDisplayed())) }
            onView(withId(R.id.botonRecalcularSaldo)).perform(click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
            reintentar { onView(withId(R.id.avisoRecalculo)).check(matches(not(isDisplayed()))) }
            escenario.recreate()
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
            onView(withId(R.id.avisoRecalculo)).check(matches(not(isDisplayed())))
        }
        runBlocking {
            assertFalse(checkNotNull(base.usuarioDao().obtenerPorId(1)).reconstruccionPendiente)
            assertEquals(2, base.transaccionDao().contar())
            assertEquals(Resultado.Ok(7_000L), app.dependencias.servicio.reconstruirSaldo())
        }
    }

    @Test
    fun fallo_real_conserva_aviso_saldo_y_bandera_y_permite_reintentar() {
        prepararSaldoPendiente()
        // Falla después de asignar el saldo: SQLite debe revertir también esa primera escritura.
        base.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER impedir_recalculo BEFORE UPDATE OF reconstruccionPendiente ON Usuario " +
                "WHEN NEW.reconstruccionPendiente = 0 BEGIN SELECT RAISE(ABORT, 'Falla de prueba'); END",
        )
        ActivityScenario.launch(InicioActivity::class.java).use {
            reintentar { onView(withId(R.id.botonRecalcularSaldo)).check(matches(isDisplayed())) }
            onView(withId(R.id.botonRecalcularSaldo)).perform(click())
            reintentar { onView(withId(R.id.textoErrorRecalculo)).check(matches(isDisplayed())) }
            runBlocking {
                val usuario = checkNotNull(base.usuarioDao().obtenerPorId(1))
                assertEquals(123L, usuario.saldoCentavos)
                assertTrue(usuario.reconstruccionPendiente)
            }
            base.openHelper.writableDatabase.execSQL("DROP TRIGGER impedir_recalculo")
            onView(withId(R.id.botonRecalcularSaldo)).perform(click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
            reintentar { onView(withId(R.id.avisoRecalculo)).check(matches(not(isDisplayed()))) }
        }
    }

    @Test
    fun saldo_cero_sin_bandera_no_ofrece_recalculo() {
        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.textoSaldoDisponible, "Q 0.00")
            onView(withId(R.id.avisoRecalculo)).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun selector_guarda_las_tres_opciones_y_aplica_claro_y_oscuro_a_las_pantallas() {
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            esperarTexto(R.id.textoSaldoDisponible, "Q 0.00")
            for ((tema, texto) in listOf(Tema.OSCURO to "Oscuro", Tema.CLARO to "Claro", Tema.SISTEMA to "Sistema")) {
                onView(withId(R.id.opcionTema)).perform(click())
                onView(withText(texto)).perform(click())
                reintentar {
                    assertEquals(tema, PreferenciaTema(app).tema.value)
                    assertEquals(tema.modoNocturno, AppCompatDelegate.getDefaultNightMode())
                }
                escenario.recreate()
                esperarTexto(R.id.textoSaldoDisponible, "Q 0.00")
                if (tema != Tema.SISTEMA) {
                    val modo = if (tema == Tema.OSCURO) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                    escenario.onActivity { assertEquals(modo, it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) }
                    onView(withId(R.id.botonAgregarMovimiento)).perform(click())
                    reintentar { onView(withId(R.id.entradaMonto)).check(matches(isDisplayed())) }
                    onView(withId(R.id.entradaMonto)).check { vista, error ->
                        if (error != null) throw error
                        assertEquals(modo, vista.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                    }
                    pressBack()
                }
                onView(withId(R.id.opcionTema)).perform(click())
                onView(withText(texto)).check(matches(isChecked()))
                onView(withText("Cancelar")).perform(click())
            }
        }
    }
}
