package gt.edu.umg.finanzas.app.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.remoto.tipocambio.LectorRespuestaBanguat
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.baseLimpia
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.capturar
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarTexto
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.core.dominio.*
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegibilidadTest {
    @Test fun importe_grande_no_oculta_la_etiqueta_del_resultado() {
        baseLimpia()
        runBlocking {
            val categoria = app.dependencias.baseDatos.categoriaDao().obtenerTodas().first { it.clave == "SALARIO" }
            assertTrue(app.dependencias.servicio.registrar(DatosMovimiento(TipoMovimiento.INGRESO,
                "9999999.99", Moneda.GTQ, null, categoria.idCategoria, "Ingreso de prueba", Periodo.hoy()),
                Periodo.hoy()) is Resultado.Ok)
        }
        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.textoSaldoDisponible, "Q 9,999,999.99")
            onView(withId(R.id.textoResultado)).perform(scrollTo()).check(matches(withText("+Q 9,999,999.99")))
            onView(withText(R.string.resultado_periodo)).check(matches(isDisplayed()))
            capturar("importe-grande")
        }
    }

    @Test fun detalle_usd_largo_permite_llegar_a_editar() {
        baseLimpia()
        val descripcion = "Descripcion extensa para comprobar el detalle de movimiento"
        val xml = InstrumentationRegistry.getInstrumentation().context.assets
            .open("banguat/tipo-cambio-dia-2026-10-29.xml").bufferedReader().use { it.readText() }
        runBlocking {
            val categoria = app.dependencias.baseDatos.categoriaDao().obtenerTodas().first { it.clave == "SALARIO" }
            assertTrue(app.dependencias.servicio.registrar(DatosMovimiento(TipoMovimiento.INGRESO,
                "999.99", Moneda.USD, LectorRespuestaBanguat.leer(xml), categoria.idCategoria, descripcion,
                Periodo.hoy()), Periodo.hoy()) is Resultado.Ok)
        }
        ActivityScenario.launch(InicioActivity::class.java).use {
            onView(withId(R.id.botonIrOperaciones)).perform(click())
            reintentar { onView(withText(descripcion)).perform(click()) }
            onView(withId(R.id.botonEditar)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.botonEliminar)).perform(scrollTo()).check(matches(isDisplayed()))
            for (id in listOf(R.id.botonEditar, R.id.botonEliminar)) {
                onView(withId(id)).check { vista, _ ->
                    val boton = vista as android.widget.TextView
                    assertTrue("Texto de acción cortado", boton.paint.measureText(boton.text.toString()) <=
                        boton.width - boton.compoundPaddingLeft - boton.compoundPaddingRight)
                }
            }
            capturar("detalle-largo")
            onView(withId(R.id.botonEditar)).perform(scrollTo(), click())
            esperarTexto(R.id.entradaMonto, "999.99")
        }
    }
}
