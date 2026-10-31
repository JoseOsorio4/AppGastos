package gt.edu.umg.finanzas.app.ui

import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.preferencias.Tema
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.baseLimpia
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.capturar
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarAccesoListo
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.mostrarInicio
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.acceso.AccesoActivity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccesoAparienciaTest {
    private val instrumento = InstrumentationRegistry.getInstrumentation()

    private fun comprobar(tema: Tema) {
        check(app.packageName.endsWith(".pruebas"))
        baseLimpia()
        val anterior = app.dependencias.preferenciaTema.tema.value
        val archivo = File(app.noBackupFilesDir, "acceso.json")
        check(archivo.delete() || !archivo.exists())
        app.dependencias.bloqueo.preparar(null)
        instrumento.runOnMainSync {
            app.dependencias.preferenciaTema.guardar(tema)
            AppCompatDelegate.setDefaultNightMode(tema.modoNocturno)
        }
        val perfil = InstrumentationRegistry.getArguments().getString("perfilCapturas", "normal")
        val sufijo = "${tema.name.lowercase()}-$perfil"
        try {
            ActivityScenario.launch(AccesoActivity::class.java).use {
                reintentar { onView(withId(R.id.entradaNombre)).check(matches(withEffectiveVisibility(Visibility.VISIBLE))) }
                esperarAccesoListo()
                capturar("acceso-nombre-$sufijo")
                onView(withId(R.id.entradaNombre)).perform(scrollTo(), click(), typeText("Ana Maria"))
                reintentar { comprobarCursor(R.id.entradaNombre) }
                capturar("acceso-nombre-teclado-$sufijo")
                onView(withId(R.id.botonAcceso)).perform(scrollTo()).check(matches(isDisplayed()))
                comprobarTecladoAbierto()
                onView(withId(R.id.entradaNombre)).check(matches(withText("Ana Maria"))).perform(closeSoftKeyboard())
                onView(withId(R.id.botonAcceso)).perform(scrollTo(), click())
                esperarAccesoListo()
                onView(withId(R.id.tituloAcceso)).perform(mostrarInicio())
                    .check(matches(withText("¡Bienvenido, Ana Maria!")))
                capturar("acceso-saludo-$sufijo")
                onView(withId(R.id.botonContinuar)).perform(scrollTo()).check(matches(isDisplayed()))
                onView(withId(R.id.botonConfigurarPin)).perform(scrollTo(), click())
                esperarAccesoListo()
                onView(withId(R.id.tituloAcceso)).perform(mostrarInicio())
                capturar("acceso-configurar-$sufijo")
                onView(withId(R.id.entradaPinNuevo)).perform(scrollTo(), click(), typeText("123456"))
                reintentar { comprobarCursor(R.id.entradaPinNuevo) }
                onView(withId(R.id.entradaPinConfirmacion)).perform(scrollTo(), click(), typeText("123456"))
                reintentar { comprobarCursor(R.id.entradaPinConfirmacion) }
                capturar("acceso-pin-teclado-$sufijo")
                onView(withId(R.id.botonAcceso)).perform(scrollTo()).check(matches(isDisplayed()))
                comprobarTecladoAbierto()
                onView(withId(R.id.entradaPinConfirmacion)).check(matches(withText("123456")))
                // El acceso no incluye vistas financieras, tampoco antes de crear el PIN.
                onView(withId(R.id.scrollAcceso)).check { vista, _ ->
                    assertNull(vista.rootView.findViewById<View>(R.id.textoSaldoDisponible))
                }
                onView(withId(R.id.entradaPinConfirmacion)).perform(closeSoftKeyboard())
                onView(withId(R.id.botonContinuar)).perform(scrollTo(), click())
                esperarAccesoListo()
                assertFalse(app.dependencias.bloqueo.tienePin())
                assertFalse(app.dependencias.bloqueo.necesitaBienvenida())
                assertEquals("Ana Maria", runBlocking {
                    app.dependencias.baseDatos.usuarioDao().obtenerPorId(1)!!.nombrePerfil
                })
            }
        } finally {
            archivo.delete()
            app.dependencias.bloqueo.preparar("Perfil de prueba")
            instrumento.runOnMainSync {
                app.dependencias.preferenciaTema.guardar(anterior)
                AppCompatDelegate.setDefaultNightMode(anterior.modoNocturno)
            }
        }
    }

    private fun comprobarTecladoAbierto() = onView(withId(R.id.scrollAcceso)).check { vista, _ ->
        assertTrue(ViewCompat.getRootWindowInsets(vista)!!.isVisible(WindowInsetsCompat.Type.ime()))
    }

    private fun comprobarCursor(id: Int) = onView(withId(id)).check { vista, _ ->
        val campo = vista as EditText
        val insets = ViewCompat.getRootWindowInsets(campo)!!
        assertTrue(insets.isVisible(WindowInsetsCompat.Type.ime()))
        assertTrue("Debe abrir el teclado inferior, no el panel flotante de escritura", insets.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0)
        val posicion = IntArray(2)
        campo.getLocationInWindow(posicion)
        val limite = campo.rootView.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val linea = campo.layout.getLineForOffset(campo.selectionEnd.coerceAtLeast(0))
        val cursor = posicion[1] + campo.totalPaddingTop + campo.layout.getLineBottom(linea) - campo.scrollY
        assertTrue("Cursor $cursor bajo teclado $limite", cursor <= limite)
        assertTrue("Campo oculto por el teclado", posicion[1] + campo.height <= limite)
    }

    @Test fun bienvenida_clara_con_teclado() = comprobar(Tema.CLARO)
    @Test fun bienvenida_oscura_con_teclado() = comprobar(Tema.OSCURO)
}
