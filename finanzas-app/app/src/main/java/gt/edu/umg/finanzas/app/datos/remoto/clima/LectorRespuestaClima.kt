package gt.edu.umg.finanzas.app.datos.remoto.clima

import java.time.LocalDateTime
import org.json.JSONObject

object LectorRespuestaClima {
    fun leer(respuesta: String): ClimaJutiapa {
        val raiz = JSONObject(respuesta)
        require(raiz.getString("timezone") == "America/Guatemala")
        val unidades = raiz.getJSONObject("current_units")
        require(unidades.getString("temperature_2m") == "°C")
        require(unidades.getString("weather_code") == "wmo code")
        require(unidades.getString("time") == "iso8601")
        val actual = raiz.getJSONObject("current")
        val temperatura = (actual.get("temperature_2m") as Number).toDouble()
        require(temperatura.isFinite())
        val codigo = actual.get("weather_code")
        require(codigo is Int)
        return ClimaJutiapa(temperatura, codigo, LocalDateTime.parse(actual.getString("time")))
    }
}
