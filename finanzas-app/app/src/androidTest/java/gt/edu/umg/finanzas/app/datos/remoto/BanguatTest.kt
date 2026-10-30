package gt.edu.umg.finanzas.app.datos.remoto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.remoto.tipocambio.LectorRespuestaBanguat
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BanguatTest {
    private fun respuestaReal() = InstrumentationRegistry.getInstrumentation().context.assets
        .open("banguat/tipo-cambio-dia-2026-10-29.xml").bufferedReader().use { it.readText() }

    @Test
    fun lee_la_respuesta_real_sin_perder_decimales() {
        val tasa = LectorRespuestaBanguat.leer(respuestaReal())
        assertEquals(BigDecimal("7.64382"), tasa.gtqPorUsd)
        assertEquals(LocalDate.of(2026, 10, 29), tasa.fechaReferencia)
        assertEquals("Referencia Banguat", tasa.fuente)
    }

    @Test
    fun rechaza_fecha_invalida_tasa_no_positiva_campos_ausentes_y_dtd() {
        val real = respuestaReal()
        for (invalida in listOf(
            real.replace("29/10/2026", "31/02/2026"),
            real.replace("7.64382", "0"),
            real.replace("<referencia>7.64382</referencia>", ""),
            real.replace("CambioDolar", "CambioDia"),
            "<!DOCTYPE x SYSTEM 'https://example.invalid/externo'>" + real,
        )) {
            assertThrows(Exception::class.java) { LectorRespuestaBanguat.leer(invalida) }
        }
    }
}
