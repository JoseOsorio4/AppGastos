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
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.capturar
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.formulario.FormularioActivity
import gt.edu.umg.finanzas.core.dominio.*
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TecladoFormularioTest {
    private fun comprobar(edicion: Boolean, tema: Tema) {
        AyudaInterfaz.baseLimpia()
        val instrumento = InstrumentationRegistry.getInstrumentation()
        val anterior = app.dependencias.preferenciaTema.tema.value
        val id = if (edicion) runBlocking {
            val categoria = app.dependencias.baseDatos.categoriaDao().obtenerTodas().first { it.clave == "SALARIO" }
            val resultado = app.dependencias.servicio.registrar(
                DatosMovimiento(TipoMovimiento.INGRESO, "100", Moneda.GTQ, null,
                    categoria.idCategoria, "Antes", Periodo.hoy()), Periodo.hoy(),
            )
            (resultado as Resultado.Ok).valor
        } else null
        instrumento.runOnMainSync {
            app.dependencias.preferenciaTema.guardar(tema)
            AppCompatDelegate.setDefaultNightMode(tema.modoNocturno)
        }
        try {
            val intent = if (id == null) FormularioActivity.paraRegistrar(app) else FormularioActivity.paraEditar(app, id)
            ActivityScenario.launch<FormularioActivity>(intent).use {
                onView(withId(R.id.entradaDescripcion)).perform(scrollTo(), clearText(), click())
                reintentar { comprobarCursorVisible() }
                onView(withId(R.id.entradaDescripcion)).perform(typeText("Descripcion visible mientras escribo"))
                reintentar { comprobarCursorVisible() }
                val perfil = InstrumentationRegistry.getArguments().getString("perfilCapturas", "normal")
                val caso = "${if (edicion) "editar" else "crear"}-${tema.name.lowercase()}-$perfil"
                capturar("teclado-$caso")
                onView(withId(R.id.botonGuardar)).perform(scrollTo()).check(matches(isDisplayed()))
                onView(withId(R.id.raiz)).check { vista, _ ->
                    assertTrue(ViewCompat.getRootWindowInsets(vista)!!.isVisible(WindowInsetsCompat.Type.ime()))
                }
                onView(withId(R.id.entradaDescripcion)).check(matches(withText("Descripcion visible mientras escribo")))
                if (!edicion && tema == Tema.CLARO) capturar("guardar-$perfil")
                onView(withId(R.id.entradaDescripcion)).perform(closeSoftKeyboard())
            }
        } finally {
            instrumento.runOnMainSync {
                app.dependencias.preferenciaTema.guardar(anterior)
                AppCompatDelegate.setDefaultNightMode(anterior.modoNocturno)
            }
        }
    }

    private fun comprobarCursorVisible() = onView(withId(R.id.entradaDescripcion)).check { vista, _ ->
        val campo = vista as EditText
        val insets = ViewCompat.getRootWindowInsets(campo)!!
        assertTrue("El teclado debe permanecer abierto", insets.isVisible(WindowInsetsCompat.Type.ime()))
        val posicion = IntArray(2)
        campo.getLocationInWindow(posicion)
        val limite = campo.rootView.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val linea = campo.layout.getLineForOffset(campo.selectionEnd.coerceAtLeast(0))
        val cursor = posicion[1] + campo.totalPaddingTop + campo.layout.getLineBottom(linea) - campo.scrollY
        assertTrue("Cursor $cursor bajo teclado $limite", cursor <= limite)
        assertTrue("Campo oculto tras el teclado", posicion[1] + campo.height <= limite)
        assertEquals(View.VISIBLE, campo.visibility)
    }

    @Test fun crear_claro() = comprobar(false, Tema.CLARO)
    @Test fun crear_oscuro() = comprobar(false, Tema.OSCURO)
    @Test fun editar_claro() = comprobar(true, Tema.CLARO)
    @Test fun editar_oscuro() = comprobar(true, Tema.OSCURO)
}
