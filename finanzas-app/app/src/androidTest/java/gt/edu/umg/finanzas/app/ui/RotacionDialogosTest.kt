package gt.edu.umg.finanzas.app.ui

import android.app.UiAutomation
import android.content.res.Configuration
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.operaciones.OperacionesActivity
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RotacionDialogosTest {
    private val automatizacion = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun preparar() = runBlocking {
        AyudaInterfaz.baseLimpia()
        val hoy = Periodo.hoy()
        val categorias = app.dependencias.baseDatos.categoriaDao().obtenerTodas()
        for ((tipo, clave, monto, descripcion) in listOf(
            listOf("INGRESO", "SALARIO", "100", "Ingreso rotacion"),
            listOf("GASTO", "ALIMENTACION", "30", "Gasto rotacion"),
        )) {
            val categoria = categorias.first { it.clave == clave }
            assertTrue(app.dependencias.servicio.registrar(
                DatosMovimiento(TipoMovimiento.valueOf(tipo), monto, Moneda.GTQ, null,
                    categoria.idCategoria, descripcion, hoy), hoy,
            ) is Resultado.Ok)
        }
    }

    private fun girar(escenario: ActivityScenario<OperacionesActivity>, rotacion: Int, orientacion: Int) {
        automatizacion.setRotation(rotacion)
        reintentar {
            escenario.onActivity { assertEquals(orientacion, it.resources.configuration.orientation) }
        }
        automatizacion.waitForIdle(300, 5_000)
    }

    private fun comprobarBase(cantidad: Int, saldo: Long) = runBlocking {
        assertEquals(cantidad, app.dependencias.baseDatos.transaccionDao().contar())
        assertEquals(saldo, app.dependencias.baseDatos.usuarioDao().obtenerPorId(1)!!.saldoCentavos)
    }

    @Test
    fun detalle_permanece_al_girar_y_editar_conserva_el_movimiento() {
        preparar()
        try {
            automatizacion.setRotation(UiAutomation.ROTATION_FREEZE_0)
            ActivityScenario.launch(OperacionesActivity::class.java).use { escenario ->
                reintentar { onView(withText("Gasto rotacion")).perform(click()) }
                girar(escenario, UiAutomation.ROTATION_FREEZE_90, Configuration.ORIENTATION_LANDSCAPE)
                onView(withId(R.id.textoDescripcionDetalle)).check(matches(withText("Gasto rotacion")))
                girar(escenario, UiAutomation.ROTATION_FREEZE_0, Configuration.ORIENTATION_PORTRAIT)
                onView(withId(R.id.botonEditar)).perform(click())
                AyudaInterfaz.esperarTexto(R.id.entradaMonto, "30.00")
                onView(withId(R.id.entradaDescripcion)).check(matches(withText("Gasto rotacion")))
                pressBack()
                comprobarBase(2, 7000)
            }
        } finally {
            automatizacion.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
    }

    private fun confirmarTrasRotar(eliminar: Boolean) {
        preparar()
        try {
            automatizacion.setRotation(UiAutomation.ROTATION_FREEZE_0)
            ActivityScenario.launch(OperacionesActivity::class.java).use { escenario ->
                reintentar { onView(withText("Gasto rotacion")).perform(click()) }
                onView(withId(R.id.botonEliminar)).perform(click())
                girar(escenario, UiAutomation.ROTATION_FREEZE_90, Configuration.ORIENTATION_LANDSCAPE)
                onView(withText(R.string.confirmar_eliminar_titulo)).check(matches(isDisplayed()))
                girar(escenario, UiAutomation.ROTATION_FREEZE_0, Configuration.ORIENTATION_PORTRAIT)
                comprobarBase(2, 7000)
                onView(withText(if (eliminar) "Eliminar" else "Cancelar")).perform(click())
                reintentar { comprobarBase(if (eliminar) 1 else 2, if (eliminar) 10000 else 7000) }
                onView(withText(R.string.confirmar_eliminar_titulo)).check(doesNotExist())
            }
        } finally {
            automatizacion.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
    }

    @Test
    fun confirmacion_rotada_permite_cancelar_sin_borrar() = confirmarTrasRotar(eliminar = false)

    @Test
    fun confirmacion_rotada_elimina_una_sola_vez() = confirmarTrasRotar(eliminar = true)
}
