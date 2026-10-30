package gt.edu.umg.finanzas.app.datos.preferencias

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Tema(val modoNocturno: Int) {
    SISTEMA(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    CLARO(AppCompatDelegate.MODE_NIGHT_NO),
    OSCURO(AppCompatDelegate.MODE_NIGHT_YES),
}

/** Una sola clave local; no pertenece a las tablas financieras de Room. */
class PreferenciaTema(context: Context) {
    private val preferencias = context.applicationContext.getSharedPreferences("apariencia", Context.MODE_PRIVATE)
    private val elegido = MutableStateFlow(
        Tema.entries.firstOrNull { it.name == preferencias.getString("tema", null) } ?: Tema.SISTEMA,
    )
    val tema: StateFlow<Tema> = elegido.asStateFlow()

    fun guardar(tema: Tema) {
        preferencias.edit().putString("tema", tema.name).apply()
        elegido.value = tema
    }
}
