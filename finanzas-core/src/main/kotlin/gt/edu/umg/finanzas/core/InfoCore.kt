package gt.edu.umg.finanzas.core

import java.util.Properties

/** Permite comprobar desde Android qué versión del core contiene el JAR. */
object InfoCore {

    // Maven copia la versión del pom.xml a este recurso al construir el JAR.
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
