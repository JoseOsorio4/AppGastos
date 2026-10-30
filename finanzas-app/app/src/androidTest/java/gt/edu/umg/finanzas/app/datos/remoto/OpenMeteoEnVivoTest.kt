package gt.edu.umg.finanzas.app.datos.remoto

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.PruebaEnVivo
import gt.edu.umg.finanzas.app.datos.remoto.clima.ClienteOpenMeteo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenMeteoEnVivoTest {
    @Test
    @PruebaEnVivo
    fun consulta_real_desde_android() = runBlocking<Unit> {
        val clima = ClienteOpenMeteo().consultar()
        assertTrue(clima.temperaturaC.isFinite())
        Log.i("OpenMeteoEnVivoTest", clima.toString())
    }
}
