package gt.edu.umg.finanzas.app.datos.remoto.tipocambio

import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException

object LectorRespuestaBanguat {
    const val ESPACIO = "http://www.banguat.gob.gt/variables/ws/"
    private const val SOAP = "http://schemas.xmlsoap.org/soap/envelope/"
    private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun leer(xml: String): TasaCambio {
        // El contrato no usa DTD; se rechazan para no resolver contenido externo.
        require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true)) { "DTD no permitido" }
        val fabrica = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
        }
        val lector = fabrica.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> throw SAXException("Entidad externa no permitida") }
        }
        val raiz = lector.parse(InputSource(StringReader(xml))).documentElement
        require(raiz.localName == "Envelope" && raiz.namespaceURI == SOAP) { "No es un sobre SOAP 1.1" }
        val dolar = raiz.hijo("Body", SOAP)
            .hijo("TipoCambioDiaResponse").hijo("TipoCambioDiaResult")
            .hijo("CambioDolar").hijo("VarDolar")
        return TasaCambio(
            BigDecimal(dolar.hijo("referencia").textContent.trim()),
            LocalDate.parse(dolar.hijo("fecha").textContent.trim(), formatoFecha),
            "Referencia Banguat",
        )
    }

    private fun Element.hijo(nombre: String, espacio: String = ESPACIO): Element {
        val encontrados = (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }
            .filter { it.localName == nombre && it.namespaceURI == espacio }
        require(encontrados.size == 1) { "Falta o se repite $nombre" }
        return encontrados.single()
    }
}
