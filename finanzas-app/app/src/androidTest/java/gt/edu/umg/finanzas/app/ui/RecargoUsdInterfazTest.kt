package gt.edu.umg.finanzas.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.local.aEntity
import gt.edu.umg.finanzas.app.datos.preferencias.TasaGuardada
import gt.edu.umg.finanzas.app.datos.preferencias.Tema
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.app
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.capturar
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarTexto
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.mostrarInicio
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.core.dominio.*
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecargoUsdInterfazTest {
    private lateinit var temaAnterior: Tema
    private val referencia = TasaCambio(BigDecimal("7.64187"), Periodo.hoy().minusDays(2), "Referencia guardada de prueba")
    private val perfil get() = InstrumentationRegistry.getArguments().getString("perfilCapturas", "normal")

    @Before fun preparar() {
        AyudaInterfaz.baseLimpia()
        temaAnterior = app.dependencias.preferenciaTema.tema.value
        elegirTema(Tema.CLARO)
        guardarReferencia(referencia)
    }

    @After fun restaurarTema() = elegirTema(temaAnterior)

    private fun elegirTema(tema: Tema) = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        app.dependencias.preferenciaTema.guardar(tema)
        AppCompatDelegate.setDefaultNightMode(tema.modoNocturno)
    }

    private fun guardarReferencia(tasa: TasaCambio) = runBlocking {
        app.dependencias.cacheServicios.guardarTasa(TasaGuardada(tasa, Instant.now()))
        app.dependencias.tipoCambio.actualizar()
    }

    private fun comprobarTexto(id: Int, texto: String) =
        reintentar { onView(withId(id)).check(matches(withText(containsString(texto)))) }

    private fun abrirGasto(monto: String) {
        onView(withId(R.id.botonAgregarMovimiento)).perform(click())
        reintentar { onView(withId(R.id.botonMonedaUsd)).check(matches(isEnabled())).perform(scrollTo(), click()) }
        onView(withId(R.id.entradaMonto)).perform(replaceText(monto), closeSoftKeyboard())
    }

    private fun completarYGuardar(descripcion: String) {
        reintentar { onView(withText("Alimentación")).perform(scrollTo(), click()) }
        onView(withId(R.id.entradaDescripcion)).perform(scrollTo(), replaceText(descripcion), closeSoftKeyboard())
        onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
    }

    private fun comprobarNuevo(tema: Tema) {
        elegirTema(tema)
        val captura = "${tema.name.lowercase()}-$perfil"
        ActivityScenario.launch(InicioActivity::class.java).use {
            comprobarTexto(R.id.textoTasa, "7.64187")
            onView(withId(R.id.textoTasa)).check(matches(withText(not(containsString("7.94187")))))
            comprobarTexto(R.id.textoRecargoEstimado, "+Q 0.30 por cada USD")
            comprobarTexto(R.id.textoRecargoEstimado, "7.94187")
            onView(withId(R.id.textoRecargoEstimado)).perform(scrollTo())
            capturar("recargo-tarjeta-$captura")
            onView(withId(R.id.botonConversor)).perform(scrollTo(), click())
            onView(withId(R.id.entradaMontoConversor)).perform(replaceText("10"), closeSoftKeyboard())
            esperarTexto(R.id.textoResultadoConversion, "Q 76.42")
            onView(withId(R.id.botonGtqAUsd)).perform(click())
            onView(withId(R.id.entradaMontoConversor)).perform(replaceText("100"), closeSoftKeyboard())
            esperarTexto(R.id.textoResultadoConversion, "US$ 13.09")
            onView(withText("Cerrar")).perform(click())

            abrirGasto("10")
            comprobarTexto(R.id.textoTasaFormulario, "Referencia oficial: 1 USD = Q 7.64187")
            comprobarTexto(R.id.textoTasaFormulario, "Recargo estimado: Q 0.30 por cada USD")
            comprobarTexto(R.id.textoTasaFormulario, "Tasa aplicada: Q 7.94187")
            comprobarTexto(R.id.textoConversionPrevia, "Equivale a Q 79.42")
            comprobarTexto(R.id.textoConversionPrevia, "Referencia oficial: Q 76.42")
            comprobarTexto(R.id.textoConversionPrevia, "Recargo estimado: Q 3.00")
            assertEquals(0, runBlocking { app.dependencias.baseDatos.transaccionDao().contar() })
            onView(withId(R.id.textoTasaFormulario)).perform(mostrarInicio())
            capturar("recargo-condiciones-$captura")
            onView(withId(R.id.textoConversionPrevia)).perform(scrollTo())
            capturar("recargo-formulario-$captura")
            completarYGuardar("Gasto USD nuevo")
            esperarTexto(R.id.textoSaldoDisponible, "-Q 79.42")
            guardarReferencia(TasaCambio(BigDecimal("9.00000"), Periodo.hoy(), "Otra referencia de prueba"))
            comprobarTexto(R.id.textoTasa, "9.00000")
            esperarTexto(R.id.textoSaldoDisponible, "-Q 79.42")
            onView(withId(R.id.botonIrOperaciones)).perform(click())
            reintentar { onView(withText("Gasto USD nuevo")).perform(click()) }
            comprobarTexto(R.id.textoConversionDetalle, "7.64187")
            comprobarTexto(R.id.textoConversionDetalle, "7.94187")
            comprobarTexto(R.id.textoConversionDetalle, referencia.fuente)
            comprobarTexto(R.id.textoConversionDetalle, "Equivale a Q 79.42")
            onView(withId(R.id.textoConversionDetalle)).perform(mostrarInicio())
            capturar("recargo-detalle-$captura")
            onView(withId(R.id.botonEditar)).perform(scrollTo(), click())
            esperarTexto(R.id.entradaMonto, "10.00")
            comprobarTexto(R.id.textoTasaFormulario, "Se conserva el recargo guardado")
            onView(withId(R.id.entradaMonto)).perform(replaceText("12"), closeSoftKeyboard())
            comprobarTexto(R.id.textoConversionPrevia, "Equivale a Q 95.30")
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
            reintentar { onView(withText("Original: US$ 12.00")).check(matches(isDisplayed())) }
            val fila = runBlocking { app.dependencias.baseDatos.transaccionDao().obtenerTodas().single() }
            assertEquals(9530L, fila.montoCentavos)
            assertEquals("7.64187", fila.tasaGtqPorUsd.toPlainString())
            assertEquals("0.30", fila.recargoGtqPorUsd.toPlainString())
            reintentar { onView(withText("Gasto USD nuevo")).perform(click()) }
            onView(withId(R.id.botonEliminar)).perform(scrollTo(), click())
            reintentar { onView(withText("Eliminar")).perform(click()) }
            reintentar { assertEquals(0, runBlocking { app.dependencias.baseDatos.transaccionDao().contar() }) }
            onView(withId(R.id.botonIrInicio)).perform(click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 0.00")
        }
    }

    @Test fun gasto_nuevo_claro_y_detalle_historico() = comprobarNuevo(Tema.CLARO)
    @Test fun gasto_nuevo_oscuro_y_detalle_historico() = comprobarNuevo(Tema.OSCURO)

    @Test fun gasto_anterior_conserva_cero_en_detalle_y_edicion(): Unit = runBlocking {
        val base = app.dependencias.baseDatos
        val categoria = base.categoriaDao().obtenerTodas().first { it.clave == "ALIMENTACION" }.idCategoria
        val id = base.transaccionDao().insertar(Gasto(0, categoria, 7642, "Gasto anterior", Periodo.hoy(),
            ConversionAplicada(1000, referencia)).aEntity(0))
        base.usuarioDao().actualizarSaldo(1, -7642)
        guardarReferencia(referencia.copy(gtqPorUsd = BigDecimal("9.00000")))
        ActivityScenario.launch(InicioActivity::class.java).use {
            onView(withId(R.id.botonIrOperaciones)).perform(click())
            reintentar { onView(withText("Gasto anterior")).perform(click()) }
            comprobarTexto(R.id.textoConversionDetalle, "Recargo estimado: Q 0 por cada USD")
            comprobarTexto(R.id.textoConversionDetalle, "Tasa aplicada: Q 7.64187")
            comprobarTexto(R.id.textoConversionDetalle, "Equivale a Q 76.42")
            onView(withId(R.id.textoConversionDetalle)).perform(mostrarInicio())
            capturar("recargo-antiguo-detalle-$perfil")
            onView(withId(R.id.botonEditar)).perform(scrollTo(), click())
            esperarTexto(R.id.entradaMonto, "10.00")
            esperarTexto(R.id.textoConversionPrevia, "Equivale a Q 76.42")
            onView(withId(R.id.textoTasaFormulario)).perform(mostrarInicio())
            capturar("recargo-antiguo-formulario-$perfil")
            onView(withId(R.id.entradaMonto)).perform(scrollTo(), replaceText("12"), closeSoftKeyboard())
            esperarTexto(R.id.textoConversionPrevia, "Equivale a Q 91.70")
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
            reintentar { onView(withText("Original: US$ 12.00")).check(matches(isDisplayed())) }
            val fila = base.transaccionDao().obtenerPorId(id)!!
            assertEquals(9170L, fila.montoCentavos)
            assertEquals(0, fila.recargoGtqPorUsd.signum())
            assertEquals("7.64187", fila.tasaAplicadaGtqPorUsd.toPlainString())
        }
    }

    @Test fun siete_centavos_usd_muestran_desglose_que_suma_el_total_guardado() {
        ActivityScenario.launch(InicioActivity::class.java).use {
            abrirGasto("0.07")
            comprobarTexto(R.id.textoConversionPrevia, "Equivale a Q 0.56")
            comprobarTexto(R.id.textoConversionPrevia, "Referencia oficial: Q 0.53")
            comprobarTexto(R.id.textoConversionPrevia, "Recargo estimado: Q 0.03")
            onView(withId(R.id.textoConversionPrevia)).perform(scrollTo())
            capturar("recargo-redondeo-$perfil")
            completarYGuardar("Gasto pequeño")
            esperarTexto(R.id.textoSaldoDisponible, "-Q 0.56")
            assertEquals(56L, runBlocking { app.dependencias.baseDatos.transaccionDao().obtenerTodas().single().montoCentavos })
        }
    }
}
