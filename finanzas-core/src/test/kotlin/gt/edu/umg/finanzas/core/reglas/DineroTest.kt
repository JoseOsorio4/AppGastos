package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.esperarErrorNegocio
import gt.edu.umg.finanzas.core.esperarOk
import kotlin.test.Test
import kotlin.test.assertEquals

class DineroTest {

    @Test
    fun `acepta enteros y hasta dos decimales con punto`() {
        assertEquals(1200L, Dinero.desdeTexto("12").esperarOk())
        assertEquals(1250L, Dinero.desdeTexto("12.5").esperarOk())
        assertEquals(1250L, Dinero.desdeTexto("12.50").esperarOk())
        assertEquals(1L, Dinero.desdeTexto("0.01").esperarOk())
        assertEquals(1250L, Dinero.desdeTexto("  12.50 ").esperarOk())
    }

    @Test
    fun `caso f rechaza coma decimal tres decimales y cero`() {
        assertEquals("Usa punto para los decimales", Dinero.desdeTexto("12,50").esperarErrorNegocio().mensaje)
        assertEquals("Usa como máximo dos decimales", Dinero.desdeTexto("12.505").esperarErrorNegocio().mensaje)
        assertEquals("El monto debe ser mayor que cero", Dinero.desdeTexto("0").esperarErrorNegocio().mensaje)
        Dinero.desdeTexto("0.00").esperarErrorNegocio()
    }

    @Test
    fun `rechaza signo espacios internos y texto que no es un numero`() {
        listOf("", "   ", "-5", "+5", "1 000", "abc", "12.", ".5", "1e3", "١٢").forEach { texto ->
            Dinero.desdeTexto(texto).esperarErrorNegocio()
        }
    }

    @Test
    fun `respeta el maximo por movimiento`() {
        assertEquals(999_999_999L, Dinero.desdeTexto("9999999.99").esperarOk())
        assertEquals(
            "El monto máximo es 9,999,999.99",
            Dinero.desdeTexto("10000000").esperarErrorNegocio().mensaje,
        )
        // Un número enorme no desborda Long: se rechaza antes de convertirlo.
        Dinero.desdeTexto("99999999999999999999999").esperarErrorNegocio()
    }

    @Test
    fun `aTexto produce un texto que desdeTexto vuelve a aceptar`() {
        assertEquals("12.50", Dinero.aTexto(1_250))
        assertEquals("0.05", Dinero.aTexto(5))
        assertEquals("9999999.99", Dinero.aTexto(999_999_999))
        listOf(1L, 1_250L, 100_000L, 999_999_999L).forEach { centavos ->
            assertEquals(centavos, Dinero.desdeTexto(Dinero.aTexto(centavos)).esperarOk())
        }
    }

    @Test
    fun `formatea con simbolo coma de miles y signo`() {
        assertEquals("Q 1,000.00", Dinero.formatear(100_000))
        assertEquals("-Q 30.00", Dinero.formatear(-3_000))
        assertEquals("Q 0.00", Dinero.formatear(0))
        assertEquals("US$ 13.05", Dinero.formatear(1_305, Moneda.USD))
        assertEquals("-Q 92,233,720,368,547,758.08", Dinero.formatear(Long.MIN_VALUE))
    }
}
