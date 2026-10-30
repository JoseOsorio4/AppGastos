package gt.edu.umg.finanzas.app.datos.repositorios

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
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
        val repositorio = RepositorioReflexion(cache) { fecha }
        repositorio.actualizar()
        val primera = repositorio.estado.value.dato!!
        repositorio.actualizar()
        assertEquals(primera, cache.leerReflexion())
        val reabierto = RepositorioReflexion(CacheServicios(contexto, "reflexion-prueba")) { fecha }
        reabierto.actualizar()
        assertEquals(primera, reabierto.estado.value.dato)
        fecha = fecha.plusDays(1)
        reabierto.actualizar()
        assertEquals(fecha, cache.leerReflexion()?.fecha)
        assertNotEquals(primera.reflexion, reabierto.estado.value.dato?.reflexion)
        assertFalse(reabierto.estado.value.error)
    }

    @Test
    fun ocultar_persiste_sin_eliminar_la_cita_y_mostrar_la_recupera() = runBlocking {
        val cache = cache()
        val repositorio = RepositorioReflexion(cache)
        repositorio.actualizar()
        val cita = repositorio.estado.value.dato
        repositorio.cambiarVisibilidad()
        val reabierto = RepositorioReflexion(CacheServicios(contexto, "reflexion-prueba"))
        reabierto.actualizar()
        assertFalse(reabierto.estado.value.visible)
        reabierto.cambiarVisibilidad()
        assertTrue(reabierto.estado.value.visible)
        assertEquals(cita, reabierto.estado.value.dato)
    }
}
