package gt.edu.umg.finanzas.app.datos.repositorios

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.ClimaGuardado
import gt.edu.umg.finanzas.app.datos.remoto.clima.LectorRespuestaClima
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositorioClimaTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val ahora = Instant.parse("2026-10-30T07:00:00Z")
    private val clima get() = InstrumentationRegistry.getInstrumentation().context.assets
        .open("openmeteo/clima-2026-10-30.json").bufferedReader().use { LectorRespuestaClima.leer(it.readText()) }

    private fun cache(): CacheServicios {
        contexto.getSharedPreferences("clima-prueba", 0).edit().clear().commit()
        return CacheServicios(contexto, "clima-prueba")
    }

    @Test
    fun sin_red_conserva_el_dato_y_sus_fechas_y_reintentar_recupera() = runBlocking {
        val cache = cache()
        var sinRed = true
        val repositorio = RepositorioClima(cache, { if (sinRed) throw IOException("Sin red de prueba") else clima }, { ahora })
        repositorio.actualizar()
        assertTrue(repositorio.estado.value.error)
        assertNull(repositorio.estado.value.dato)
        val viejo = ClimaGuardado(clima, ahora.minusSeconds(3600))
        cache.guardarClima(viejo)
        repositorio.actualizar()
        assertEquals(viejo, repositorio.estado.value.dato)
        assertTrue(repositorio.estado.value.esGuardado && repositorio.estado.value.error)
        assertEquals(viejo, CacheServicios(contexto, "clima-prueba").leerClima())
        sinRed = false
        repositorio.actualizar(forzar = true)
        assertFalse(repositorio.estado.value.error || repositorio.estado.value.esGuardado)
        assertEquals(ahora, cache.leerClima()?.consultadoEn)
    }

    @Test
    fun cache_dura_treinta_minutos_y_reintento_la_omite() = runBlocking {
        val cache = cache()
        var reloj = ahora
        var llamadas = 0
        val consultar = suspend { llamadas++; clima }
        val repositorio = RepositorioClima(cache, consultar, { reloj })
        repositorio.actualizar()
        reloj = ahora.plusSeconds(1799)
        val reabierto = RepositorioClima(CacheServicios(contexto, "clima-prueba"), consultar, { reloj })
        reabierto.actualizar()
        assertEquals(1, llamadas)
        assertTrue(reabierto.estado.value.esGuardado)
        reloj = ahora.plusSeconds(1800)
        reabierto.actualizar()
        assertEquals(2, llamadas)
        reabierto.actualizar(forzar = true)
        assertEquals(3, llamadas)
    }

    @Test
    fun carga_no_borra_cache_y_la_cancelacion_no_se_confunde_con_error_de_red() = runBlocking {
        val cache = cache()
        val guardado = ClimaGuardado(clima, ahora.minusSeconds(3600))
        cache.guardarClima(guardado)
        val espera = CompletableDeferred<Unit>()
        val repositorio = RepositorioClima(cache, { espera.await(); clima }, { ahora })
        val trabajo = async { repositorio.actualizar() }
        yield()
        assertTrue(repositorio.estado.value.cargando)
        assertEquals(guardado, repositorio.estado.value.dato)
        trabajo.cancel()
        trabajo.join()
        assertTrue(trabajo.isCancelled)
        assertFalse(repositorio.estado.value.cargando || repositorio.estado.value.error)
        assertEquals(guardado, cache.leerClima())
    }
}
