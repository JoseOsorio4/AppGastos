package gt.edu.umg.finanzas.app.datos.repositorios

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.TasaGuardada
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositorioTipoCambioTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val ahora = Instant.parse("2026-10-29T18:00:00Z")
    private val tasa = TasaCambio(BigDecimal("7.66000"), LocalDate.of(2026,10,28), "Ficticia de prueba")

    private fun cache(): CacheServicios {
        contexto.getSharedPreferences("cache-prueba", 0).edit().clear().commit()
        return CacheServicios(contexto, "cache-prueba")
    }

    @Test
    fun sin_red_ni_cache_no_inventa_tasa_y_con_cache_conserva_fecha_y_fuente() = runBlocking {
        val cache = cache()
        val repositorio = RepositorioTipoCambio(cache, { throw IOException("Sin red de prueba") }, { ahora })
        repositorio.actualizar()
        assertNull(repositorio.estado.value.dato)
        assertTrue(repositorio.estado.value.error)
        val dato = TasaGuardada(tasa, ahora.minusSeconds(86_400))
        cache.guardarTasa(dato)
        repositorio.actualizar()
        assertEquals(dato, repositorio.estado.value.dato)
        assertTrue(repositorio.estado.value.esGuardada && repositorio.estado.value.error)
        assertEquals(dato, CacheServicios(contexto, "cache-prueba").leerTasa())
    }

    @Test
    fun consulta_una_vez_por_dia_y_recupera_la_cache_al_reabrir() = runBlocking {
        val cache = cache()
        var llamadas = 0
        val repositorio = RepositorioTipoCambio(cache, { llamadas++; tasa }, { ahora })
        repositorio.actualizar()
        repositorio.actualizar()
        assertEquals(1, llamadas)
        val reabierto = RepositorioTipoCambio(CacheServicios(contexto, "cache-prueba"), { error("No debe llamar a la red") }, { ahora })
        reabierto.actualizar()
        assertEquals(tasa, reabierto.estado.value.dato?.tasa)
        assertTrue(reabierto.estado.value.esGuardada)
    }
}
