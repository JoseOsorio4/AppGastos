package gt.edu.umg.finanzas.app

import android.app.Application
import android.util.Log
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Crea Dependencias una vez por proceso y siembra los datos iniciales al arrancar,
 * antes de abrir cualquier pantalla.
 */
class FinanzasApp : Application() {

    lateinit var dependencias: Dependencias
        private set

    /** Las pantallas del Inc. 3 esperarán este resultado antes de leer saldo y categorías. */
    lateinit var inicializacion: Deferred<Resultado<Unit>>
        private set

    // Vive lo mismo que el proceso; Room ejecuta sus funciones suspend fuera del hilo principal.
    private val alcance = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        dependencias = Dependencias(this)
        inicializacion = alcance.async {
            dependencias.inicializador.inicializar().also { resultado ->
                if (resultado is Resultado.ErrorAlmacenamiento) {
                    Log.e(ETIQUETA, "No se pudieron crear los datos iniciales", resultado.causa)
                }
            }
        }
    }

    private companion object {
        const val ETIQUETA = "FinanzasApp"
    }
}
