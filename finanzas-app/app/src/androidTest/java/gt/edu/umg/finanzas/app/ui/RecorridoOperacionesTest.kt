package gt.edu.umg.finanzas.app.ui

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.esperarTexto
import gt.edu.umg.finanzas.app.ui.AyudaInterfaz.reintentar
import gt.edu.umg.finanzas.app.ui.inicio.InicioActivity
import gt.edu.umg.finanzas.app.ui.operaciones.OperacionesActivity
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Recorrido de Operaciones sobre la base de la app: lista, filtros, detalle, edición con el mismo
 * Formulario y eliminación con confirmación. Inicio debe reflejar cada cambio por los Flow de Room.
 */
@RunWith(AndroidJUnit4::class)
class RecorridoOperacionesTest {

    private val hoy = Periodo.hoy()

    @Before
    fun baseLimpia() = AyudaInterfaz.baseLimpia()

    // Los datos se crean por el servicio para empezar cada prueba con un estado conocido.
    private fun registrar(tipo: TipoMovimiento, monto: String, clave: String, descripcion: String, fecha: LocalDate = hoy) =
        runBlocking {
            val servicio = AyudaInterfaz.app.dependencias.servicio
            val categoria = AyudaInterfaz.app.dependencias.baseDatos.categoriaDao().obtenerTodas().first { it.clave == clave }
            val resultado = servicio.registrar(
                DatosMovimiento(tipo, monto, Moneda.GTQ, null, categoria.idCategoria, descripcion, fecha),
                hoy,
            )
            assertTrue("Llegó $resultado", resultado is Resultado.Ok)
        }

    private fun contarMovimientos(): Int = runBlocking { AyudaInterfaz.app.dependencias.baseDatos.transaccionDao().contar() }

    @Test
    fun lista_vacia_muestra_el_aviso() {
        ActivityScenario.launch(OperacionesActivity::class.java).use {
            reintentar { onView(withId(R.id.textoSinOperaciones)).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun filtros_por_tipo_y_periodo() {
        // "Ayer" debe caer en el mismo mes para que el filtro Mes lo muestre.
        assumeTrue(hoy.dayOfMonth > 1)
        registrar(TipoMovimiento.INGRESO, "100", "SALARIO", "Pago")
        registrar(TipoMovimiento.GASTO, "30", "ALIMENTACION", "Almuerzo")
        registrar(TipoMovimiento.GASTO, "20", "TRANSPORTE", "Bus", fecha = hoy.minusDays(1))

        ActivityScenario.launch(OperacionesActivity::class.java).use {
            reintentar { onView(withText("Bus")).check(matches(isDisplayed())) }
            onView(withText("Pago")).check(matches(isDisplayed()))

            onView(withId(R.id.chipIngresos)).perform(click())
            reintentar { onView(withText("Almuerzo")).check(doesNotExist()) }
            onView(withText("Pago")).check(matches(isDisplayed()))

            onView(withId(R.id.chipGastos)).perform(click())
            reintentar { onView(withText("Pago")).check(doesNotExist()) }
            onView(withText("Almuerzo")).check(matches(isDisplayed()))

            onView(withId(R.id.chipDia)).perform(click())
            reintentar { onView(withText("Bus")).check(doesNotExist()) }
            onView(withText("Almuerzo")).check(matches(isDisplayed()))

            // Con Día y Todos vuelve a aparecer el ingreso de hoy: los dos filtros se combinan.
            onView(withId(R.id.chipTodos)).perform(click())
            reintentar { onView(withText("Pago")).check(matches(isDisplayed())) }
        }
    }

    @Test
    fun editar_importe_y_tipo_actualiza_operaciones_e_inicio() {
        registrar(TipoMovimiento.INGRESO, "100", "SALARIO", "Pago")
        registrar(TipoMovimiento.GASTO, "30", "ALIMENTACION", "Almuerzo")

        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
            onView(withId(R.id.botonIrOperaciones)).perform(click())

            reintentar { onView(withText("Almuerzo")).perform(click()) }
            reintentar { onView(withId(R.id.botonEditar)).perform(click()) }

            // El mismo Formulario llega con los datos guardados.
            esperarTexto(R.id.textoTituloFormulario, "Editar movimiento")
            esperarTexto(R.id.entradaMonto, "30.00")
            onView(withId(R.id.botonTipoIngreso)).perform(click())
            onView(withId(R.id.entradaMonto)).perform(replaceText("45"))
            reintentar { onView(withText("Regalo")).perform(scrollTo(), click()) }
            reintentar { onView(withContentDescription("Regalo, seleccionada")).check(matches(isDisplayed())) }
            onView(withId(R.id.entradaDescripcion)).perform(scrollTo(), replaceText("Almuerzo pagado"), closeSoftKeyboard())
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())

            // De vuelta en Operaciones con la fila corregida; luego Inicio con el saldo nuevo.
            reintentar { onView(withText("Almuerzo pagado")).check(matches(isDisplayed())) }
            onView(withText("+Q 45.00")).check(matches(isDisplayed()))
            onView(withId(R.id.botonIrInicio)).perform(click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 145.00")
            esperarTexto(R.id.textoIngresos, "Q 145.00")
            esperarTexto(R.id.textoGastos, "Q 0.00")
        }
        assertEquals(2, contarMovimientos())
    }

    @Test
    fun cancelar_la_eliminacion_no_borra_y_confirmar_si() {
        registrar(TipoMovimiento.INGRESO, "100", "SALARIO", "Pago")
        registrar(TipoMovimiento.GASTO, "30", "ALIMENTACION", "Almuerzo")
        registrar(TipoMovimiento.GASTO, "20", "TRANSPORTE", "Bus")

        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.textoSaldoDisponible, "Q 50.00")
            onView(withId(R.id.botonIrOperaciones)).perform(click())

            // Cancelar: nada cambia.
            reintentar { onView(withText("Bus")).perform(click()) }
            reintentar { onView(withId(R.id.botonEliminar)).perform(click()) }
            reintentar { onView(withText("Cancelar")).perform(click()) }
            assertEquals(3, contarMovimientos())
            onView(withText("Bus")).check(matches(isDisplayed()))

            // Confirmar: se borra el movimiento y el saldo vuelve a sumar Q 20.
            onView(withText("Bus")).perform(click())
            reintentar { onView(withId(R.id.botonEliminar)).perform(click()) }
            reintentar { onView(withText("Eliminar")).perform(click()) }
            reintentar { onView(withText("Bus")).check(doesNotExist()) }
            assertEquals(2, contarMovimientos())

            pressBack()
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
        }
    }

    /** Caso a del diseño (§14.1) desde la pantalla: Q 70 → editar el gasto a Q 20 → Q 80 → eliminarlo → Q 100. */
    @Test
    fun caso_a_editar_y_eliminar_el_gasto_desde_operaciones() {
        registrar(TipoMovimiento.INGRESO, "100", "SALARIO", "Pago")
        registrar(TipoMovimiento.GASTO, "30", "ALIMENTACION", "Almuerzo")

        ActivityScenario.launch(InicioActivity::class.java).use {
            esperarTexto(R.id.textoSaldoDisponible, "Q 70.00")
            onView(withId(R.id.botonIrOperaciones)).perform(click())

            reintentar { onView(withText("Almuerzo")).perform(click()) }
            reintentar { onView(withId(R.id.botonEditar)).perform(click()) }
            esperarTexto(R.id.entradaMonto, "30.00")
            onView(withId(R.id.entradaMonto)).perform(replaceText("20"), closeSoftKeyboard())
            onView(withId(R.id.botonGuardar)).perform(scrollTo(), click())
            reintentar { onView(withText("-Q 20.00")).check(matches(isDisplayed())) }

            onView(withId(R.id.botonIrInicio)).perform(click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 80.00")

            onView(withId(R.id.botonIrOperaciones)).perform(click())
            reintentar { onView(withText("Almuerzo")).perform(click()) }
            reintentar { onView(withId(R.id.botonEliminar)).perform(click()) }
            reintentar { onView(withText("Eliminar")).perform(click()) }
            reintentar { onView(withText("Almuerzo")).check(doesNotExist()) }

            onView(withId(R.id.botonIrInicio)).perform(click())
            esperarTexto(R.id.textoSaldoDisponible, "Q 100.00")
        }
        assertEquals(1, contarMovimientos())
    }

    @Test
    fun la_barra_inferior_va_y_vuelve_sin_apilar_pantallas() {
        ActivityScenario.launch(OperacionesActivity::class.java).use { escenario ->
            // Tocar el destino actual no hace nada.
            onView(withId(R.id.botonIrOperaciones)).perform(click())
            assertEquals(Lifecycle.State.RESUMED, escenario.state)
            // "Inicio" cierra Operaciones en lugar de abrir otra copia de Inicio encima.
            onView(withId(R.id.botonIrInicio)).perform(click())
            reintentar { assertEquals(Lifecycle.State.DESTROYED, escenario.state) }
        }
    }
}
