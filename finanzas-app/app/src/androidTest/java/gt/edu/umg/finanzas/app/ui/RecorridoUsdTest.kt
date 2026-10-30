package gt.edu.umg.finanzas.app.ui

import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.preferencias.TasaGuardada
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarTexto
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.comun.SentidoConversion
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorridoUsdTest {
    private val app get() = AyudaInterfaz.app

    @Before
    fun preparar() {
        AyudaInterfaz.baseLimpia()
        guardarReferencia()
    }

    private fun guardarReferencia(valor: String = "7.66000", antigua: Boolean = false) = runBlocking {
        val tasa = TasaCambio(BigDecimal(valor), Periodo.hoy().minusDays(if (antigua) 1 else 0), "Ficticia de prueba")
        app.dependencias.cacheServicios.guardarTasa(TasaGuardada(tasa, if (antigua) Instant.now().minusSeconds(86_400) else Instant.now()))
        app.dependencias.tipoCambio.actualizar()
    }

    private fun registrar(moneda: Int, monto: String, descripcion: String) {
        onView(withId(R.id.botonAgregarMovimiento)).perform(click())
        onView(withId(R.id.botonTipoIngreso)).perform(click())
        reintentar { onView(withId(moneda)).check(matches(isEnabled())) }
        onView(withId(moneda)).perform(click())
        onView(withId(R.id.entradaMonto)).perform(replaceText(monto), closeSoftKeyboard())
        reintentar { onView(withText("Salario")).perform(scrollTo(), click()) }
        onView(withId(R.id.entradaDescripcion)).perform(scrollTo(), replaceText(descripcion), closeSoftKeyboard())
        onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
    }

    @Test
    fun caso_i_el_conversor_solo_ofrece_los_dos_sentidos_y_conserva_estado() {
        assertEquals(listOf(Moneda.GTQ, Moneda.USD), Moneda.entries)
        assertEquals(2, SentidoConversion.entries.size)
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            reintentar { onView(withId(R.id.botonConversor)).perform(scrollTo(), click()) }
            onView(withId(R.id.grupoSentido)).check { vista, _ -> assertEquals(2, (vista as ViewGroup).childCount) }
            onView(withId(R.id.entradaMontoConversor)).perform(replaceText("10"), closeSoftKeyboard())
            esperarTexto(R.id.textoResultadoConversion, "Q 76.60")
            onView(withId(R.id.botonGtqAUsd)).perform(click())
            onView(withId(R.id.entradaMontoConversor)).perform(replaceText("100"), closeSoftKeyboard())
            esperarTexto(R.id.textoResultadoConversion, "US$ 13.05")
            escenario.recreate()
            esperarTexto(R.id.entradaMontoConversor, "100")
            esperarTexto(R.id.textoResultadoConversion, "US$ 13.05")
            onView(withText("Cerrar")).perform(click())
        }
    }

    @Test
    fun caso_e_registra_muestra_y_edita_usd_con_la_tasa_historica() {
        ActivityScenario.launch(InicioActivity::class.java).use {
            registrar(R.id.botonMonedaUsd, "10", "Pago USD")
            esperarTexto(R.id.textoSaldoDisponible, "Q 76.60")
            guardarReferencia("7.70000")
            esperarTexto(R.id.textoSaldoDisponible, "Q 76.60")
            onView(withId(R.id.botonIrOperaciones)).perform(click())
            reintentar { onView(withText("Original: US$ 10.00")).check(matches(isDisplayed())) }
            onView(withText("Pago USD")).perform(click())
            reintentar { onView(withId(R.id.textoConversionDetalle)).check(matches(withText(containsString("7.66000")))) }
            onView(withId(R.id.botonEditar)).perform(click())
            esperarTexto(R.id.entradaMonto, "10.00")
            onView(withId(R.id.textoTasaFormulario)).check(matches(withText(containsString("Se conserva la tasa"))))
            onView(withId(R.id.entradaMonto)).perform(replaceText("12"), closeSoftKeyboard())
            esperarTexto(R.id.textoConversionPrevia, "Equivale a Q 91.92")
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
            reintentar { onView(withText("Original: US$ 12.00")).check(matches(isDisplayed())) }
            onView(withId(R.id.botonIrInicio)).perform(click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 91.92")
        }
    }

    private fun shell(orden: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(orden),
    ).bufferedReader().use { it.readText().trim() }

    @Test
    fun sin_red_gtq_funciona_y_usd_exige_una_tasa_guardada() {
        check(Build.HARDWARE in listOf("ranchu", "goldfish")) { "Esta prueba de conectividad es solo para emulador" }
        val wifi = shell("settings get global wifi_on")
        val datos = shell("settings get global mobile_data")
        try {
            shell("svc wifi disable")
            shell("svc data disable")
            app.getSharedPreferences("servicios", 0).edit().clear().commit()
            runBlocking { app.dependencias.tipoCambio.actualizar(forzar = true) }
            assertNull(app.dependencias.tipoCambio.estado.value.dato)
            assertTrue(app.dependencias.tipoCambio.estado.value.error)
            ActivityScenario.launch(InicioActivity::class.java).use {
                onView(withId(R.id.botonAgregarMovimiento)).perform(click())
                reintentar { onView(withId(R.id.botonMonedaUsd)).check(matches(not(isEnabled()))) }
                onView(withId(R.id.textoTasaFormulario)).check(matches(withText(containsString("Sin tasa disponible"))))
                pressBack()
                registrar(R.id.botonMonedaGtq, "100", "Sin internet GTQ")
                esperarTexto(R.id.textoSaldoDisponible, "Q 100.00")
                guardarReferencia(antigua = true)
                assertTrue(app.dependencias.tipoCambio.estado.value.error)
                registrar(R.id.botonMonedaUsd, "10", "Sin internet USD")
                esperarTexto(R.id.textoSaldoDisponible, "Q 176.60")
                onView(withId(R.id.textoTasa)).perform(scrollTo()).check(matches(withText(containsString("Última disponible"))))
                onView(withId(R.id.textoTasa)).check(matches(withText(containsString("No se pudo actualizar"))))
            }
        } finally {
            shell(if (wifi == "1") "svc wifi enable" else "svc wifi disable")
            shell(if (datos == "1") "svc data enable" else "svc data disable")
        }
    }
}
