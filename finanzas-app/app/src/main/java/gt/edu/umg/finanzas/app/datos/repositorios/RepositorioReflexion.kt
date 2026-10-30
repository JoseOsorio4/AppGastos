package gt.edu.umg.finanzas.app.datos.repositorios

import gt.edu.umg.finanzas.app.datos.local.CatalogoReflexiones
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.ReflexionGuardada
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EstadoReflexion(
    val dato: ReflexionGuardada? = null,
    val visible: Boolean = true,
    val cargando: Boolean = false,
    val error: Boolean = false,
)

class RepositorioReflexion(
    private val cache: CacheServicios,
    private val hoy: () -> LocalDate = { LocalDate.now(ZoneId.of("America/Guatemala")) },
) {
    private val consulta = Mutex()
    private val actual = MutableStateFlow(EstadoReflexion(cache.leerReflexion(), cache.reflexionVisible()))
    val estado = actual.asStateFlow()

    suspend fun actualizar() = consulta.withLock {
        val fecha = hoy()
        val guardada = cache.leerReflexion()
        if (guardada?.fecha == fecha) {
            actual.value = actual.value.copy(dato = guardada, cargando = false, error = false)
            return@withLock
        }
        actual.value = actual.value.copy(cargando = true)
        // El catálogo local no depende del proveedor remoto pendiente ni de la conectividad.
        val nueva = ReflexionGuardada(CatalogoReflexiones.paraFecha(fecha), fecha)
        try {
            cache.guardarReflexion(nueva)
            actual.value = actual.value.copy(dato = nueva, cargando = false, error = false)
        } catch (e: CancellationException) {
            actual.value = actual.value.copy(cargando = false)
            throw e
        } catch (_: Exception) {
            actual.value = actual.value.copy(dato = nueva, cargando = false, error = true)
        }
    }

    fun cambiarVisibilidad() {
        val visible = !actual.value.visible
        cache.guardarReflexionVisible(visible)
        actual.value = actual.value.copy(visible = visible)
    }
}
