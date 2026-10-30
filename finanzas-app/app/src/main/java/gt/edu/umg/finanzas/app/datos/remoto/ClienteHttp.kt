package gt.edu.umg.finanzas.app.datos.remoto

import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClienteHttp {
    suspend fun post(url: String, cuerpo: String, accionSoap: String): String = withContext(Dispatchers.IO) {
        val direccion = URL(url)
        require(direccion.protocol == "https")
        val conexion = direccion.openConnection() as HttpsURLConnection
        try {
            conexion.connectTimeout = 10_000
            conexion.readTimeout = 10_000
            conexion.instanceFollowRedirects = false
            conexion.requestMethod = "POST"
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
            conexion.setRequestProperty("SOAPAction", "\"$accionSoap\"")
            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            check(conexion.responseCode in 200..299) { "HTTP ${conexion.responseCode}" }
            conexion.inputStream.use { entrada ->
                val salida = ByteArrayOutputStream()
                val bloque = ByteArray(4096)
                while (true) {
                    val leidos = entrada.read(bloque)
                    if (leidos == -1) break
                    check(salida.size() + leidos <= 256 * 1024) { "Respuesta demasiado grande" }
                    salida.write(bloque, 0, leidos)
                }
                salida.toString("UTF-8")
            }
        } finally {
            conexion.disconnect()
        }
    }
}
