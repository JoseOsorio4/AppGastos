package gt.edu.umg.finanzas.app.ui.operaciones

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.DialogoDetalleMovimientoBinding
import gt.edu.umg.finanzas.app.ui.comun.Fechas
import gt.edu.umg.finanzas.app.ui.comun.MontoConSigno
import gt.edu.umg.finanzas.app.ui.comun.RecursosCategoria
import gt.edu.umg.finanzas.app.ui.formulario.FormularioActivity
import gt.edu.umg.finanzas.core.dominio.MovimientoVista
import kotlinx.coroutines.launch

/**
 * Detalle de un movimiento (dialogo_detalle_movimiento.xml). Es un DialogFragment para sobrevivir
 * a la rotación. Lee el movimiento del mismo OperacionesViewModel que la lista y se cierra solo si
 * el movimiento ya no está.
 */
class DetalleMovimientoDialogo : DialogFragment() {

    private lateinit var vista: DialogoDetalleMovimientoBinding
    private val viewModel: OperacionesViewModel by lazy {
        ViewModelProvider(requireActivity(), OperacionesViewModel.Fabrica)[OperacionesViewModel::class.java]
    }
    private val idTransaccion: Long get() = requireArguments().getLong(ARGUMENTO_ID)

    override fun onCreateDialog(estadoGuardado: Bundle?): Dialog {
        vista = DialogoDetalleMovimientoBinding.inflate(layoutInflater)
        vista.botonEditar.setOnClickListener {
            startActivity(FormularioActivity.paraEditar(requireContext(), idTransaccion))
            dismiss()
        }
        vista.botonEliminar.setOnClickListener {
            ConfirmarEliminacionDialogo.nuevo(idTransaccion).show(parentFragmentManager, ConfirmarEliminacionDialogo.ETIQUETA)
            dismiss()
        }
        // Se observa con el ciclo de vida del fragmento: solo mientras el diálogo está visible.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { estado ->
                    if (estado.cargando) return@collect
                    val movimiento = estado.movimiento(idTransaccion)
                    if (movimiento == null) dismiss() else pintar(movimiento)
                }
            }
        }
        return MaterialAlertDialogBuilder(requireContext()).setView(vista.root).create()
    }

    private fun pintar(movimiento: MovimientoVista) {
        val contexto = requireContext()
        vista.iconoCategoriaDetalle.setImageResource(RecursosCategoria.icono(movimiento.claveCategoria))
        ViewCompat.setBackgroundTintList(
            vista.iconoCategoriaDetalle,
            ColorStateList.valueOf(ContextCompat.getColor(contexto, RecursosCategoria.color(movimiento.claveCategoria))),
        )
        vista.textoCategoriaDetalle.text = movimiento.nombreCategoria
        vista.textoMontoDetalle.text = MontoConSigno.texto(movimiento.tipo, movimiento.montoCentavos)
        vista.textoMontoDetalle.setTextColor(ContextCompat.getColor(contexto, MontoConSigno.color(movimiento.tipo)))
        vista.textoTipoYFechaDetalle.text = getString(
            R.string.tipo_y_fecha,
            getString(MontoConSigno.nombreTipo(movimiento.tipo)),
            Fechas.texto(movimiento.fecha),
        )
        vista.textoDescripcionDetalle.text = movimiento.descripcion
    }

    companion object {
        const val ETIQUETA = "detalleMovimiento"
        private const val ARGUMENTO_ID = "idTransaccion"

        fun nuevo(idTransaccion: Long) = DetalleMovimientoDialogo().apply {
            arguments = Bundle().apply { putLong(ARGUMENTO_ID, idTransaccion) }
        }
    }
}
