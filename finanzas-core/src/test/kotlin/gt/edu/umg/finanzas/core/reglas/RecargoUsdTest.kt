package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.*
import gt.edu.umg.finanzas.core.esperarErrorNegocio
import gt.edu.umg.finanzas.core.esperarOk
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*

class RecargoUsdTest {
    private val hoy = LocalDate.of(2026, 10, 31)
    private val oficial = TasaCambio(BigDecimal("7.64187"), hoy, "Referencia de prueba")
    private val gasto = Categoria(1, "ALIMENTACION", "Alimentación", TipoMovimiento.GASTO)
    private val ingreso = Categoria(9, "SALARIO", "Salario", TipoMovimiento.INGRESO)

    private fun datos(tipo: TipoMovimiento = TipoMovimiento.GASTO, monto: String = "10.00") =
        DatosMovimiento(tipo, monto, Moneda.USD, oficial,
            if (tipo == TipoMovimiento.GASTO) gasto.idCategoria else ingreso.idCategoria, "Prueba USD", hoy)

    @Test fun `gasto de diez USD suma treinta centavos por USD a la tasa oficial`() {
        val movimiento = ValidadorMovimiento.validar(datos(), gasto, hoy).esperarOk()
        val foto = checkNotNull(movimiento.conversion)
        assertEquals(7942L, movimiento.montoCentavos)
        assertEquals(-7942L, movimiento.impactoEnSaldo())
        assertEquals(1000L, foto.montoOriginalCentavos)
        assertEquals(oficial, foto.tasa)
        assertEquals(BigDecimal("0.30"), foto.recargoGtqPorUsd)
        assertEquals(BigDecimal("7.94187"), ConversorMoneda.tasaAplicada(foto.tasa.gtqPorUsd, foto.recargoGtqPorUsd))
        assertEquals(DesgloseConversion(7642, 300, 7942), ConversorMoneda.desglosar(foto))
    }

    @Test fun `ingreso USD y conversor general conservan la referencia sin recargo`() {
        val movimiento = ValidadorMovimiento.validar(datos(TipoMovimiento.INGRESO), ingreso, hoy).esperarOk()
        assertEquals(7642L, movimiento.montoCentavos)
        assertEquals(BigDecimal.ZERO, movimiento.conversion!!.recargoGtqPorUsd)
        assertEquals(7642L, ConversorMoneda.usdAGtq(1000, oficial))
        assertEquals(1309L, ConversorMoneda.gtqAUsd(10000, oficial))
    }

    @Test fun `GTQ no conserva tasa ni recargo aunque reciba una referencia`() {
        val movimiento = ValidadorMovimiento.validar(datos().copy(moneda = Moneda.GTQ), gasto, hoy).esperarOk()
        assertEquals(1000L, movimiento.montoCentavos)
        assertNull(movimiento.conversion)
    }

    @Test fun `montos pequenos redondean el total una sola vez y el desglose suma ese total`() {
        val esperados = mapOf("0.01" to 8L, "0.02" to 16L, "0.05" to 40L, "0.07" to 56L)
        for ((monto, centavos) in esperados) {
            val movimiento = ValidadorMovimiento.validar(datos(monto = monto), gasto, hoy).esperarOk()
            assertEquals(centavos, movimiento.montoCentavos)
            val desglose = ConversorMoneda.desglosar(movimiento.conversion!!)
            assertEquals(centavos, desglose.referenciaCentavos + desglose.recargoCentavos)
        }
        val foto = ValidadorMovimiento.validar(datos(monto = "0.07"), gasto, hoy).esperarOk().conversion!!
        // 0.5349309 + 0.021 = 0.5559309; las partes se presentan como 0.53 + 0.03 = 0.56.
        assertEquals(DesgloseConversion(53, 3, 56), ConversorMoneda.desglosar(foto))
    }

    @Test fun `editar gasto conserva fotografia sin recargar ni exigir tasa actual`() {
        val anterior = ValidadorMovimiento.validar(datos(), gasto, hoy).esperarOk()
        val editado = ValidadorMovimiento.validar(datos(monto = "12.00").copy(tasa = null), gasto, hoy, anterior).esperarOk()
        assertEquals(9530L, editado.montoCentavos)
        assertEquals(anterior.conversion!!.tasa, editado.conversion!!.tasa)
        assertEquals(BigDecimal("0.30"), editado.conversion!!.recargoGtqPorUsd)
    }

    @Test fun `gasto anterior sin recargo conserva cero incluso con referencia futura`() {
        val anterior = Gasto(12, gasto.idCategoria, 7642, "Anterior", hoy, ConversionAplicada(1000, oficial))
        val editado = ValidadorMovimiento.validar(datos().copy(tasa = oficial.copy(gtqPorUsd = BigDecimal("9.0"))),
            gasto, hoy, anterior).esperarOk()
        assertEquals(7642L, editado.montoCentavos)
        assertEquals(anterior.conversion, editado.conversion)
    }

    @Test fun `cambiar tipo conserva referencia historica y selecciona una sola vez el recargo del nuevo tipo`() {
        val anterior = ValidadorMovimiento.validar(datos(), gasto, hoy).esperarOk()
        val nuevoIngreso = ValidadorMovimiento.validar(datos(TipoMovimiento.INGRESO).copy(tasa = null), ingreso, hoy, anterior).esperarOk()
        assertEquals(7642L, nuevoIngreso.montoCentavos)
        assertEquals(BigDecimal.ZERO, nuevoIngreso.conversion!!.recargoGtqPorUsd)
        val nuevoGasto = ValidadorMovimiento.validar(datos().copy(tasa = null), gasto, hoy, nuevoIngreso).esperarOk()
        assertEquals(7942L, nuevoGasto.montoCentavos)
        assertEquals(oficial, nuevoGasto.conversion!!.tasa)
        val gtq = ValidadorMovimiento.validar(datos().copy(moneda = Moneda.GTQ), gasto, hoy, nuevoGasto).esperarOk()
        assertNull(gtq.conversion)
        assertEquals(1000L, gtq.montoCentavos)
    }

    @Test fun `el limite monetario comprueba tambien el recargo`() {
        ValidadorMovimiento.validar(datos(TipoMovimiento.INGRESO, "1300000.00"), ingreso, hoy).esperarOk()
        val error = ValidadorMovimiento.validar(datos(monto = "1300000.00"), gasto, hoy).esperarErrorNegocio()
        assertEquals(CampoMovimiento.MONTO, error.campo)
    }
}
