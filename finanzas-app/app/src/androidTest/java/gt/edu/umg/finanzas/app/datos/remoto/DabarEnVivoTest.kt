package gt.edu.umg.finanzas.app.datos.remoto

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.PruebaEnVivo
import gt.edu.umg.finanzas.app.datos.remoto.reflexion.ClienteDabar
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DabarEnVivoTest {
    @Test
    @PruebaEnVivo
    fun consulta_un_versiculo_rv1909_desde_android() = runBlocking<Unit> {
        val cita = ClienteDabar().consultar("Filipenses 4:13")
        assertEquals("Filipenses 4:13", cita.referencia)
        assertEquals("Reina Valera 1909", cita.traduccion)
        assertEquals("Dabar.io", cita.fuente)
        Log.i("DabarEnVivoTest", cita.toString())
    }
}
