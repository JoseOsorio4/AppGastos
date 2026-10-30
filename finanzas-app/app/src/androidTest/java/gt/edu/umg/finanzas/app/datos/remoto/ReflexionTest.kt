package gt.edu.umg.finanzas.app.datos.remoto

import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.datos.remoto.reflexion.LectorRespuestaReflexion
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReflexionTest {
    @Test
    fun lee_las_tres_respuestas_reales_con_referencia_traduccion_y_fuente() {
        val referencias = listOf("Salmos 23:1", "Proverbios 16:3", "Filipenses 4:13")
        for (referencia in referencias) {
            val dato = RespuestaDabarGuardada.leer(referencia)
            assertEquals(referencia, dato.referencia)
            assertEquals("Reina Valera 1909", dato.traduccion)
            assertEquals("Dabar.io", dato.fuente)
            assertFalse(dato.texto.startsWith("\n") || dato.texto.endsWith("\n"))
        }
        assertEquals("Todo lo puedo en Cristo que me fortalece.", RespuestaDabarGuardada.leer("Filipenses 4:13").texto)
    }

    @Test
    fun rechaza_otra_traduccion_referencia_licencia_o_mas_de_un_versiculo() {
        fun respuesta() = JSONObject(RespuestaDabarGuardada.texto("Filipenses 4:13"))
        val distintas = listOf(
            respuesta().put("translation_id", "rv1960"),
            respuesta().put("translation_name", "Reina Valera 1960"),
            respuesta().put("translation_note", "Copyright"),
            respuesta().put("reference", "Filipenses 4:14"),
            respuesta().apply { getJSONArray("verses").getJSONObject(0).put("verse", 14) },
            respuesta().apply { getJSONArray("verses").getJSONObject(0).put("chapter", 4.5) },
            respuesta().apply { getJSONArray("verses").put(getJSONArray("verses").getJSONObject(0)) },
            respuesta().put("verses", JSONArray()),
        )
        for (invalida in distintas) assertThrows(Exception::class.java) {
            LectorRespuestaReflexion.leer(invalida.toString(), "Filipenses 4:13")
        }
    }

    @Test
    fun rechaza_textos_nulos_ausentes_vacios_html_y_respuestas_incoherentes() {
        fun respuesta() = JSONObject(RespuestaDabarGuardada.texto("Filipenses 4:13"))
        for (texto in listOf(JSONObject.NULL, "", "<b>Texto</b>", "a".repeat(301))) {
            val invalida = respuesta().put("text", texto).apply { getJSONArray("verses").getJSONObject(0).put("text", texto) }
            assertThrows(Exception::class.java) { LectorRespuestaReflexion.leer(invalida.toString(), "Filipenses 4:13") }
        }
        val ausente = respuesta().apply { getJSONArray("verses").getJSONObject(0).remove("text") }
        val distinta = respuesta().put("text", "Otro texto")
        for (invalida in listOf(ausente, distinta)) assertThrows(Exception::class.java) {
            LectorRespuestaReflexion.leer(invalida.toString(), "Filipenses 4:13")
        }
    }
}
