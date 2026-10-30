package gt.edu.umg.finanzas.app.datos.remoto.reflexion

import gt.edu.umg.finanzas.app.datos.local.Reflexion
import gt.edu.umg.finanzas.app.datos.remoto.ClienteHttp
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClienteDabar(private val http: ClienteHttp = ClienteHttp()) {
    suspend fun consultar(referencia: String): Reflexion = withContext(Dispatchers.IO) {
        // El servicio interpreta el nombre del libro en español para rv1909.
        val pasaje = URLEncoder.encode(referencia, "UTF-8").replace("+", "%20")
        LectorRespuestaReflexion.leer(http.get("https://dabar.io/$pasaje?translation=rv1909"), referencia)
    }
}
