package gt.edu.umg.finanzas.app.datos.preferencias

import android.content.Context
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TasaGuardada(val tasa: TasaCambio, val consultadoEn: Instant)

/** Caché auxiliar de la tasa; nunca contiene movimientos ni saldos. */
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
}
