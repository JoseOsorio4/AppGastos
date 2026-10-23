package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConversorMonedaTest {

    // Tasa ficticia del diseño (§7.2).
    private val tasa = TasaCambio(BigDecimal("7.66285"), LocalDate.of(2026, 10, 21), "Prueba")

    @Test
    fun `caso i solo existen GTQ y USD`() {
        assertEquals(listOf(Moneda.GTQ, Moneda.USD), Moneda.entries.toList())
    }

    @Test
    fun `caso g USD a GTQ redondea una vez al final`() {
        assertEquals(9_456L, ConversorMoneda.usdAGtq(1_234, tasa)) // US$ 12.34 → Q 94.56
    }

    @Test
    fun `caso g GTQ a USD redondea una vez al final`() {
        assertEquals(1_305L, ConversorMoneda.gtqAUsd(10_000, tasa)) // Q 100.00 → US$ 13.05
    }

    @Test
    fun `HALF_UP sube el medio centavo en ambos sentidos`() {
        val fecha = LocalDate.of(2026, 10, 21)
        assertEquals(3L, ConversorMoneda.usdAGtq(1, TasaCambio(BigDecimal("2.5"), fecha, "Prueba"))) // 1 × 2.5 = 2.5 → 3
        assertEquals(3L, ConversorMoneda.gtqAUsd(1, TasaCambio(BigDecimal("0.4"), fecha, "Prueba"))) // 1 ÷ 0.4 = 2.5 → 3
    }

    @Test
    fun `ida y vuelta conserva el monto con la tasa del diseno`() {
        val usd = ConversorMoneda.gtqAUsd(10_000, tasa) // Q 100.00 → US$ 13.05
        assertEquals(10_000L, ConversorMoneda.usdAGtq(usd, tasa)) // 1305 × 7.66285 = 10000.019 → Q 100.00
    }

    @Test
    fun `la tasa debe ser positiva y tener fuente`() {
        assertFailsWith<IllegalArgumentException> { TasaCambio(BigDecimal.ZERO, LocalDate.of(2026, 10, 21), "Prueba") }
        assertFailsWith<IllegalArgumentException> { TasaCambio(BigDecimal("-7.6"), LocalDate.of(2026, 10, 21), "Prueba") }
        assertFailsWith<IllegalArgumentException> { TasaCambio(BigDecimal("7.6"), LocalDate.of(2026, 10, 21), " ") }
    }
}
