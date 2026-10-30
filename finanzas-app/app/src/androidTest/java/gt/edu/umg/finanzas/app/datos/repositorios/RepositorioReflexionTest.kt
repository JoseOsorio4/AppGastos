package gt.edu.umg.finanzas.app.datos.repositorios

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.ReflexionGuardada
import gt.edu.umg.finanzas.app.datos.remoto.RespuestaDabarGuardada
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositorioReflexionTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private fun cache(): CacheServicios {
        contexto.getSharedPreferences("reflexion-prueba", 0).edit().clear().commit()
        return CacheServicios(contexto, "reflexion-prueba")
    }

    @Test
    fun conserva_una_cita_por_dia_al_reabrir_y_la_renueva_al_dia_siguiente() = runBlocking {
        val cache = cache()
        var fecha = LocalDate.of(2026, 10, 30)
        var llamadas = 0
        val repositorio = RepositorioReflexion(cache, { llamadas++; RespuestaDabarGuardada.leer(it) }) { fecha }
        repositorio.actualizar()
        val primera = repositorio.estado.value.dato!!
        repositorio.actualizar()
        assertEquals(primera, cache.leerReflexion())
        assertEquals(1, llamadas)
        val reabierto = RepositorioReflexion(CacheServicios(contexto, "reflexion-prueba"),
            { llamadas++; RespuestaDabarGuardada.leer(it) }) { fecha }
        reabierto.actualizar()
        assertEquals(primera, reabierto.estado.value.dato)
        assertEquals(1, llamadas)
        fecha = fecha.plusDays(1)
        reabierto.actualizar()
        assertEquals(fecha, cache.leerReflexion()?.fecha)
        assertNotEquals(primera.reflexion, reabierto.estado.value.dato?.reflexion)
        assertFalse(reabierto.estado.value.error)
        assertEquals(2, llamadas)
        assertEquals("Dabar.io", cache.leerReflexion()?.reflexion?.fuente)
    }

    @Test
    fun ocultar_persiste_sin_eliminar_la_cita_y_mostrar_la_recupera() = runBlocking {
        val cache = cache()
        val repositorio = RepositorioReflexion(cache, RespuestaDabarGuardada::leer)
        repositorio.actualizar()
        val cita = repositorio.estado.value.dato
        repositorio.cambiarVisibilidad()
        val reabierto = RepositorioReflexion(CacheServicios(contexto, "reflexion-prueba"), { error("Debe usar caché") })
        reabierto.actualizar()
        assertFalse(reabierto.estado.value.visible)
        reabierto.cambiarVisibilidad()
        assertTrue(reabierto.estado.value.visible)
        assertEquals(cita, reabierto.estado.value.dato)
    }

    @Test
    fun sin_red_guarda_el_catalogo_local_y_no_repite_la_peticion_en_el_dia() = runBlocking {
        val cache = cache()
        val hoy = LocalDate.of(2026, 10, 30)
        var llamadas = 0
        val repositorio = RepositorioReflexion(cache, { llamadas++; throw IOException("Sin red de prueba") }) { hoy }
        repositorio.actualizar()
        assertTrue(repositorio.estado.value.errorConsulta)
        val guardada = cache.leerReflexion()!!
        assertEquals(hoy, guardada.fecha)
        assertEquals("Filipenses 4:13", guardada.reflexion.referencia)
        assertEquals("Catálogo local · eBible", guardada.reflexion.fuente)
        assertEquals("Reina-Valera 1909", guardada.reflexion.traduccion)
        val reabierto = RepositorioReflexion(CacheServicios(contexto, "reflexion-prueba"),
            { llamadas++; error("No debe consultar de nuevo") }) { hoy }
        reabierto.actualizar()
        assertEquals(guardada, reabierto.estado.value.dato)
        assertEquals(1, llamadas)
    }

    @Test
    fun reintentar_recupera_de_local_a_remoto_y_otro_fallo_conserva_la_cita_remota() = runBlocking {
        val cache = cache()
        var sinRed = true
        val repositorio = RepositorioReflexion(cache, {
            if (sinRed) throw IOException("Sin red de prueba")
            RespuestaDabarGuardada.leer(it)
        }) { LocalDate.of(2026, 10, 30) }
        repositorio.actualizar()
        assertEquals("Catálogo local · eBible", cache.leerReflexion()?.reflexion?.fuente)
        sinRed = false
        repositorio.actualizar(forzar = true)
        val remota = cache.leerReflexion()!!
        assertEquals("Dabar.io", remota.reflexion.fuente)
        assertFalse(repositorio.estado.value.errorConsulta || repositorio.estado.value.error)
        sinRed = true
        repositorio.actualizar(forzar = true)
        assertEquals(remota, repositorio.estado.value.dato)
        assertEquals(remota, CacheServicios(contexto, "reflexion-prueba").leerReflexion())
        assertTrue(repositorio.estado.value.errorConsulta)
    }

    @Test
    fun otro_dia_sin_red_usa_cita_local_de_hoy_y_no_reetiqueta_la_remota_anterior() = runBlocking {
        val cache = cache()
        val ayer = LocalDate.of(2026, 10, 30)
        cache.guardarReflexion(ReflexionGuardada(RespuestaDabarGuardada.leer("Filipenses 4:13"), ayer))
        val repositorio = RepositorioReflexion(cache, { throw IOException("Sin red de prueba") }) { ayer.plusDays(1) }
        repositorio.actualizar()
        assertEquals("Salmos 23:1", repositorio.estado.value.dato?.reflexion?.referencia)
        assertEquals("Catálogo local · eBible", repositorio.estado.value.dato?.reflexion?.fuente)
        assertEquals(ayer.plusDays(1), cache.leerReflexion()?.fecha)
    }

    @Test
    fun cancelar_consulta_no_guarda_una_respuesta_ni_se_convierte_en_error_de_red() = runBlocking {
        val cache = cache()
        val espera = CompletableDeferred<Unit>()
        val repositorio = RepositorioReflexion(cache, { espera.await(); RespuestaDabarGuardada.leer(it) })
        val trabajo = async { repositorio.actualizar() }
        yield()
        assertTrue(repositorio.estado.value.cargando)
        assertNotNull(repositorio.estado.value.dato)
        trabajo.cancel()
        trabajo.join()
        assertTrue(trabajo.isCancelled)
        assertNull(cache.leerReflexion())
        assertFalse(repositorio.estado.value.cargando || repositorio.estado.value.errorConsulta)
    }
}
