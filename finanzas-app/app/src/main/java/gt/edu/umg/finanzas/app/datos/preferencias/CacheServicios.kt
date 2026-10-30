package gt.edu.umg.finanzas.app.datos.preferencias

import android.content.Context
import gt.edu.umg.finanzas.app.datos.local.Reflexion
import gt.edu.umg.finanzas.app.datos.remoto.clima.ClimaJutiapa
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TasaGuardada(val tasa: TasaCambio, val consultadoEn: Instant)
data class ClimaGuardado(val clima: ClimaJutiapa, val consultadoEn: Instant)
data class ReflexionGuardada(val reflexion: Reflexion, val fecha: LocalDate)

/** Caché auxiliar de servicios; nunca contiene movimientos ni saldos. */
class CacheServicios(context: Context, nombre: String = "servicios") {
    private val preferencias = context.applicationContext.getSharedPreferences(nombre, Context.MODE_PRIVATE)

    fun leerTasa(): TasaGuardada? = try {
        val valor = preferencias.getString("tasaGtqPorUsd", null)
        if (valor == null) null else TasaGuardada(
            TasaCambio(
                BigDecimal(valor),
                LocalDate.parse(checkNotNull(preferencias.getString("fechaTasa", null))),
                checkNotNull(preferencias.getString("fuenteTasa", null)),
            ),
            Instant.parse(checkNotNull(preferencias.getString("consultadoEn", null))),
        )
    } catch (_: RuntimeException) {
        null
    }

    suspend fun guardarTasa(dato: TasaGuardada) = withContext(Dispatchers.IO) {
        check(preferencias.edit()
            .putString("tasaGtqPorUsd", dato.tasa.gtqPorUsd.toPlainString())
            .putString("fechaTasa", dato.tasa.fechaReferencia.toString())
            .putString("fuenteTasa", dato.tasa.fuente)
            .putString("consultadoEn", dato.consultadoEn.toString())
            .commit()) { "No se guardó la tasa" }
    }

    fun leerClima(): ClimaGuardado? = try {
        val temperatura = preferencias.getString("climaTemperatura", null)?.toDouble()
        if (temperatura == null || !temperatura.isFinite()) null else ClimaGuardado(
            ClimaJutiapa(
                temperatura,
                preferencias.getInt("climaCodigo", -1),
                LocalDateTime.parse(checkNotNull(preferencias.getString("climaMedidoEn", null))),
            ),
            Instant.parse(checkNotNull(preferencias.getString("climaConsultadoEn", null))),
        )
    } catch (_: RuntimeException) {
        null
    }

    suspend fun guardarClima(dato: ClimaGuardado) = withContext(Dispatchers.IO) {
        check(preferencias.edit()
            .putString("climaTemperatura", dato.clima.temperaturaC.toString())
            .putInt("climaCodigo", dato.clima.codigoWmo)
            .putString("climaMedidoEn", dato.clima.medidoEn.toString())
            .putString("climaConsultadoEn", dato.consultadoEn.toString())
            .commit()) { "No se guardó el clima" }
    }

    fun leerReflexion(): ReflexionGuardada? = try {
        val texto = preferencias.getString("reflexionTexto", null)
        if (texto.isNullOrBlank()) null else ReflexionGuardada(
            Reflexion(
                texto,
                checkNotNull(preferencias.getString("reflexionReferencia", null)),
                checkNotNull(preferencias.getString("reflexionTraduccion", null)),
                checkNotNull(preferencias.getString("reflexionFuente", null)),
            ),
            LocalDate.parse(checkNotNull(preferencias.getString("reflexionFecha", null))),
        )
    } catch (_: RuntimeException) {
        null
    }

    suspend fun guardarReflexion(dato: ReflexionGuardada) = withContext(Dispatchers.IO) {
        check(preferencias.edit()
            .putString("reflexionTexto", dato.reflexion.texto)
            .putString("reflexionReferencia", dato.reflexion.referencia)
            .putString("reflexionTraduccion", dato.reflexion.traduccion)
            .putString("reflexionFuente", dato.reflexion.fuente)
            .putString("reflexionFecha", dato.fecha.toString())
            .commit()) { "No se guardó la reflexión" }
    }

    fun reflexionVisible(): Boolean = preferencias.getBoolean("reflexionVisible", true)

    fun guardarReflexionVisible(visible: Boolean) {
        preferencias.edit().putBoolean("reflexionVisible", visible).apply()
    }
}
