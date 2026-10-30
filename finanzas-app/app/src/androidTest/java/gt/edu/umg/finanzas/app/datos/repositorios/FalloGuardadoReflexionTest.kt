package gt.edu.umg.finanzas.app.datos.repositorios

import android.content.Context
import android.content.ContextWrapper
import android.system.Os
import android.system.OsConstants
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.ReflexionGuardada
import gt.edu.umg.finanzas.app.datos.remoto.RespuestaDabarGuardada
import java.io.File
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FalloGuardadoReflexionTest {
    @Test
    fun fallo_real_de_escritura_conserva_el_aviso_hasta_persistir_al_reintentar() = runBlocking {
        val destino = InstrumentationRegistry.getInstrumentation().targetContext
        check(destino.packageName.endsWith(".pruebas"))
        // Otro almacén de Android: negar escritura no afecta las preferencias de tema, tasa o clima.
        val contexto = object : ContextWrapper(destino.createDeviceProtectedStorageContext()) {
            override fun getApplicationContext(): Context = this
        }
        val nombre = "escritura-reflexion-prueba"
        contexto.getSharedPreferences(nombre, 0).edit().clear().commit()
        val cache = CacheServicios(contexto, nombre)
        val hoy = LocalDate.of(2026, 10, 30)
        cache.guardarReflexion(ReflexionGuardada(RespuestaDabarGuardada.leer("Proverbios 16:3"), hoy.minusDays(1)))
        val archivo = File(contexto.dataDir, "shared_prefs/$nombre.xml")
        val anterior = archivo.readBytes()
        val carpeta = archivo.parentFile!!
        val permisos = Os.stat(carpeta.path).st_mode and 0xFFF
        val escritura = OsConstants.S_IWUSR or OsConstants.S_IWGRP or OsConstants.S_IWOTH
        val repositorio = RepositorioReflexion(cache, RespuestaDabarGuardada::leer) { hoy }
        try {
            Os.chmod(carpeta.path, permisos and escritura.inv())
            assertThrows(IOException::class.java) { File(carpeta, "sonda").outputStream().close() }
            repositorio.actualizar()
            assertTrue(repositorio.estado.value.error)
            assertFalse(repositorio.estado.value.errorConsulta)
            assertNotNull(repositorio.estado.value.dato)
            assertArrayEquals(anterior, archivo.readBytes())
            // Inicio vuelve a pedir el estado al entrar en STARTED; eso no debe ocultar un fallo de disco.
            repositorio.actualizar()
            assertTrue("El fallo sigue sin resolverse en disco", repositorio.estado.value.error)
            assertArrayEquals(anterior, archivo.readBytes())
        } finally {
            Os.chmod(carpeta.path, permisos)
        }
        repositorio.actualizar(forzar = true)
        assertFalse(repositorio.estado.value.error || repositorio.estado.value.errorConsulta)
        assertEquals(hoy, cache.leerReflexion()?.fecha)
        assertFalse(anterior.contentEquals(archivo.readBytes()))
        assertTrue(archivo.readText().contains(hoy.toString()))
    }
}
