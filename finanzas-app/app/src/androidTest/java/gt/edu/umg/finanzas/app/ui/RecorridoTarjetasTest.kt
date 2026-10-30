package gt.edu.umg.finanzas.app.ui

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.preferencias.ClimaGuardado
import gt.edu.umg.finanzas.app.datos.preferencias.TasaGuardada
import gt.edu.umg.finanzas.app.datos.remoto.clima.LectorRespuestaClima
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarTexto
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorridoTarjetasTest {
    private fun shell(orden: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(orden),
    ).bufferedReader().use { it.readText().trim() }

    private fun sinRed(accion: () -> Unit) {
        check(Build.HARDWARE in listOf("ranchu", "goldfish"))
        val wifi = shell("settings get global wifi_on")
        val datos = shell("settings get global mobile_data")
        try {
            shell("svc wifi disable")
            shell("svc data disable")
            AyudaInterfaz.baseLimpia()
            app.getSharedPreferences("servicios", 0).edit().clear().commit()
            runBlocking {
                app.dependencias.cacheServicios.guardarTasa(TasaGuardada(
                    TasaCambio(BigDecimal("7.66000"), LocalDate.now(), "Ficticia de prueba"), Instant.now(),
                ))
                app.dependencias.tipoCambio.actualizar()
                app.dependencias.reflexion.actualizar()
            }
            if (!app.dependencias.reflexion.estado.value.visible) app.dependencias.reflexion.cambiarVisibilidad()
            accion()
        } finally {
            shell("svc wifi ${if (wifi == "1") "enable" else "disable"}")
            shell("svc data ${if (datos == "1") "enable" else "disable"}")
        }
    }

    @Test
    fun clima_sin_datos_no_bloquea_reflexion_conversor_ni_registro_gtq() = sinRed {
        ActivityScenario.launch(InicioActivity::class.java).use {
            reintentar {
                onView(withId(R.id.textoClima)).perform(scrollTo()).check(matches(withText("Sin datos del clima")))
                onView(withId(R.id.textoEstadoClima)).check(matches(withText(containsString("No se pudo actualizar"))))
            }
            onView(withId(R.id.botonReintentarClima)).perform(scrollTo(), click())
            reintentar { assertTrue(app.dependencias.clima.estado.value.error) }
            onView(withId(R.id.textoFuenteReflexion)).perform(scrollTo()).check(matches(withText(containsString("Catálogo local"))))
            onView(withId(R.id.botonConversor)).perform(scrollTo(), click())
            onView(withId(R.id.entradaMontoConversor)).perform(replaceText("10"), closeSoftKeyboard())
            esperarTexto(R.id.textoResultadoConversion, "Q 76.60")
            onView(withText("Cerrar")).perform(click())
            onView(withId(R.id.botonAgregarMovimiento)).perform(click())
            onView(withId(R.id.botonTipoIngreso)).perform(click())
            onView(withId(R.id.entradaMonto)).perform(replaceText("10"), closeSoftKeyboard())
            onView(withText("Salario")).perform(scrollTo(), click())
            onView(withId(R.id.entradaDescripcion)).perform(scrollTo(), replaceText("Ingreso sin clima"), closeSoftKeyboard())
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 10.00")
        }
    }

    @Test
    fun dato_guardado_resiste_reintento_sin_red_y_reflexion_se_puede_ocultar() = sinRed {
        val clima = InstrumentationRegistry.getInstrumentation().context.assets
            .open("openmeteo/clima-2026-10-30.json").bufferedReader().use { LectorRespuestaClima.leer(it.readText()) }
        val guardado = ClimaGuardado(clima, Instant.now())
        runBlocking { app.dependencias.cacheServicios.guardarClima(guardado) }
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            reintentar { onView(withId(R.id.textoClima)).perform(scrollTo()).check(matches(withText(containsString("Despejado")))) }
            onView(withId(R.id.textoEstadoClima)).check(matches(withText(containsString("Dato guardado"))))
            onView(withId(R.id.botonReintentarClima)).perform(scrollTo(), click())
            reintentar { onView(withId(R.id.textoEstadoClima)).check(matches(withText(containsString("No se pudo actualizar")))) }
            assertEquals(guardado, app.dependencias.cacheServicios.leerClima())
            onView(withId(R.id.botonVisibilidadReflexion)).perform(scrollTo(), click())
            onView(withId(R.id.contenidoReflexion)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            escenario.recreate()
            onView(withId(R.id.botonVisibilidadReflexion)).perform(scrollTo()).check(matches(withText("Mostrar reflexión")))
            onView(withId(R.id.botonVisibilidadReflexion)).perform(click())
            onView(withId(R.id.textoReflexion)).check(matches(isDisplayed()))
        }
    }
}
