package gt.edu.umg.finanzas.core.dominio

import gt.edu.umg.finanzas.core.esperarErrorNegocio
import gt.edu.umg.finanzas.core.esperarOk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeriodoTest {

    private fun fecha(dia: Int, mes: Int = 10, anio: Int = 2026) = LocalDate.of(anio, mes, dia)

    // Jueves 22/10/2026, tabla de §3.2 del diseño.
    private val hoy = fecha(22)

    @Test
    fun `caso h intervalos del jueves 22 de octubre de 2026`() {
        assertEquals(Periodo(fecha(22), fecha(22)), Periodo.de(FiltroPeriodo.DIA, hoy))
        assertEquals(Periodo(fecha(18), fecha(24)), Periodo.de(FiltroPeriodo.SEMANA, hoy))
        assertEquals(Periodo(fecha(1), fecha(31)), Periodo.de(FiltroPeriodo.MES, hoy))
        assertEquals(Periodo(fecha(1, 1), fecha(31, 12)), Periodo.de(FiltroPeriodo.ANIO, hoy))
    }

    @Test
    fun `la semana va de domingo a sabado`() {
        assertEquals(Periodo(fecha(18), fecha(24)), Periodo.de(FiltroPeriodo.SEMANA, fecha(18))) // domingo
        assertEquals(Periodo(fecha(18), fecha(24)), Periodo.de(FiltroPeriodo.SEMANA, fecha(24))) // sábado
        assertEquals(Periodo(fecha(25), fecha(31)), Periodo.de(FiltroPeriodo.SEMANA, fecha(25))) // domingo siguiente
    }

    @Test
    fun `el mes de febrero bisiesto termina el 29`() {
        assertEquals(Periodo(fecha(1, 2, 2028), fecha(29, 2, 2028)), Periodo.de(FiltroPeriodo.MES, fecha(10, 2, 2028)))
    }

    @Test
    fun `contiene incluye ambos extremos`() {
        val semana = Periodo.de(FiltroPeriodo.SEMANA, hoy)
        assertTrue(semana.contiene(fecha(18)))
        assertTrue(semana.contiene(fecha(24)))
        assertFalse(semana.contiene(fecha(17)))
        assertFalse(semana.contiene(fecha(25)))
    }

    @Test
    fun `personalizado rechaza un rango invertido`() {
        Periodo.personalizado(fecha(24), fecha(18)).esperarErrorNegocio()
        assertEquals(Periodo(fecha(18), fecha(18)), Periodo.personalizado(fecha(18), fecha(18)).esperarOk())
    }

    @Test
    fun `hoy se calcula en la zona de Guatemala`() {
        // 03:00 UTC del 23/10 todavía es 21:00 del 22/10 en Guatemala (UTC−6).
        val reloj = Clock.fixed(Instant.parse("2026-10-23T03:00:00Z"), ZoneOffset.UTC)
        assertEquals(fecha(22), Periodo.hoy(reloj))
    }
}
