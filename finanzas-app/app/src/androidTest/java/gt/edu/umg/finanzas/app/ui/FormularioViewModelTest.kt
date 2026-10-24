package gt.edu.umg.finanzas.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.local.AppDatabase
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.app.datos.local.InicializadorDatos
import gt.edu.umg.finanzas.app.ui.formulario.FormularioViewModel
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Muerte del proceso: el ViewModel nuevo recupera los campos desde SavedStateHandle (§9.5). */
@RunWith(AndroidJUnit4::class)
class FormularioViewModelTest {

    private lateinit var baseDatos: AppDatabase

    @Before
    fun abrir() = runBlocking {
        baseDatos = BaseDePrueba.enMemoria()
        InicializadorDatos(baseDatos).inicializar().esperarOk()
    }

    @After
    fun cerrar() = baseDatos.close()

    @Test
    fun recupera_los_campos_guardados_tras_la_muerte_del_proceso() {
        val guardado = SavedStateHandle(
            mapOf(
                FormularioViewModel.CLAVE_TIPO to "INGRESO",
                FormularioViewModel.CLAVE_MONTO to "12.5",
                FormularioViewModel.CLAVE_CATEGORIA to 9L,
                FormularioViewModel.CLAVE_FECHA to LocalDate.of(2026, 10, 20).toEpochDay(),
                FormularioViewModel.CLAVE_DESCRIPCION to "Pago",
            ),
        )
        lateinit var viewModel: FormularioViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = FormularioViewModel(BaseDePrueba.servicioSobre(baseDatos), guardado)
        }

        val estado = viewModel.estado.value
        assertEquals(TipoMovimiento.INGRESO, estado.tipo)
        assertEquals("12.5", estado.montoTexto)
        assertEquals(9L, estado.idCategoria)
        assertEquals(LocalDate.of(2026, 10, 20), estado.fecha)
        assertEquals("Pago", estado.descripcion)
    }
}
