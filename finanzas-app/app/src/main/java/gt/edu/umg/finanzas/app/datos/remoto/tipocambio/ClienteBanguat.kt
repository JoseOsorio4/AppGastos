package gt.edu.umg.finanzas.app.datos.remoto.tipocambio

import gt.edu.umg.finanzas.app.datos.remoto.ClienteHttp
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClienteBanguat(private val http: ClienteHttp = ClienteHttp()) {
    suspend fun consultar(): TasaCambio = withContext(Dispatchers.IO) {
        LectorRespuestaBanguat.leer(
            http.post(
                "https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx",
                SOBRE,
                "${LectorRespuestaBanguat.ESPACIO}TipoCambioDia",
            ),
        )
    }

    private companion object {
        // SOAP 1.1 comprobado con el contrato oficial y un POST real el 29/10/2026.
        const val SOBRE = """<?xml version="1.0" encoding="utf-8"?><soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><TipoCambioDia xmlns="http://www.banguat.gob.gt/variables/ws/" /></soap:Body></soap:Envelope>"""
    }
}
