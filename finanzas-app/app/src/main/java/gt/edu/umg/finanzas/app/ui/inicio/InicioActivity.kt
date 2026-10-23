package gt.edu.umg.finanzas.app.ui.inicio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import gt.edu.umg.finanzas.app.databinding.ActivityInicioBinding
import gt.edu.umg.finanzas.core.InfoCore

/**
 * Pantalla Inicio. En el Incremento 0 solo demuestra que la app ejecuta código del JAR del core;
 * en el Incremento 3 mostrará saldo, período y gráfico observando InicioViewModel.
 */
class InicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textoConexionCore.text = InfoCore.mensajeConexion()
    }
}
