package gt.edu.umg.finanzas.app.ui

import android.content.res.Configuration
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.local.CatalogoReflexiones
import gt.edu.umg.finanzas.app.datos.preferencias.ReflexionGuardada
import gt.edu.umg.finanzas.app.datos.remoto.RespuestaDabarGuardada
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecorridoReflexionTest {
    private val instrumento = InstrumentationRegistry.getInstrumentation()
    private fun shell(orden: String) = ParcelFileDescriptor.AutoCloseInputStream(
        instrumento.uiAutomation.executeShellCommand(orden),
    ).bufferedReader().use { it.readText().trim() }

    private fun sinRed(accion: () -> Unit) {
        check(app.packageName.endsWith(".pruebas"))
        check(Build.HARDWARE in listOf("ranchu", "goldfish"))
        val wifi = shell("settings get global wifi_on")
        val datos = shell("settings get global mobile_data")
        val tema = app.dependencias.preferenciaTema.tema.value
        try {
            shell("svc wifi disable")
            shell("svc data disable")
            runBlocking { app.inicializacion.await() }
            val preferencias = app.getSharedPreferences("servicios", 0)
            preferencias.edit().apply {
                preferencias.all.keys.filter { it.startsWith("reflexion") }.forEach { remove(it) }
            }.commit()
            if (!app.dependencias.reflexion.estado.value.visible) app.dependencias.reflexion.cambiarVisibilidad()
            accion()
        } finally {
            shell("svc wifi ${if (wifi == "1") "enable" else "disable"}")
            shell("svc data ${if (datos == "1") "enable" else "disable"}")
            instrumento.runOnMainSync {
                app.dependencias.preferenciaTema.guardar(tema)
                AppCompatDelegate.setDefaultNightMode(tema.modoNocturno)
            }
        }
    }

    @Test
    fun cita_remota_guardada_conserva_metadatos_al_reintentar_sin_red_en_claro_y_oscuro() = sinRed {
        val hoy = LocalDate.now(ZoneId.of("America/Guatemala"))
        val cita = ReflexionGuardada(RespuestaDabarGuardada.leer(CatalogoReflexiones.paraFecha(hoy).referencia), hoy)
        runBlocking { app.dependencias.cacheServicios.guardarReflexion(cita) }
        ActivityScenario.launch(InicioActivity::class.java).use { escenario ->
            for ((tema, modo) in listOf("Claro" to Configuration.UI_MODE_NIGHT_NO, "Oscuro" to Configuration.UI_MODE_NIGHT_YES)) {
                onView(withId(R.id.opcionTema)).perform(click())
                onView(withText(tema)).perform(click())
                reintentar {
                    onView(withId(R.id.textoFuenteReflexion)).perform(scrollTo()).check(matches(withText(containsString("Dabar.io"))))
                }
                onView(withId(R.id.textoFuenteReflexion)).check { vista, error ->
                    if (error != null) throw error
                    assertEquals(modo, vista.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                }
                onView(withId(R.id.textoReflexion)).check(matches(withText(cita.reflexion.texto)))
                onView(withId(R.id.textoFuenteReflexion)).check(matches(withText(containsString(cita.reflexion.referencia))))
                onView(withId(R.id.textoFuenteReflexion)).check(matches(withText(containsString("Reina Valera 1909"))))
                onView(withId(R.id.botonReintentarReflexion)).perform(scrollTo(), click())
                reintentar {
                    onView(withId(R.id.textoFuenteReflexion)).check(matches(withText(containsString("No se pudo consultar Dabar.io"))))
                }
                assertEquals(cita, app.dependencias.cacheServicios.leerReflexion())
                escenario.recreate()
                reintentar {
                    onView(withId(R.id.textoFuenteReflexion)).perform(scrollTo()).check(matches(withText(containsString("Dabar.io"))))
                }
            }
        }
    }

    @Test
    fun sin_cache_y_sin_red_muestra_catalogo_local_y_su_propia_fuente() = sinRed {
        ActivityScenario.launch(InicioActivity::class.java).use {
            reintentar {
                onView(withId(R.id.textoFuenteReflexion)).perform(scrollTo()).check(matches(withText(containsString("Catálogo local · eBible"))))
            }
            onView(withId(R.id.botonReintentarReflexion)).perform(scrollTo(), click())
            reintentar { onView(withId(R.id.botonReintentarReflexion)).check(matches(withText("Reintentar reflexión"))) }
            onView(withId(R.id.textoFuenteReflexion)).check(matches(withText(containsString("Catálogo local · eBible"))))
            assertEquals("Catálogo local · eBible", app.dependencias.cacheServicios.leerReflexion()?.reflexion?.fuente)
        }
    }
}
