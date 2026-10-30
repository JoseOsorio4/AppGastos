package gt.edu.umg.finanzas.app.ui.inicio

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.preferencias.Tema

/** DialogFragment conserva el selector abierto al recrear Inicio. */
class TemaDialogo : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity(), InicioViewModel.Fabrica)[InicioViewModel::class.java]
        val temas = Tema.entries
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.tema)
            .setSingleChoiceItems(R.array.opciones_tema, temas.indexOf(viewModel.estado.value.tema)) { _, posicion ->
                val elegido = temas[posicion]
                viewModel.elegirTema(elegido)
                dismiss()
                AppCompatDelegate.setDefaultNightMode(elegido.modoNocturno)
            }
            .setNegativeButton(R.string.cancelar, null)
            .create()
    }

    companion object {
        const val ETIQUETA = "selectorTema"
    }
}
