package gt.edu.umg.finanzas.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.local.AppDatabase
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.app.datos.local.InicializadorDatos
import gt.edu.umg.finanzas.app.datos.local.OperacionesMovimientoRoom
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.repositorios.RepositorioTipoCambio
import gt.edu.umg.finanzas.app.ui.formulario.FormularioUiState
import gt.edu.umg.finanzas.app.ui.formulario.FormularioViewModel
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.dominio.ConversionAplicada
import gt.edu.umg.finanzas.core.dominio.Gasto
import gt.edu.umg.finanzas.app.datos.local.aEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.math.BigDecimal

/**
 * Recuperación desde estado guardado (§9.5): un ViewModel creado con un SavedStateHandle precargado
 * muestra esos campos. Simula lo que Android entrega tras recrear el proceso; no mata un proceso real.
 */
@RunWith(AndroidJUnit4::class)
class FormularioViewModelTest {

    private lateinit var baseDatos: AppDatabase
    private val modelos = mutableListOf<FormularioViewModel>()

    @Before
    fun abrir() = runBlocking {
        baseDatos = BaseDePrueba.enMemoria()
        InicializadorDatos(baseDatos).inicializar().esperarOk()
    }

    @After
    fun cerrar() = runBlocking {
        // Estas instancias no pertenecen a una Activity: la prueba debe terminar sus lecturas antes de cerrar Room.
        modelos.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        baseDatos.close()
    }

    @Test
    fun recupera_los_campos_desde_un_estado_guardado() {
        val guardado = SavedStateHandle(
            mapOf(
                FormularioViewModel.CLAVE_TIPO to "INGRESO",
                FormularioViewModel.CLAVE_MONTO to "12.5",
                FormularioViewModel.CLAVE_CATEGORIA to 9L,
                FormularioViewModel.CLAVE_FECHA to LocalDate.of(2026, 10, 20).toEpochDay(),
                FormularioViewModel.CLAVE_DESCRIPCION to "Pago",
            ),
        )
        val viewModel = crearViewModel(guardado)

        val estado = viewModel.estado.value
        assertEquals(TipoMovimiento.INGRESO, estado.tipo)
        assertEquals("12.5", estado.montoTexto)
        assertEquals(9L, estado.idCategoria)
        assertEquals(LocalDate.of(2026, 10, 20), estado.fecha)
        assertEquals("Pago", estado.descripcion)
    }

    private fun crearViewModel(guardado: SavedStateHandle, tasas: RepositorioTipoCambio? = null): FormularioViewModel {
        lateinit var viewModel: FormularioViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = FormularioViewModel(BaseDePrueba.servicioSobre(baseDatos), guardado, tipoCambio = tasas)
        }
        modelos.add(viewModel)
        return viewModel
    }

    // Room responde en otro hilo: se espera hasta 5 s a que el estado cumpla la condición.
    private fun esperar(viewModel: FormularioViewModel, condicion: (FormularioUiState) -> Boolean): FormularioUiState =
        runBlocking { withTimeout(5_000) { viewModel.estado.first(condicion) } }

    @Test
    fun en_edicion_carga_el_movimiento_y_guarda_con_editar() = runBlocking {
        val id = OperacionesMovimientoRoom(baseDatos).crear(BaseDePrueba.gasto(baseDatos, "30"), BaseDePrueba.hoy).esperarOk()
        val viewModel = crearViewModel(SavedStateHandle(mapOf(FormularioViewModel.CLAVE_ID_EN_EDICION to id)))

        val cargado = esperar(viewModel) { !it.cargandoMovimiento && it.montoTexto.isNotEmpty() }
        assertTrue(cargado.enEdicion)
        assertEquals("30.00", cargado.montoTexto)
        assertEquals(TipoMovimiento.GASTO, cargado.tipo)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel.onMontoCambiado("20")
            viewModel.guardar()
        }
        esperar(viewModel) { it.guardadoExitoso }

        // Se editó el mismo movimiento: sigue habiendo uno y el saldo pasa de −Q 30 a −Q 20.
        assertEquals(1, baseDatos.transaccionDao().contar())
        assertEquals(2_000L, checkNotNull(baseDatos.transaccionDao().obtenerPorId(id)).montoCentavos)
        assertEquals(-2_000L, BaseDePrueba.saldo(baseDatos))
    }

    @Test
    fun editar_un_id_que_ya_no_existe_avisa_y_no_inserta() = runBlocking {
        val viewModel = crearViewModel(SavedStateHandle(mapOf(FormularioViewModel.CLAVE_ID_EN_EDICION to 999L)))

        val estado = esperar(viewModel) { it.errores.movimientoNoEncontrado }
        assertTrue(estado.enEdicion)
        assertEquals(0, baseDatos.transaccionDao().contar())
    }

    @Test fun recrear_edicion_usd_relee_fotografia_sin_pisar_campos_ni_consultar_api() = runBlocking {
        check(BaseDePrueba.contexto.packageName.endsWith(".pruebas"))
        val tasa = TasaCambio(BigDecimal("7.64187"), BaseDePrueba.hoy, "Histórica de prueba")
        val datos = BaseDePrueba.gasto(baseDatos, "10").copy(moneda = Moneda.USD, tasa = tasa)
        val id = OperacionesMovimientoRoom(baseDatos).crear(datos, BaseDePrueba.hoy).esperarOk()
        var consultas = 0
        val tasas = RepositorioTipoCambio(CacheServicios(BaseDePrueba.contexto, "tasa-vm-recargo-prueba"),
            consultar = { consultas++; error("No debe consultar al editar") })
        val guardado = SavedStateHandle(mapOf(FormularioViewModel.CLAVE_ID_EN_EDICION to id))
        val original = crearViewModel(guardado, tasas)
        esperar(original) { !it.cargandoMovimiento && it.montoTexto.isNotEmpty() }
        val salario = BaseDePrueba.idCategoria(baseDatos, "SALARIO")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            original.onMontoCambiado("12")
            original.onDescripcionCambiada("Corregido")
            original.elegirTipo(TipoMovimiento.INGRESO)
            original.elegirCategoria(salario)
        }
        val recreado = crearViewModel(SavedStateHandle(guardado.keys().associateWith { guardado.get<Any?>(it) }), tasas)
        val estado = esperar(recreado) { !it.cargandoMovimiento }
        assertEquals("12", estado.montoTexto)
        assertEquals("Corregido", estado.descripcion)
        assertEquals(TipoMovimiento.INGRESO, estado.tipo)
        assertEquals(salario, estado.idCategoria)
        assertEquals(tasa, estado.tasaHistorica)
        assertEquals(BigDecimal("0.30"), estado.recargoHistorico)
        assertEquals(0, estado.recargoGtqPorUsd.signum())
        assertEquals(9170L, estado.conversionPrevia!!.esperarOk().totalCentavos)
        assertEquals(0, consultas)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { recreado.guardar() }
        esperar(recreado) { it.guardadoExitoso }
        assertEquals(9170L, BaseDePrueba.saldo(baseDatos))
        assertEquals(0, consultas)
    }

    @Test fun estado_guardado_anterior_a_v3_relee_recargo_cero_sin_borrar_edicion() = runBlocking {
        val tasa = TasaCambio(BigDecimal("7.64187"), BaseDePrueba.hoy, "Histórica de prueba")
        val categoria = BaseDePrueba.idCategoria(baseDatos, "ALIMENTACION")
        val id = baseDatos.transaccionDao().insertar(Gasto(0, categoria, 7642, "Anterior", BaseDePrueba.hoy,
            ConversionAplicada(1000, tasa)).aEntity(0))
        baseDatos.usuarioDao().actualizarSaldo(1, -7642)
        val guardado = SavedStateHandle(mapOf(
            FormularioViewModel.CLAVE_ID_EN_EDICION to id,
            "movimientoCargado" to true, "moneda" to "USD", "montoTexto" to "12",
            "descripcion" to "No sobrescribir", "idCategoria" to categoria, "tipo" to "GASTO",
        ))
        val modelo = crearViewModel(guardado)
        val estado = esperar(modelo) { !it.cargandoMovimiento }
        assertEquals("12", estado.montoTexto)
        assertEquals("No sobrescribir", estado.descripcion)
        assertEquals(0, estado.recargoGtqPorUsd.signum())
        assertEquals(9170L, estado.conversionPrevia!!.esperarOk().totalCentavos)
    }
}
