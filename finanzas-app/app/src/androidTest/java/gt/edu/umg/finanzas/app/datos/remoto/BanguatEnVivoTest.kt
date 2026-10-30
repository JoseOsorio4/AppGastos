package gt.edu.umg.finanzas.app.datos.remoto

import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.edu.umg.finanzas.app.PruebaEnVivo
import gt.edu.umg.finanzas.app.datos.remoto.tipocambio.ClienteBanguat
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BanguatEnVivoTest {
    @Test
    @PruebaEnVivo
    fun consulta_el_servicio_oficial_desde_android(): Unit = runBlocking {
        val tasa = ClienteBanguat().consultar()
        assertTrue(tasa.gtqPorUsd > BigDecimal.ZERO)
        android.util.Log.i("BanguatRealTest", "Referencia ${tasa.fechaReferencia}: ${tasa.gtqPorUsd.toPlainString()}")
    }
}
