package gt.edu.umg.finanzas.app.datos.repositorios

import gt.edu.umg.finanzas.app.datos.local.CatalogoReflexiones
import gt.edu.umg.finanzas.app.datos.local.Reflexion
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.ReflexionGuardada
import gt.edu.umg.finanzas.app.datos.remoto.reflexion.ClienteDabar
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
    val errorConsulta: Boolean = false,
)

class RepositorioReflexion(
    private val cache: CacheServicios,
    private val consultar: suspend (String) -> Reflexion = ClienteDabar()::consultar,
    private val hoy: () -> LocalDate = { LocalDate.now(ZoneId.of("America/Guatemala")) },
) {
    private val consulta = Mutex()
    private val actual = MutableStateFlow(EstadoReflexion(cache.leerReflexion(), cache.reflexionVisible()))
    val estado = actual.asStateFlow()

    suspend fun actualizar(forzar: Boolean = false) = consulta.withLock {
        val fecha = hoy()
        val guardada = cache.leerReflexion()
        // Un commit fallido puede dejar la cita solo en memoria de SharedPreferences.
        if (!forzar && guardada?.fecha == fecha && !actual.value.error) {
            actual.value = actual.value.copy(dato = guardada, cargando = false, error = false, errorConsulta = false)
            return@withLock
        }
        val local = CatalogoReflexiones.paraFecha(fecha)
        val respaldo = guardada?.takeIf { it.fecha == fecha } ?: ReflexionGuardada(local, fecha)
        actual.value = actual.value.copy(dato = respaldo, cargando = true, error = false, errorConsulta = false)
        var errorConsulta = false
        val nueva = try {
            ReflexionGuardada(consultar(local.referencia), fecha)
        } catch (e: CancellationException) {
            actual.value = actual.value.copy(dato = guardada, cargando = false)
            throw e
        } catch (_: Exception) {
            // Un fallo de red o del contrato conserva la cita de hoy; nunca se atribuye el catálogo a Dabar.
            errorConsulta = true
            respaldo
        }
        try {
            cache.guardarReflexion(nueva)
            actual.value = actual.value.copy(dato = nueva, cargando = false, error = false, errorConsulta = errorConsulta)
        } catch (e: CancellationException) {
            actual.value = actual.value.copy(cargando = false)
            throw e
        } catch (_: Exception) {
            actual.value = actual.value.copy(dato = nueva, cargando = false, error = true, errorConsulta = errorConsulta)
        }
    }

    fun cambiarVisibilidad() {
        val visible = !actual.value.visible
        cache.guardarReflexionVisible(visible)
        actual.value = actual.value.copy(visible = visible)
    }
}
