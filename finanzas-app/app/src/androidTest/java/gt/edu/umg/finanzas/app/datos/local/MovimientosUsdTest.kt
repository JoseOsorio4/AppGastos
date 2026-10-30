package gt.edu.umg.finanzas.app.datos.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovimientosUsdTest {
    private val historica = TasaCambio(BigDecimal("7.66000"), BaseDePrueba.hoy, "Ficticia de prueba")

    @Test
    fun caso_e_conserva_original_equivalente_y_tasa_historica_al_editar() = runBlocking {
        val base = BaseDePrueba.enMemoria()
        try {
            InicializadorDatos(base).inicializar().esperarOk()
            val servicio = BaseDePrueba.servicioSobre(base)
            val datos = BaseDePrueba.ingreso(base, "10").copy(moneda = Moneda.USD, tasa = historica)
            val id = servicio.registrar(datos, BaseDePrueba.hoy).esperarOk()
            val fila = checkNotNull(base.transaccionDao().obtenerPorId(id))
            assertEquals(7_660L, fila.montoCentavos)
            assertEquals(1_000L, fila.montoOriginalCentavos)
            assertEquals("7.66000", fila.tasaGtqPorUsd.toPlainString())
            assertEquals(historica, fila.aDominio().conversion?.tasa)
            val listado = servicio.observarMovimientos(Periodo.de(FiltroPeriodo.MES, BaseDePrueba.hoy), null).first()
            assertEquals(fila.aDominio().conversion, listado.single().conversion)

            // Una referencia recibida después no sustituye la fotografía del registro USD.
            val nueva = historica.copy(gtqPorUsd = BigDecimal("7.70000"))
            servicio.editar(id, datos.copy(tasa = nueva, descripcion = "Descripción corregida"), BaseDePrueba.hoy).esperarOk()
            assertEquals(fila.copy(descripcion = "Descripción corregida"), base.transaccionDao().obtenerPorId(id))
            assertEquals(7_660L, BaseDePrueba.saldo(base))
            servicio.editar(id, datos.copy(tasa = nueva, montoTexto = "12"), BaseDePrueba.hoy).esperarOk()
            assertEquals(9_192L, BaseDePrueba.saldo(base))
            assertEquals(historica, checkNotNull(base.transaccionDao().obtenerPorId(id)).aDominio().conversion?.tasa)
            servicio.eliminar(id).esperarOk()
            assertEquals(0L, BaseDePrueba.saldo(base))
        } finally { base.close() }
    }

    @Test
    fun pasar_de_usd_a_gtq_elimina_la_conversion_y_actualiza_el_saldo() = runBlocking {
        val base = BaseDePrueba.enMemoria()
        try {
            InicializadorDatos(base).inicializar().esperarOk()
            val servicio = BaseDePrueba.servicioSobre(base)
            val datos = BaseDePrueba.ingreso(base, "10")
            val id = servicio.registrar(datos.copy(moneda = Moneda.USD, tasa = historica), BaseDePrueba.hoy).esperarOk()
            servicio.editar(id, datos.copy(montoTexto = "25"), BaseDePrueba.hoy).esperarOk()
            val fila = checkNotNull(base.transaccionDao().obtenerPorId(id))
            assertNull(fila.aDominio().conversion)
            assertEquals(2_500L, fila.montoOriginalCentavos)
            assertEquals(BigDecimal.ONE, fila.tasaGtqPorUsd)
            assertEquals(2_500L, BaseDePrueba.saldo(base))
        } finally { base.close() }
    }
}
