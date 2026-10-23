package gt.edu.umg.finanzas.core.servicio

import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.FiltroPeriodo
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.esperarErrorNegocio
import gt.edu.umg.finanzas.core.esperarOk
import gt.edu.umg.finanzas.core.fakes.AlmacenEnMemoria
import gt.edu.umg.finanzas.core.reglas.CalculoSaldo
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Recorridos completos con los dobles en memoria: servicio → operaciones → reglas → almacén. */
class ServicioFinanzasTest {

    private val hoy = LocalDate.of(2026, 10, 22)
    private val mes = Periodo.de(FiltroPeriodo.MES, hoy)
    private val almacen = AlmacenEnMemoria.conCategoriasIniciales()
    private val servicio = almacen.crearServicio()

    private fun datos(
        tipo: TipoMovimiento,
        monto: String,
        clave: String,
        fecha: LocalDate = hoy,
    ) = DatosMovimiento(tipo, monto, Moneda.GTQ, null, almacen.idCategoria(clave), "Movimiento de prueba", fecha)

    private fun ingreso(monto: String, clave: String = "SALARIO", fecha: LocalDate = hoy) =
        datos(TipoMovimiento.INGRESO, monto, clave, fecha)

    private fun gasto(monto: String, clave: String = "ALIMENTACION", fecha: LocalDate = hoy) =
        datos(TipoMovimiento.GASTO, monto, clave, fecha)

    private suspend fun saldo(): Long = servicio.observarResumen(mes).first().saldoDisponibleCentavos

    @Test
    fun `caso a registrar editar y eliminar un gasto`() = runTest {
        servicio.registrar(ingreso("100"), hoy).esperarOk()
        val idGasto = servicio.registrar(gasto("30"), hoy).esperarOk()
        assertEquals(7_000L, saldo())

        servicio.editar(idGasto, gasto("20"), hoy).esperarOk()
        assertEquals(8_000L, saldo())

        servicio.eliminar(idGasto).esperarOk()
        assertEquals(10_000L, saldo())
    }

    @Test
    fun `caso b cambio de tipo y eliminaciones vuelven a cero`() = runTest {
        val idIngresoGrande = servicio.registrar(ingreso("1000"), hoy).esperarOk()
        val idMovimiento = servicio.registrar(gasto("250"), hoy).esperarOk()
        assertEquals(75_000L, saldo())

        servicio.editar(idMovimiento, gasto("300"), hoy).esperarOk()
        assertEquals(70_000L, saldo())

        servicio.editar(idMovimiento, ingreso("300", clave = "REGALO"), hoy).esperarOk()
        assertEquals(130_000L, saldo())

        servicio.eliminar(idIngresoGrande).esperarOk()
        assertEquals(30_000L, saldo())

        servicio.eliminar(idMovimiento).esperarOk()
        assertEquals(0L, saldo())
    }

    @Test
    fun `resumen y anillo usan solo el periodo pero el saldo es historico`() = runTest {
        servicio.registrar(ingreso("100", fecha = LocalDate.of(2026, 10, 5)), hoy).esperarOk()
        servicio.registrar(gasto("30", "ALIMENTACION", LocalDate.of(2026, 10, 10)), hoy).esperarOk()
        servicio.registrar(gasto("20", "TRANSPORTE", LocalDate.of(2026, 10, 20)), hoy).esperarOk()
        servicio.registrar(gasto("10", "SALUD", LocalDate.of(2026, 9, 15)), hoy).esperarOk() // fuera del mes

        val resumen = servicio.observarResumen(mes).first()
        assertEquals(4_000L, resumen.saldoDisponibleCentavos)
        assertEquals(10_000L, resumen.ingresosCentavos)
        assertEquals(5_000L, resumen.gastosCentavos)
        assertEquals(5_000L, resumen.resultadoCentavos)

        val anillo = servicio.observarDistribucionGastos(mes).first()
        assertEquals(listOf("ALIMENTACION" to 6_000, "TRANSPORTE" to 4_000), anillo.map { it.clave to it.puntosBasicos })
    }

    @Test
    fun `movimientos se filtran por tipo y van del mas reciente al mas antiguo`() = runTest {
        servicio.registrar(gasto("5", fecha = LocalDate.of(2026, 10, 1)), hoy).esperarOk()
        servicio.registrar(ingreso("50", fecha = LocalDate.of(2026, 10, 15)), hoy).esperarOk()
        servicio.registrar(gasto("7", fecha = LocalDate.of(2026, 10, 20)), hoy).esperarOk()

        val gastos = servicio.observarMovimientos(mes, TipoMovimiento.GASTO).first()
        assertEquals(listOf(700L, 500L), gastos.map { it.montoCentavos })
        assertEquals("Alimentación", gastos.first().nombreCategoria)
        assertEquals(3, servicio.observarMovimientos(mes, null).first().size)
    }

    @Test
    fun `solo se ofrecen categorias activas del tipo pedido`() = runTest {
        val categoriasGasto = servicio.observarCategorias(TipoMovimiento.GASTO).first()
        assertEquals(8, categoriasGasto.size)
        assertFalse(categoriasGasto.any { it.tipo == TipoMovimiento.INGRESO })
    }

    @Test
    fun `un dato invalido no escribe nada`() = runTest {
        val error = servicio.registrar(gasto("12,50"), hoy).esperarErrorNegocio()
        assertEquals(CampoMovimiento.MONTO, error.campo)
        assertEquals(0, almacen.cantidadMovimientos)
        assertEquals(0L, almacen.saldo)
    }

    @Test
    fun `caso j un desborde del saldo no escribe el movimiento`() = runTest {
        val almacenAlLimite = AlmacenEnMemoria.conCategoriasIniciales(saldoInicialCentavos = Long.MAX_VALUE - 50)
        val servicioAlLimite = almacenAlLimite.crearServicio()
        val datos = DatosMovimiento(
            TipoMovimiento.INGRESO, "1.00", Moneda.GTQ, null, almacenAlLimite.idCategoria("SALARIO"), "Ingreso", hoy,
        )

        val error = servicioAlLimite.registrar(datos, hoy).esperarErrorNegocio()

        assertEquals(CalculoSaldo.MENSAJE_DESBORDE, error.mensaje)
        assertEquals(0, almacenAlLimite.cantidadMovimientos)
        assertEquals(Long.MAX_VALUE - 50, almacenAlLimite.saldo)
    }

    @Test
    fun `editar eliminar u obtener un ID inexistente da NoEncontrado sin insertar`() = runTest {
        assertEquals(Resultado.NoEncontrado, servicio.editar(99, gasto("10"), hoy))
        assertEquals(Resultado.NoEncontrado, servicio.eliminar(99))
        assertEquals(Resultado.NoEncontrado, servicio.obtenerParaEditar(99))
        assertEquals(0, almacen.cantidadMovimientos)
    }

    @Test
    fun `una categoria inexistente se rechaza`() = runTest {
        val datos = gasto("10").copy(idCategoria = 999)
        assertEquals(CampoMovimiento.CATEGORIA, servicio.registrar(datos, hoy).esperarErrorNegocio().campo)
    }

    @Test
    fun `la reconstruccion asigna el saldo calculado apaga la bandera y es repetible`() = runTest {
        servicio.registrar(ingreso("100"), hoy).esperarOk()
        servicio.registrar(gasto("30"), hoy).esperarOk()
        // Saldo erróneo con la bandera encendida, como tras una migración fallida.
        almacen.estado.value = almacen.estado.value.let {
            it.copy(usuario = it.usuario.copy(saldoCentavos = 123, reconstruccionPendiente = true))
        }

        assertEquals(7_000L, servicio.reconstruirSaldo().esperarOk())
        assertFalse(almacen.estado.value.usuario.reconstruccionPendiente)
        assertEquals(7_000L, servicio.reconstruirSaldo().esperarOk())
        assertEquals(7_000L, almacen.saldo)
    }

    @Test
    fun `sin bandera la reconstruccion no toca el saldo`() = runTest {
        servicio.registrar(ingreso("100"), hoy).esperarOk()
        almacen.estado.value = almacen.estado.value.let { it.copy(usuario = it.usuario.copy(saldoCentavos = 0)) }

        assertEquals(0L, servicio.reconstruirSaldo().esperarOk())
        assertEquals(0L, almacen.saldo)
    }
}
