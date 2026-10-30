package gt.edu.umg.finanzas.app.datos.remoto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.remoto.clima.LectorRespuestaClima
import java.time.LocalDateTime
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClimaTest {
    private fun respuesta() = InstrumentationRegistry.getInstrumentation().context.assets
        .open("openmeteo/clima-2026-10-30.json").bufferedReader().use { it.readText() }

    @Test
    fun lee_la_respuesta_real_guardada_con_unidades_y_hora_local() {
        val dato = LectorRespuestaClima.leer(respuesta())
        assertEquals(18.3, dato.temperaturaC, 0.0)
        assertEquals(0, dato.codigoWmo)
        assertEquals(LocalDateTime.of(2026, 10, 30, 0, 45), dato.medidoEn)
    }

    @Test
    fun rechaza_campos_ausentes_nulos_y_unidades_incompatibles() {
        val incompleta = JSONObject(respuesta()).apply { getJSONObject("current").remove("temperature_2m") }
        val nula = JSONObject(respuesta()).apply { getJSONObject("current").put("temperature_2m", JSONObject.NULL) }
        val fahrenheit = JSONObject(respuesta()).apply { getJSONObject("current_units").put("temperature_2m", "°F") }
        val zonaIncorrecta = JSONObject(respuesta()).apply { put("timezone", "GMT") }
        val codigoFraccion = JSONObject(respuesta()).apply { getJSONObject("current").put("weather_code", 0.5) }
        for (dato in listOf(incompleta, nula, fahrenheit, zonaIncorrecta, codigoFraccion)) {
            assertThrows(Exception::class.java) { LectorRespuestaClima.leer(dato.toString()) }
        }
    }
}
