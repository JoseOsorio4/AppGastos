package gt.edu.umg.finanzas.app.datos.remoto.reflexion

import gt.edu.umg.finanzas.app.datos.local.Reflexion
import org.json.JSONObject

object LectorRespuestaReflexion {
    fun leer(respuesta: String, referenciaEsperada: String): Reflexion {
        val raiz = JSONObject(respuesta)
        require(raiz.getString("translation_id") == "rv1909")
        require(raiz.getString("translation_name") == "Reina Valera 1909")
        require(raiz.getString("translation_note") == "Public Domain")
        require(raiz.getString("reference") == referenciaEsperada)
        val versiculos = raiz.getJSONArray("verses")
        require(versiculos.length() == 1)
        val versiculo = versiculos.getJSONObject(0)
        val capitulo = versiculo.get("chapter")
        val numero = versiculo.get("verse")
        require(capitulo is Int && capitulo > 0 && numero is Int && numero > 0)
        require("${versiculo.getString("book_name")} $capitulo:$numero" == referenciaEsperada)
        val textoVersiculo = versiculo.get("text")
        val textoPasaje = raiz.get("text")
        require(textoVersiculo is String && textoPasaje is String)
        val texto = textoVersiculo.trim()
        require(texto.length in 1..300 && '<' !in texto && '>' !in texto)
        require(textoPasaje.trim() == texto)
        return Reflexion(texto, referenciaEsperada, raiz.getString("translation_name"), "Dabar.io")
    }
}
