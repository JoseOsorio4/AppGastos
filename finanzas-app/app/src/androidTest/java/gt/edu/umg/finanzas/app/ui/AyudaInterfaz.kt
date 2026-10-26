package gt.edu.umg.finanzas.app.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue

/** Utilidades de las pruebas de pantalla, que usan la base real de la app. */
object AyudaInterfaz {

    val app: FinanzasApp get() = ApplicationProvider.getApplicationContext()

    /** Vacía la base y vuelve a sembrar el usuario y las 12 categorías, como una instalación nueva. */
    fun baseLimpia() = runBlocking {
        app.inicializacion.await()
        app.dependencias.baseDatos.clearAllTables()
        assertTrue(app.dependencias.inicializador.inicializar() is Resultado.Ok)
    }

    fun esperarTexto(id: Int, texto: String) =
        reintentar { onView(withId(id)).check(matches(withText(texto))) }

    /** Room responde en otro hilo y Espresso no lo espera: se repite la comprobación hasta 5 s. */
    fun reintentar(accion: () -> Unit) {
        val limite = System.currentTimeMillis() + 5_000
        while (true) {
            try {
                accion()
                return
            } catch (error: Throwable) {
                if (System.currentTimeMillis() > limite) throw error
                Thread.sleep(100)
            }
        }
    }
}
