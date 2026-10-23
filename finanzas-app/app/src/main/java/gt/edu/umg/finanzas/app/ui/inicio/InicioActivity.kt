package gt.edu.umg.finanzas.app.ui.inicio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import gt.edu.umg.finanzas.app.databinding.ActivityInicioBinding
import gt.edu.umg.finanzas.core.InfoCore

/** Muestra el mensaje del core para comprobar que la app está usando el JAR. */
class InicioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textoConexionCore.text = InfoCore.mensajeConexion()
    }
}
