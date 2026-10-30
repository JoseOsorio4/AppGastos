package gt.edu.umg.finanzas.app.datos.repositorios

import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.ClimaGuardado
import gt.edu.umg.finanzas.app.datos.remoto.clima.ClimaJutiapa
import gt.edu.umg.finanzas.app.datos.remoto.clima.ClienteOpenMeteo
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EstadoClima(
    val dato: ClimaGuardado? = null,
    val cargando: Boolean = false,
    val esGuardado: Boolean = true,
    val error: Boolean = false,
)

class RepositorioClima(
    private val cache: CacheServicios,
    private val consultar: suspend () -> ClimaJutiapa = ClienteOpenMeteo()::consultar,
    private val ahora: () -> Instant = { Instant.now() },
) {
    private val consulta = Mutex()
    private val actual = MutableStateFlow(EstadoClima(cache.leerClima()))
    val estado = actual.asStateFlow()

    suspend fun actualizar(forzar: Boolean = false) = consulta.withLock {
        val guardado = cache.leerClima()
        val edad = guardado?.let { Duration.between(it.consultadoEn, ahora()) }
        if (!forzar && edad != null && !edad.isNegative && edad < Duration.ofMinutes(30)) {
            actual.value = EstadoClima(guardado)
            return@withLock
        }
        actual.value = EstadoClima(guardado, cargando = true)
        try {
            val nuevo = ClimaGuardado(consultar(), ahora())
            cache.guardarClima(nuevo)
            actual.value = EstadoClima(nuevo, esGuardado = false)
        } catch (e: CancellationException) {
            actual.value = EstadoClima(guardado)
            throw e
        } catch (_: Exception) {
            actual.value = EstadoClima(guardado, error = true)
        }
    }
}
