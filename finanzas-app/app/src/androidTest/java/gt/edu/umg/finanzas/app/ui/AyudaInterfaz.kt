package gt.edu.umg.finanzas.app.ui

import android.os.ParcelFileDescriptor
import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.platform.app.InstrumentationRegistry
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue

/** Utilidades de las pruebas de pantalla, que usan la base real de la app. */
object AyudaInterfaz {

    val app: FinanzasApp get() = ApplicationProvider.getApplicationContext()

    /** Vacía la base y vuelve a sembrar el usuario y las 12 categorías, como una instalación nueva. */
    fun baseLimpia() = runBlocking {
        check(app.packageName.endsWith(".pruebas")) { "Ejecuta con -PdatosPrueba para proteger los datos de uso diario" }
        app.inicializacion.await()
        app.dependencias.baseDatos.clearAllTables()
        assertTrue(app.dependencias.inicializador.inicializar() is Resultado.Ok)
    }

    fun esperarTexto(id: Int, texto: String) =
        reintentar { onView(withId(id)).check(matches(withText(texto))) }

    fun capturar(nombre: String) {
        val instrumento = InstrumentationRegistry.getInstrumentation()
        instrumento.uiAutomation.waitForIdle(500, 5_000)
        ParcelFileDescriptor.AutoCloseInputStream(instrumento.uiAutomation.executeShellCommand(
            "screencap -p /sdcard/Download/usabilidad-$nombre.png",
        )).use { it.readBytes() }
    }

    // scrollTo exige ver el 90 % de la vista, imposible si los metadatos superan el viewport.
    fun mostrarInicio() = object : ViewAction {
        override fun getConstraints() = isAssignableFrom(View::class.java)
        override fun getDescription() = "desplazar hasta el inicio de un texto largo"
        override fun perform(controlador: UiController, vista: View) {
            vista.requestRectangleOnScreen(Rect(0, 0, vista.width, minOf(vista.height, 100)), true)
            controlador.loopMainThreadUntilIdle()
        }
    }

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
