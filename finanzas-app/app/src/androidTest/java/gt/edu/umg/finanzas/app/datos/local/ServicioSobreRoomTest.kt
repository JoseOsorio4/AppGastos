package gt.edu.umg.finanzas.app.datos.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.gasto
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.hoy
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.ingreso
import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * El mismo recorrido que usarán las pantallas: ServicioFinanzas → contratos → implementaciones Room
 * → DAO → SQLite. Casos a y b de §14.1 y las consultas Flow de Inicio y Operaciones.
 */
@RunWith(AndroidJUnit4::class)
class ServicioSobreRoomTest {

    private val mes = Periodo.de(FiltroPeriodo.MES, hoy)
    private lateinit var baseDatos: AppDatabase
    private lateinit var servicio: ServicioFinanzas

    @Before
    fun abrir() = runBlocking {
        baseDatos = BaseDePrueba.enMemoria()
        InicializadorDatos(baseDatos).inicializar().esperarOk()
        servicio = BaseDePrueba.servicioSobre(baseDatos)
    }

    @After
    fun cerrar() = baseDatos.close()

    // El saldo se lee por el Flow del servicio, igual que lo hará Inicio.
    private suspend fun saldo(): Long = servicio.observarResumen(mes).first().saldoDisponibleCentavos

    @Test
    fun caso_a_registrar_editar_y_eliminar_un_gasto() = runBlocking {
        servicio.registrar(ingreso(baseDatos, "100"), hoy).esperarOk()
        val idGasto = servicio.registrar(gasto(baseDatos, "30"), hoy).esperarOk()
        assertEquals(7_000L, saldo())

        servicio.editar(idGasto, gasto(baseDatos, "20"), hoy).esperarOk()
        assertEquals(8_000L, saldo())

        servicio.eliminar(idGasto).esperarOk()
        assertEquals(10_000L, saldo())
    }

    @Test
    fun caso_b_cambio_de_tipo_y_eliminaciones_vuelven_a_cero() = runBlocking {
        val idIngresoGrande = servicio.registrar(ingreso(baseDatos, "1000"), hoy).esperarOk()
        val idMovimiento = servicio.registrar(gasto(baseDatos, "250"), hoy).esperarOk()
        assertEquals(75_000L, saldo())

        servicio.editar(idMovimiento, gasto(baseDatos, "300"), hoy).esperarOk()
        assertEquals(70_000L, saldo())

        servicio.editar(idMovimiento, ingreso(baseDatos, "300", clave = "REGALO"), hoy).esperarOk()
        assertEquals(130_000L, saldo())
        assertEquals(TipoMovimiento.INGRESO, checkNotNull(baseDatos.transaccionDao().obtenerPorId(idMovimiento)).tipo)

        servicio.eliminar(idIngresoGrande).esperarOk()
        assertEquals(30_000L, saldo())

        servicio.eliminar(idMovimiento).esperarOk()
        assertEquals(0L, saldo())
    }

    @Test
    fun totales_y_anillo_usan_el_periodo_y_el_saldo_es_historico() = runBlocking {
        servicio.registrar(ingreso(baseDatos, "100", fecha = LocalDate.of(2026, 10, 5)), hoy).esperarOk()
        servicio.registrar(gasto(baseDatos, "30", "ALIMENTACION", LocalDate.of(2026, 10, 10)), hoy).esperarOk()
        servicio.registrar(gasto(baseDatos, "20", "TRANSPORTE", LocalDate.of(2026, 10, 20)), hoy).esperarOk()
        servicio.registrar(gasto(baseDatos, "10", "SALUD", LocalDate.of(2026, 9, 15)), hoy).esperarOk()

        val resumen = servicio.observarResumen(mes).first()
        assertEquals(4_000L, resumen.saldoDisponibleCentavos)
        assertEquals(10_000L, resumen.ingresosCentavos)
        assertEquals(5_000L, resumen.gastosCentavos)

        val anillo = servicio.observarDistribucionGastos(mes).first()
        assertEquals(listOf("ALIMENTACION" to 6_000, "TRANSPORTE" to 4_000), anillo.map { it.clave to it.puntosBasicos })
    }

    @Test
    fun movimientos_filtrados_por_tipo_del_mas_reciente_al_mas_antiguo() = runBlocking {
        servicio.registrar(gasto(baseDatos, "5", fecha = LocalDate.of(2026, 10, 1)), hoy).esperarOk()
        servicio.registrar(ingreso(baseDatos, "50", fecha = LocalDate.of(2026, 10, 15)), hoy).esperarOk()
        servicio.registrar(gasto(baseDatos, "7", fecha = LocalDate.of(2026, 10, 20)), hoy).esperarOk()

        val gastos = servicio.observarMovimientos(mes, TipoMovimiento.GASTO).first()
        assertEquals(listOf(700L, 500L), gastos.map { it.montoCentavos })
        assertEquals("Alimentación", gastos.first().nombreCategoria)
        assertEquals(LocalDate.of(2026, 10, 20), gastos.first().fecha)
        assertEquals(3, servicio.observarMovimientos(mes, null).first().size)
        assertEquals(8, servicio.observarCategorias(TipoMovimiento.GASTO).first().size)
    }

    @Test
    fun el_flow_del_resumen_emite_de_nuevo_despues_de_guardar() = runBlocking {
        // El colector empieza antes de guardar: solo termina si Room vuelve a emitir con el gasto.
        val conGasto = async { withTimeout(5_000) { servicio.observarResumen(mes).first { it.gastosCentavos == 3_000L } } }
        servicio.registrar(gasto(baseDatos, "30"), hoy).esperarOk()

        assertEquals(-3_000L, conGasto.await().saldoDisponibleCentavos)
    }
}
