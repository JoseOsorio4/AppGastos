package gt.edu.umg.finanzas.app.datos.remoto

import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.remoto.reflexion.LectorRespuestaReflexion

object RespuestaDabarGuardada {
    fun texto(referencia: String): String {
        val archivo = when (referencia) {
            "Salmos 23:1" -> "salmos"
            "Proverbios 16:3" -> "proverbios"
            "Filipenses 4:13" -> "filipenses"
            else -> error("No hay respuesta real guardada para $referencia")
        }
        return InstrumentationRegistry.getInstrumentation().context.assets
            .open("dabar/$archivo-2026-10-30.json").bufferedReader().use { it.readText() }
    }

    fun leer(referencia: String) = LectorRespuestaReflexion.leer(texto(referencia), referencia)
}
