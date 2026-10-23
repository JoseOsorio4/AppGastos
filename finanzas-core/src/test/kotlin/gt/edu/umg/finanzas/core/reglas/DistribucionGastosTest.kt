package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.TotalCategoria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DistribucionGastosTest {

    private fun total(clave: String, centavos: Long) = TotalCategoria(clave, clave, centavos)

    @Test
    fun `caso h Q 30 y Q 20 dan 60 y 40 por ciento`() {
        val segmentos = DistribucionGastos.calcular(listOf(total("ALIMENTACION", 3_000), total("TRANSPORTE", 2_000)))
        assertEquals(listOf(6_000, 4_000), segmentos.map { it.puntosBasicos })
    }

    @Test
    fun `tres partes iguales suman exactamente 10000`() {
        val segmentos = DistribucionGastos.calcular(listOf(total("A", 100), total("B", 100), total("C", 100)))
        assertEquals(listOf(3_334, 3_333, 3_333), segmentos.map { it.puntosBasicos })
    }

    @Test
    fun `siempre suma 10000 con montos irregulares`() {
        val totales = listOf(1L, 2L, 3L, 5L, 8L, 13L, 21L).mapIndexed { i, c -> total("C$i", c) }
        assertEquals(DistribucionGastos.PUNTOS_TOTALES, DistribucionGastos.calcular(totales).sumOf { it.puntosBasicos })
    }

    @Test
    fun `ordena del mayor al menor gasto`() {
        val segmentos = DistribucionGastos.calcular(listOf(total("PEQUENO", 100), total("GRANDE", 900)))
        assertEquals(listOf("GRANDE", "PEQUENO"), segmentos.map { it.clave })
    }

    @Test
    fun `sin gastos no hay segmentos`() {
        assertTrue(DistribucionGastos.calcular(emptyList()).isEmpty())
        assertTrue(DistribucionGastos.calcular(listOf(total("A", 0))).isEmpty())
    }

    @Test
    fun `totales enormes no desbordan`() {
        val segmentos = DistribucionGastos.calcular(listOf(total("A", Long.MAX_VALUE / 2), total("B", Long.MAX_VALUE / 2)))
        assertEquals(listOf(5_000, 5_000), segmentos.map { it.puntosBasicos })
    }
}
