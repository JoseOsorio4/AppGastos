package gt.edu.umg.finanzas.app.datos.preferencias

import android.util.AtomicFile
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.json.JSONObject

class BloqueoLocal(directorio: File) {
    private companion object { const val NOMBRE_HEREDADO = "Principal" }

    private val archivo = AtomicFile(File(directorio, "acceso.json"))
    private var sesion = false
    private var generacion = 0L
    private fun leer(): JSONObject = if (archivo.baseFile.exists())
        archivo.openRead().bufferedReader().use { JSONObject(it.readText()) } else JSONObject()
    private fun guardar(dato: JSONObject) {
        val salida = archivo.startWrite()
        try {
            val bytes = dato.toString().toByteArray(Charsets.UTF_8)
            salida.write(bytes); archivo.finishWrite(salida)
            check(archivo.openRead().use { it.readBytes() }.contentEquals(bytes)) { "No se pudo guardar el acceso. Reintenta." }
        }
        catch (error: Exception) { archivo.failWrite(salida); throw error }
    }
    @Synchronized fun preparar(nombrePerfilAnterior: String?) {
        if (!archivo.baseFile.exists()) guardar(JSONObject()
            .put("bienvenida", nombrePerfilAnterior != null && nombrePerfilAnterior != NOMBRE_HEREDADO)
            .put("personalizacionOpcional", nombrePerfilAnterior == NOMBRE_HEREDADO))
    }
    @Synchronized fun personalizacionOpcional(nombreActual: String) =
        leer().optBoolean("personalizacionOpcional") && nombreActual == NOMBRE_HEREDADO
    @Synchronized fun confirmarNombre() { guardar(leer().put("personalizacionOpcional", false)) }
    @Synchronized fun tienePin() = leer().has("hash")
    @Synchronized fun necesitaBienvenida() = !leer().optBoolean("bienvenida")
    @Synchronized fun bloqueado() = tienePin() && !sesion
    @Synchronized fun bloquear() { sesion = false; generacion++ }
    @Synchronized fun completarBienvenida() { guardar(leer().put("bienvenida", true).put("personalizacionOpcional", false)) }
    @Synchronized fun revision() = generacion
    @Synchronized fun abrir(pin: String, revision: Long): Boolean {
        val correcto = verificar(pin)
        if (correcto && revision == generacion) sesion = true
        return correcto && revision == generacion
    }
    @Synchronized fun cambiar(actual: String, nuevo: String, confirmacion: String) {
        check(!bloqueado()) { "Vuelve a ingresar antes de configurar el PIN." }
        if (tienePin()) require(verificar(actual)) { "PIN actual incorrecto." }
        require(nuevo.matches(Regex("[0-9]{6}"))) { "Usa exactamente 6 dígitos." }
        require(nuevo == confirmacion) { "Los PIN no coinciden." }
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        guardar(leer().put("sal", codificar(sal)).put("hash", codificar(derivar(nuevo, sal)))
            .put("intentos", 0).put("esperarHasta", 0).put("bienvenida", true))
        sesion = true
    }
    @Synchronized fun desactivar(actual: String) {
        check(!bloqueado()) { "Vuelve a ingresar antes de configurar el PIN." }
        require(verificar(actual)) { "PIN actual incorrecto." }
        val dato = leer(); dato.remove("hash"); dato.remove("sal")
        guardar(dato.put("intentos", 0).put("esperarHasta", 0)); sesion = false
    }
    private fun verificar(pin: String): Boolean {
        val dato = leer()
        check(System.currentTimeMillis() >= dato.optLong("esperarHasta")) { "Demasiados intentos. Espera 30 segundos y reintenta." }
        if (!dato.has("hash")) return false
        val correcto = MessageDigest.isEqual(Base64.decode(dato.getString("hash"), Base64.NO_WRAP),
            derivar(pin, Base64.decode(dato.getString("sal"), Base64.NO_WRAP)))
        val intentos = if (correcto) 0 else dato.optInt("intentos") + 1
        guardar(dato.put("intentos", if (intentos >= 5) 0 else intentos)
            .put("esperarHasta", if (intentos >= 5) System.currentTimeMillis() + 30_000 else 0))
        return correcto
    }
    private fun codificar(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun derivar(pin: String, sal: ByteArray): ByteArray {
        // SHA1 en PBKDF2 está disponible también en API 24–25. La sal cambia con cada PIN.
        val especificacion = PBEKeySpec(pin.toCharArray(), sal, 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(especificacion).encoded }
        finally { especificacion.clearPassword() }
    }
}
