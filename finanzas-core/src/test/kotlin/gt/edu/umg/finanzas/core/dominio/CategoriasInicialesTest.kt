package gt.edu.umg.finanzas.core.dominio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoriasInicialesTest {

    private val lista = CategoriasIniciales.lista

    @Test
    fun `son 12 categorias 8 de gasto y 4 de ingreso`() {
        assertEquals(12, lista.size)
        assertEquals(8, lista.count { it.tipo == TipoMovimiento.GASTO })
        assertEquals(4, lista.count { it.tipo == TipoMovimiento.INGRESO })
    }

    @Test
    fun `las claves son unicas y sin tildes`() {
        assertEquals(lista.size, lista.map { it.clave }.toSet().size)
        assertTrue(lista.all { it.clave.matches(Regex("[A-Z_]+")) })
    }

    @Test
    fun `cada tipo tiene su categoria de otros`() {
        assertEquals(TipoMovimiento.GASTO, lista.single { it.clave == "OTROS_GASTOS" }.tipo)
        assertEquals(TipoMovimiento.INGRESO, lista.single { it.clave == "OTROS_INGRESOS" }.tipo)
    }
}
