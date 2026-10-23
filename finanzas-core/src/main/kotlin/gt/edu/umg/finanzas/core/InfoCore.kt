package gt.edu.umg.finanzas.core

import java.util.Properties

/**
 * Diagnóstico del core: permite que la app muestre qué versión del JAR quedó empaquetada.
 *
 * La versión no se escribe a mano: Maven la copia desde pom.xml a finanzas-core.properties
 * al construir. Si Android mostrara una versión distinta de la esperada, estaría usando otro JAR.
 */
object InfoCore {

    private const val RECURSO = "/gt/edu/umg/finanzas/core/finanzas-core.properties"
    private const val SIN_VERSION = "desconocida"

    val version: String by lazy { leerVersion() }

    fun mensajeConexion(): String =
        "finanzas-core $version conectado: este texto lo generó el JAR del core."

    // "desconocida" indica que el recurso no viajó dentro del JAR o del APK.
    private fun leerVersion(): String {
        val entrada = InfoCore::class.java.getResourceAsStream(RECURSO) ?: return SIN_VERSION
        return entrada.use { flujo ->
            Properties().apply { load(flujo) }.getProperty("version") ?: SIN_VERSION
        }
    }
}
