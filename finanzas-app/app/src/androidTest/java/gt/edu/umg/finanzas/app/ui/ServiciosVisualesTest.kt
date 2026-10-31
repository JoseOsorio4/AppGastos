package gt.edu.umg.finanzas.app.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.preferencias.*
import gt.edu.umg.finanzas.app.datos.remoto.RespuestaDabarGuardada
import gt.edu.umg.finanzas.app.datos.remoto.clima.LectorRespuestaClima
import gt.edu.umg.finanzas.app.datos.remoto.tipocambio.LectorRespuestaBanguat
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.capturar
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.mostrarInicio
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ServiciosVisualesTest {
    private val instrumento = InstrumentationRegistry.getInstrumentation()
    private fun fixture(ruta: String) = instrumento.context.assets.open(ruta).bufferedReader().use { it.readText() }

    @Test
    fun metadatos_guardados_y_tema_accesible() {
        check(app.packageName.endsWith(".pruebas"))
        runBlocking {
            val cache = app.dependencias.cacheServicios
            cache.guardarTasa(TasaGuardada(LectorRespuestaBanguat.leer(fixture("banguat/tipo-cambio-dia-2026-10-29.xml")), Instant.now()))
            cache.guardarClima(ClimaGuardado(LectorRespuestaClima.leer(fixture("openmeteo/clima-2026-10-30.json")), Instant.now()))
            cache.guardarReflexion(ReflexionGuardada(RespuestaDabarGuardada.leer("Filipenses 4:13"),
                LocalDate.now(ZoneId.of("America/Guatemala"))))
            app.dependencias.tipoCambio.actualizar()
            app.dependencias.clima.actualizar()
            app.dependencias.reflexion.actualizar()
        }
        if (!app.dependencias.reflexion.estado.value.visible) app.dependencias.reflexion.cambiarVisibilidad()
        ActivityScenario.launch(InicioActivity::class.java).use {
            onView(withId(R.id.opcionTema)).check(matches(withContentDescription("Cambiar tema")))
            onView(withId(R.id.opcionTema)).check { vista, _ ->
                assertTrue(vista.width / vista.resources.displayMetrics.density >= 48)
                assertTrue(vista.height / vista.resources.displayMetrics.density >= 48)
            }
            for (tema in listOf("Claro", "Oscuro")) {
                val perfil = InstrumentationRegistry.getArguments().getString("perfilCapturas", "normal")
                val sufijo = if (perfil == "normal") "" else "-$perfil"
                onView(withId(R.id.opcionTema)).perform(click())
                onView(withText(tema)).perform(click())
                reintentar { onView(withId(R.id.textoTasa)).perform(mostrarInicio()).check(matches(withText(containsString("7.64382")))) }
                onView(withId(R.id.botonReintentarTasa)).perform(scrollTo())
                capturar("servicios-${tema.lowercase()}-tasa$sufijo")
                onView(withId(R.id.textoClima)).perform(mostrarInicio()).check(matches(withText(containsString("18.3"))))
                onView(withId(R.id.botonReintentarClima)).perform(scrollTo())
                capturar("servicios-${tema.lowercase()}-clima$sufijo")
                onView(withId(R.id.textoFuenteReflexion)).perform(mostrarInicio()).check(matches(withText(containsString("Reina Valera 1909"))))
                onView(withId(R.id.botonVisibilidadReflexion)).perform(scrollTo())
                capturar("servicios-${tema.lowercase()}-reflexion$sufijo")
            }
            onView(withId(R.id.opcionTema)).perform(click())
            onView(withText("Sistema")).perform(click())
        }
    }
}
