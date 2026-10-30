package gt.edu.umg.finanzas.app.datos.repositorios

import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.TasaGuardada
import gt.edu.umg.finanzas.app.datos.remoto.tipocambio.ClienteBanguat
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EstadoTasa(
    val dato: TasaGuardada? = null,
    val cargando: Boolean = false,
    val esGuardada: Boolean = true,
    val error: Boolean = false,
)

class RepositorioTipoCambio(
    private val cache: CacheServicios,
    private val consultar: suspend () -> TasaCambio = ClienteBanguat()::consultar,
    private val ahora: () -> Instant = { Instant.now() },
) {
    private val consulta = Mutex()
    private val actual = MutableStateFlow(EstadoTasa(dato = cache.leerTasa()))
    val estado = actual.asStateFlow()

    suspend fun actualizar(forzar: Boolean = false) = consulta.withLock {
        val guardada = cache.leerTasa()
        val zona = ZoneId.of("America/Guatemala")
        if (!forzar && guardada?.consultadoEn?.atZone(zona)?.toLocalDate() == ahora().atZone(zona).toLocalDate()) {
            actual.value = EstadoTasa(guardada)
            return@withLock
        }
        actual.value = EstadoTasa(guardada, cargando = true)
        try {
            val nueva = TasaGuardada(consultar(), ahora())
            cache.guardarTasa(nueva)
            actual.value = EstadoTasa(nueva, esGuardada = false)
        } catch (e: CancellationException) {
            actual.value = EstadoTasa(guardada)
            throw e
        } catch (_: Exception) {
            // Un fallo conserva la última tasa real con su fecha; nunca fabrica una referencia.
            actual.value = EstadoTasa(guardada, error = true)
        }
    }
}
