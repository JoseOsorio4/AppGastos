package gt.edu.umg.finanzas.app.datos.remoto.clima

import gt.edu.umg.finanzas.app.datos.remoto.ClienteHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClienteOpenMeteo(private val http: ClienteHttp = ClienteHttp()) {
    suspend fun consultar(): ClimaJutiapa = withContext(Dispatchers.IO) {
        LectorRespuestaClima.leer(http.get(URL_CLIMA))
    }

    companion object {
        // Cabecera municipal confirmada con el geocodificador; no se consulta la ubicación del teléfono.
        private const val URL_CLIMA = "https://api.open-meteo.com/v1/forecast?latitude=14.28491&longitude=-89.88798&current=temperature_2m,weather_code&timezone=America%2FGuatemala"
    }
}
