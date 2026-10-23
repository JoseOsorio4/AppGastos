package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.Gasto
import gt.edu.umg.finanzas.core.dominio.Ingreso
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.esperarErrorNegocio
import gt.edu.umg.finanzas.core.esperarOk
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculoSaldoTest {

    private val fecha = LocalDate.of(2026, 10, 22)

    private fun ingreso(centavos: Long) = Ingreso(Transaccion.SIN_ID, 9, centavos, "Ingreso", fecha)
    private fun gasto(centavos: Long) = Gasto(Transaccion.SIN_ID, 1, centavos, "Gasto", fecha)

    @Test
    fun `el signo lo decide cada subclase`() {
        val movimientos: List<Transaccion> = listOf(ingreso(10_000), gasto(3_000))
        assertEquals(listOf(10_000L, -3_000L), movimientos.map { it.impactoEnSaldo() })
    }

    @Test
    fun `deltas de crear editar y eliminar`() {
        assertEquals(-3_000L, CalculoSaldo.deltaCrear(gasto(3_000)))
        assertEquals(1_000L, CalculoSaldo.deltaEditar(gasto(3_000), gasto(2_000)))
        assertEquals(3_000L, CalculoSaldo.deltaEliminar(gasto(3_000)))
        // Cambiar de tipo: deja de restar Q 300 y empieza a sumar Q 300.
        assertEquals(60_000L, CalculoSaldo.deltaEditar(gasto(30_000), ingreso(30_000)))
    }

    @Test
    fun `aplicar devuelve el saldo final`() {
        assertEquals(7_000L, CalculoSaldo.aplicar(10_000, -3_000).esperarOk())
        assertEquals(-3_000L, CalculoSaldo.aplicar(0, -3_000).esperarOk()) // el saldo puede quedar negativo
    }

    @Test
    fun `caso j aplicar detecta el desborde en ambos extremos`() {
        assertEquals(CalculoSaldo.MENSAJE_DESBORDE, CalculoSaldo.aplicar(Long.MAX_VALUE, 1).esperarErrorNegocio().mensaje)
        CalculoSaldo.aplicar(Long.MIN_VALUE, -1).esperarErrorNegocio()
    }

    @Test
    fun `saldoDesde recorre el historial desde cero`() {
        assertEquals(0L, CalculoSaldo.saldoDesde(emptySequence()).esperarOk())
        assertEquals(5_000L, CalculoSaldo.saldoDesde(sequenceOf(ingreso(10_000), gasto(3_000), gasto(2_000))).esperarOk())
    }

    @Test
    fun `una transaccion no admite montos fuera de rango ni descripcion vacia`() {
        assertFailsWith<IllegalArgumentException> { gasto(0) }
        assertFailsWith<IllegalArgumentException> { gasto(Transaccion.MONTO_MAXIMO_CENTAVOS + 1) }
        assertFailsWith<IllegalArgumentException> { Gasto(Transaccion.SIN_ID, 1, 100, " ", fecha) }
    }
}
