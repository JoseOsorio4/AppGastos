package gt.edu.umg.finanzas.app.ui.operaciones

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.ui.comun.MontoConSigno

/**
 * Confirmación antes de borrar. Solo "Eliminar" llama a OperacionesViewModel.eliminar;
 * "Cancelar", el botón atrás o tocar fuera cierran el diálogo sin tocar la base.
 */
class ConfirmarEliminacionDialogo : DialogFragment() {

    private val viewModel: OperacionesViewModel by lazy {
        ViewModelProvider(requireActivity(), OperacionesViewModel.Fabrica)[OperacionesViewModel::class.java]
    }

    override fun onCreateDialog(estadoGuardado: Bundle?): Dialog {
        val idTransaccion = requireArguments().getLong(ARGUMENTO_ID)
        val movimiento = viewModel.estado.value.movimiento(idTransaccion)
        val mensaje = if (movimiento == null) {
            getString(R.string.confirmar_eliminar_generico)
        } else {
            getString(
                R.string.confirmar_eliminar_mensaje,
                movimiento.descripcion,
                MontoConSigno.texto(movimiento.tipo, movimiento.montoCentavos),
            )
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirmar_eliminar_titulo)
            .setMessage(mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ -> viewModel.eliminar(idTransaccion) }
            .create()
    }

    companion object {
        const val ETIQUETA = "confirmarEliminacion"
        private const val ARGUMENTO_ID = "idTransaccion"

        fun nuevo(idTransaccion: Long) = ConfirmarEliminacionDialogo().apply {
            arguments = Bundle().apply { putLong(ARGUMENTO_ID, idTransaccion) }
        }
    }
}
