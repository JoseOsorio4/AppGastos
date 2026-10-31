package gt.edu.umg.finanzas.app.ui.acceso

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gt.edu.umg.finanzas.app.FinanzasApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PasoAcceso { CARGANDO, NOMBRE, BIENVENIDA, PIN, CONFIGURAR, LISTO }
data class AccesoUiState(val paso: PasoAcceso = PasoAcceso.CARGANDO, val nombre: String = "",
    val tienePin: Boolean = false, val ocupado: Boolean = false, val error: String? = null)

class AccesoViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val app = application as FinanzasApp
    private val mutable = MutableStateFlow(AccesoUiState())
    val estado = mutable.asStateFlow()
    private val bloqueo get() = app.dependencias.bloqueo
    private fun operar(accion: suspend () -> Unit) {
        if (mutable.value.ocupado) return
        mutable.value = mutable.value.copy(ocupado = true, error = null)
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { app.inicializacion.await(); accion() } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = mutable.value.copy(error = e.message ?: "No se pudo guardar. Reintenta.") }
            finally { mutable.value = mutable.value.copy(ocupado = false) }
        }
    }
    fun actualizar(configurar: Boolean) = operar {
        val nombre = app.dependencias.baseDatos.usuarioDao().obtenerPorId(1)?.nombrePerfil.orEmpty()
        val anterior = mutable.value.paso
        val paso = when {
            bloqueo.bloqueado() -> PasoAcceso.PIN
            bloqueo.necesitaBienvenida() -> if (anterior == PasoAcceso.BIENVENIDA || anterior == PasoAcceso.CONFIGURAR)
                anterior else if (nombre.isNotBlank()) PasoAcceso.BIENVENIDA else PasoAcceso.NOMBRE
            configurar -> PasoAcceso.CONFIGURAR
            else -> PasoAcceso.LISTO
        }
        mutable.value = mutable.value.copy(paso = paso, nombre = nombre, tienePin = bloqueo.tienePin())
    }
    fun nombre(texto: String) = operar {
        val nombre = texto.trim()
        require(nombre.length in 1..40 && nombre.none { it.isISOControl() }) { "Escribe un nombre de 1 a 40 caracteres." }
        check(app.dependencias.baseDatos.usuarioDao().cambiarNombre(nombre) == 1) { "No se pudo guardar el nombre. Reintenta." }
        mutable.value = mutable.value.copy(nombre = nombre, paso = PasoAcceso.BIENVENIDA)
    }
    fun configurar() { mutable.value = mutable.value.copy(paso = PasoAcceso.CONFIGURAR, error = null) }
    fun continuar() = operar { check(!bloqueo.bloqueado()); bloqueo.completarBienvenida(); mutable.value = mutable.value.copy(paso = PasoAcceso.LISTO) }
    fun ingresar(pin: String, configurar: Boolean) {
        val revision = bloqueo.revision()
        operar {
            require(bloqueo.abrir(pin, revision)) { "PIN incorrecto o sesión interrumpida." }
            mutable.value = mutable.value.copy(paso = if (configurar) PasoAcceso.CONFIGURAR else PasoAcceso.LISTO)
        }
    }
    fun guardar(actual: String, nuevo: String, confirmacion: String) = operar {
        bloqueo.cambiar(actual, nuevo, confirmacion)
        mutable.value = mutable.value.copy(paso = PasoAcceso.LISTO)
    }
    fun desactivar(actual: String) = operar {
        bloqueo.desactivar(actual); mutable.value = mutable.value.copy(paso = PasoAcceso.LISTO)
    }
}
