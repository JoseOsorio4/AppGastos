package gt.edu.umg.finanzas.app.ui.inicio

import android.app.Dialog
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.DialogoConversorBinding
import gt.edu.umg.finanzas.app.ui.comun.SentidoConversion
import gt.edu.umg.finanzas.app.ui.comun.TextoTasa
import gt.edu.umg.finanzas.core.reglas.Dinero
import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlinx.coroutines.launch

class ConversorDialogo : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val vista = DialogoConversorBinding.inflate(layoutInflater)
        val viewModel = ViewModelProvider(this, ConversorViewModel.Fabrica)[ConversorViewModel::class.java]
        vista.entradaMontoConversor.doAfterTextChanged { viewModel.cambiarMonto(it?.toString().orEmpty()) }
        vista.grupoSentido.addOnButtonCheckedListener { _, id, marcado ->
            if (marcado) viewModel.elegirSentido(if (id == R.id.botonUsdAGtq) SentidoConversion.USD_A_GTQ else SentidoConversion.GTQ_A_USD)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { estado ->
                    val elegido = if (estado.sentido == SentidoConversion.USD_A_GTQ) R.id.botonUsdAGtq else R.id.botonGtqAUsd
                    if (vista.grupoSentido.checkedButtonId != elegido) vista.grupoSentido.check(elegido)
                    if (vista.entradaMontoConversor.text?.toString() != estado.monto) vista.entradaMontoConversor.setText(estado.monto)
                    vista.campoMontoConversor.prefixText = estado.sentido.entrada.simbolo
                    val resultado = estado.resultado
                    vista.campoMontoConversor.error = (resultado as? Resultado.ErrorNegocio)?.mensaje
                    vista.textoResultadoConversion.text = if (resultado is Resultado.Ok) Dinero.formatear(resultado.valor, estado.sentido.salida) else ""
                    vista.textoTasaConversor.text = TextoTasa.estado(requireContext(), estado.tasa)
                }
            }
        }
        return MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.conversor).setView(vista.root)
            .setNegativeButton(R.string.cerrar, null).create()
    }

    companion object { const val ETIQUETA = "conversor" }
}
