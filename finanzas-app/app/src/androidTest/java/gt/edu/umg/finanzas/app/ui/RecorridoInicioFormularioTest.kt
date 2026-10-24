package gt.edu.umg.finanzas.app.ui

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.ui.formulario.FormularioActivity
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorrido real de las pantallas sobre la base de la app: Inicio → Formulario → Room → Inicio.
 * Cada prueba vacía la base y vuelve a sembrar el usuario y las 12 categorías.
 */
@RunWith(AndroidJUnit4::class)
class RecorridoInicioFormularioTest {

    private val app: FinanzasApp get() = ApplicationProvider.getApplicationContext()

    @Before
    fun baseLimpia() = runBlocking {
        app.inicializacion.await()
        app.dependencias.baseDatos.clearAllTables()
        assertTrue(app.dependencias.inicializador.inicializar() is Resultado.Ok)
    }

    @After
    fun temaDelSistema() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Test
    fun registrar_ingreso_y_gasto_actualiza_inicio_y_se_conserva_al_reabrir() {
        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.textoSaldoDisponible, "Q 0.00")

            registrar(R.id.botonTipoIngreso, "100", "Salario", "Pago de la quincena")
            esperarTexto(R.id.textoSaldoDisponible, "Q 100.00")
            esperarTexto(R.id.textoIngresos, "Q 100.00")
            onView(withId(R.id.textoSinGastos)).check(matches(isDisplayed()))

            registrar(R.id.botonTipoGasto, "30", "Alimentación", "Almuerzo")
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
            esperarTexto(R.id.textoGastos, "Q 30.00")
            esperarTexto(R.id.textoResultado, "+Q 70.00")
            esperarTexto(R.id.textoTotalGastosAnillo, "Q 30.00")
            onView(withId(R.id.graficoGastos)).check(matches(isDisplayed()))
        }
        // Inicio se cierra y se vuelve a abrir: los datos vienen de SQLite, no de la pantalla anterior.
        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
            esperarTexto(R.id.textoGastos, "Q 30.00")
        }
    }

    @Test
    fun doble_toque_en_guardar_registra_un_solo_movimiento() {
        ActivityScenario.launch(FormularioActivity::class.java).use { escenario ->
            llenar(R.id.botonTipoGasto, "15", "Transporte", "Bus")
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), doubleClick())
            reintentar { assertEquals(Lifecycle.State.DESTROYED, escenario.state) }
        }
        runBlocking { assertEquals(1, app.dependencias.baseDatos.transaccionDao().contar()) }
    }

    @Test
    fun rotar_y_cambiar_a_tema_oscuro_conservan_lo_escrito() {
        ActivityScenario.launch(FormularioActivity::class.java).use { escenario ->
            llenar(R.id.botonTipoIngreso, "45.50", "Regalo", "Cumpleaños")

            escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            reintentar {
                escenario.onActivity {
                    assertEquals(Configuration.ORIENTATION_LANDSCAPE, it.resources.configuration.orientation)
                }
            }
            comprobarCampos("45.50", "Regalo", "Cumpleaños")

            escenario.onActivity { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
            reintentar {
                escenario.onActivity {
                    val modo = it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    assertEquals(Configuration.UI_MODE_NIGHT_YES, modo)
                }
            }
            comprobarCampos("45.50", "Regalo", "Cumpleaños")
            escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    @Test
    fun errores_claros_sin_categoria_y_con_coma_decimal() {
        ActivityScenario.launch(FormularioActivity::class.java).use {
            onView(withId(R.id.entradaMonto)).perform(replaceText("12,50"))
            onView(withId(R.id.entradaDescripcion)).perform(scrollTo(), replaceText("Prueba"), closeSoftKeyboard())

            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
            reintentar { onView(withText("Elige una categoría")).check(matches(isDisplayed())) }

            tocarCategoria("Salud")
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
            reintentar { onView(withText("Usa punto para los decimales")).check(matches(isDisplayed())) }
        }
        runBlocking { assertEquals(0, app.dependencias.baseDatos.transaccionDao().contar()) }
    }

    @Test
    fun las_categorias_se_filtran_por_tipo() {
        ActivityScenario.launch(FormularioActivity::class.java).use {
            reintentar { onView(withText("Alimentación")).check(matches(isDisplayed())) }
            onView(withText("Salario")).check(doesNotExist())

            onView(withId(R.id.botonTipoIngreso)).perform(click())
            reintentar { onView(withText("Salario")).check(matches(isDisplayed())) }
            onView(withText("Alimentación")).check(doesNotExist())
        }
    }

    @Test
    fun los_botones_rapidos_suman_al_monto() {
        ActivityScenario.launch(FormularioActivity::class.java).use {
            onView(withId(R.id.entradaMonto)).perform(replaceText("2.50"))
            onView(withId(R.id.chipSumar10)).perform(click())
            onView(withId(R.id.chipSumar5)).perform(click())
            onView(withId(R.id.entradaMonto)).check(matches(withText("17.50")))
        }
    }

    private fun registrar(botonTipo: Int, monto: String, categoria: String, descripcion: String) {
        onView(withId(R.id.botonAgregarMovimiento)).perform(click())
        llenar(botonTipo, monto, categoria, descripcion)
        onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
    }

    private fun llenar(botonTipo: Int, monto: String, categoria: String, descripcion: String) {
        onView(withId(botonTipo)).perform(click())
        onView(withId(R.id.entradaMonto)).perform(replaceText(monto))
        tocarCategoria(categoria)
        onView(withId(R.id.entradaDescripcion)).perform(scrollTo(), replaceText(descripcion), closeSoftKeyboard())
    }

    // Las categorías llegan de Room después de abrir la pantalla: se espera a que aparezcan.
    private fun tocarCategoria(nombre: String) {
        reintentar { onView(withText(nombre)).perform(scrollTo(), click()) }
        reintentar { onView(withContentDescription("$nombre, seleccionada")).check(matches(isDisplayed())) }
    }

    private fun comprobarCampos(monto: String, categoria: String, descripcion: String) {
        reintentar { onView(withId(R.id.entradaMonto)).check(matches(withText(monto))) }
        onView(withId(R.id.entradaDescripcion)).check(matches(withText(descripcion)))
        onView(withId(R.id.botonTipoIngreso)).check(matches(isChecked()))
        reintentar { onView(withContentDescription("$categoria, seleccionada")).perform(scrollTo()) }
    }

    private fun esperarTexto(id: Int, texto: String) =
        reintentar { onView(withId(id)).check(matches(withText(texto))) }

    /** Room responde en otro hilo y Espresso no lo espera: se repite la comprobación hasta 5 s. */
    private fun reintentar(accion: () -> Unit) {
        val limite = System.currentTimeMillis() + 5_000
        while (true) {
            try {
                accion()
                return
            } catch (error: Throwable) {
                if (System.currentTimeMillis() > limite) throw error
                Thread.sleep(100)
            }
        }
    }
}
