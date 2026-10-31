package gt.edu.umg.finanzas.app.datos.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.local.BaseDePrueba.esperarOk
import gt.edu.umg.finanzas.core.dominio.*
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecargoUsdRoomTest {
    private val oficial = TasaCambio(BigDecimal("7.64187"), BaseDePrueba.hoy, "Referencia de prueba")

    @Test fun alta_edicion_y_eliminacion_aplican_un_solo_recargo_y_actualizan_saldo(): Unit = runBlocking {
        val base = BaseDePrueba.enMemoria()
        try {
            InicializadorDatos(base).inicializar().esperarOk()
            val servicio = BaseDePrueba.servicioSobre(base)
            servicio.registrar(BaseDePrueba.ingreso(base, "100"), BaseDePrueba.hoy).esperarOk()
            val datos = BaseDePrueba.gasto(base, "10").copy(moneda = Moneda.USD, tasa = oficial)
            val id = servicio.registrar(datos, BaseDePrueba.hoy).esperarOk()
            val foto = base.transaccionDao().obtenerPorId(id)!!
            assertEquals(7942L, foto.montoCentavos)
            assertEquals(1000L, foto.montoOriginalCentavos)
            assertEquals("7.64187", foto.tasaGtqPorUsd.toPlainString())
            assertEquals("0.30", foto.recargoGtqPorUsd.toPlainString())
            assertEquals("7.94187", foto.tasaAplicadaGtqPorUsd.toPlainString())
            assertEquals(oficial.fechaReferencia.toEpochDay(), foto.fechaTasaEpochDay)
            assertEquals(oficial.fuente, foto.fuenteTasa)
            assertEquals(2058L, BaseDePrueba.saldo(base))
            val filas = servicio.observarMovimientos(Periodo.de(FiltroPeriodo.MES, BaseDePrueba.hoy), null).first()
            assertEquals(foto.aDominio().conversion, filas.first { it.idTransaccion == id }.conversion)

            val editados = datos.copy(montoTexto = "12", tasa = oficial.copy(gtqPorUsd = BigDecimal("9.00000")))
            servicio.editar(id, editados, BaseDePrueba.hoy).esperarOk()
            assertEquals(9530L, base.transaccionDao().obtenerPorId(id)!!.montoCentavos)
            assertEquals(470L, BaseDePrueba.saldo(base))
            val guardado = base.transaccionDao().obtenerPorId(id)!!
            repeat(2) { servicio.editar(id, editados.copy(tasa = null), BaseDePrueba.hoy).esperarOk() }
            assertEquals(guardado, base.transaccionDao().obtenerPorId(id))
            assertEquals(2, base.transaccionDao().contar())
            assertEquals(470L, BaseDePrueba.saldo(base))
            servicio.eliminar(id).esperarOk()
            assertEquals(10000L, BaseDePrueba.saldo(base))
            assertEquals(1, base.transaccionDao().contar())
        } finally { base.close() }
    }

    @Test fun cambiar_tipo_y_moneda_no_duplica_recargo_ni_reutiliza_fotografia_descartada(): Unit = runBlocking {
        val base = BaseDePrueba.enMemoria()
        try {
            InicializadorDatos(base).inicializar().esperarOk()
            val servicio = BaseDePrueba.servicioSobre(base)
            val gasto = BaseDePrueba.gasto(base, "10").copy(moneda = Moneda.USD, tasa = oficial)
            val id = servicio.registrar(gasto, BaseDePrueba.hoy).esperarOk()
            val ingreso = BaseDePrueba.ingreso(base, "10").copy(moneda = Moneda.USD, tasa = null)
            servicio.editar(id, ingreso, BaseDePrueba.hoy).esperarOk()
            assertEquals(7642L, BaseDePrueba.saldo(base))
            assertEquals(0, base.transaccionDao().obtenerPorId(id)!!.recargoGtqPorUsd.signum())
            servicio.editar(id, gasto.copy(tasa = null), BaseDePrueba.hoy).esperarOk()
            assertEquals(-7942L, BaseDePrueba.saldo(base))
            servicio.editar(id, gasto.copy(moneda = Moneda.GTQ), BaseDePrueba.hoy).esperarOk()
            assertEquals(-1000L, BaseDePrueba.saldo(base))
            assertNull(base.transaccionDao().obtenerPorId(id)!!.aDominio().conversion)
            servicio.editar(id, gasto.copy(tasa = oficial.copy(gtqPorUsd = BigDecimal("9.00000"))), BaseDePrueba.hoy).esperarOk()
            assertEquals(-9300L, BaseDePrueba.saldo(base))
            assertEquals("9.30000", base.transaccionDao().obtenerPorId(id)!!.tasaAplicadaGtqPorUsd.toPlainString())
        } finally { base.close() }
    }

    @Test fun cerrar_y_reabrir_conserva_fotografia_y_editar_no_la_recarga(): Unit = runBlocking {
        val contexto = BaseDePrueba.contexto
        check(contexto.packageName.endsWith(".pruebas"))
        val nombre = "recargo-usd-persistencia-prueba.db"
        contexto.deleteDatabase(nombre)
        var base = AppDatabase.crear(contexto, nombre)
        try {
            InicializadorDatos(base).inicializar().esperarOk()
            val datos = BaseDePrueba.gasto(base, "10").copy(moneda = Moneda.USD, tasa = oficial)
            val id = BaseDePrueba.servicioSobre(base).registrar(datos, BaseDePrueba.hoy).esperarOk()
            val anterior = base.transaccionDao().obtenerPorId(id)!!
            base.close()
            base = AppDatabase.crear(contexto, nombre)
            assertEquals(anterior, base.transaccionDao().obtenerPorId(id))
            BaseDePrueba.servicioSobre(base).editar(id, datos.copy(tasa = null), BaseDePrueba.hoy).esperarOk()
            assertEquals(anterior, base.transaccionDao().obtenerPorId(id))
            assertEquals(-7942L, BaseDePrueba.saldo(base))
        } finally { base.close(); contexto.deleteDatabase(nombre) }
    }
}
