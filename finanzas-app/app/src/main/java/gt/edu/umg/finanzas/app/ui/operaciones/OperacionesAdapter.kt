package gt.edu.umg.finanzas.app.ui.operaciones

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ItemEncabezadoDiaBinding
import gt.edu.umg.finanzas.app.databinding.ItemOperacionBinding
import gt.edu.umg.finanzas.app.ui.comun.Fechas
import gt.edu.umg.finanzas.app.ui.comun.MontoConSigno
import gt.edu.umg.finanzas.app.ui.comun.RecursosCategoria

/** Dibuja encabezados de día y filas de movimiento; avisa qué movimiento tocó el usuario. */
class OperacionesAdapter(
    private val alTocar: (idTransaccion: Long) -> Unit,
) : ListAdapter<FilaOperacion, RecyclerView.ViewHolder>(Diferencias) {

    private class Encabezado(val binding: ItemEncabezadoDiaBinding) : RecyclerView.ViewHolder(binding.root)
    private class Fila(val binding: ItemOperacionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(posicion: Int): Int = when (getItem(posicion)) {
        is FilaOperacion.EncabezadoDia -> TIPO_ENCABEZADO
        is FilaOperacion.Movimiento -> TIPO_MOVIMIENTO
    }

    override fun onCreateViewHolder(padre: ViewGroup, tipoVista: Int): RecyclerView.ViewHolder {
        val inflador = LayoutInflater.from(padre.context)
        return if (tipoVista == TIPO_ENCABEZADO) {
            Encabezado(ItemEncabezadoDiaBinding.inflate(inflador, padre, false))
        } else {
            Fila(ItemOperacionBinding.inflate(inflador, padre, false))
        }
    }

    override fun onBindViewHolder(celda: RecyclerView.ViewHolder, posicion: Int) {
        when (val fila = getItem(posicion)) {
            is FilaOperacion.EncabezadoDia -> (celda as Encabezado).binding.textoFechaDia.text = Fechas.textoDia(fila.fecha)
            is FilaOperacion.Movimiento -> pintarMovimiento(celda as Fila, fila)
        }
    }

    private fun pintarMovimiento(celda: Fila, fila: FilaOperacion.Movimiento) {
        val movimiento = fila.vista
        val contexto = celda.itemView.context
        val tipo = contexto.getString(MontoConSigno.nombreTipo(movimiento.tipo))
        val monto = MontoConSigno.texto(movimiento.tipo, movimiento.montoCentavos)
        with(celda.binding) {
            iconoCategoria.setImageResource(RecursosCategoria.icono(movimiento.claveCategoria))
            ViewCompat.setBackgroundTintList(
                iconoCategoria,
                ColorStateList.valueOf(ContextCompat.getColor(contexto, RecursosCategoria.color(movimiento.claveCategoria))),
            )
            textoDescripcion.text = movimiento.descripcion
            textoCategoriaYTipo.text = contexto.getString(R.string.categoria_y_tipo, movimiento.nombreCategoria, tipo)
            textoMonto.text = monto
            textoMonto.setTextColor(ContextCompat.getColor(contexto, MontoConSigno.color(movimiento.tipo)))
            // TalkBack lee la fila completa, incluida la fecha que en pantalla está en el encabezado.
            root.contentDescription = contexto.getString(
                R.string.descripcion_fila_operacion,
                tipo, movimiento.descripcion, movimiento.nombreCategoria, monto, Fechas.texto(movimiento.fecha),
            )
            root.setOnClickListener { alTocar(movimiento.idTransaccion) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<FilaOperacion>() {
        override fun areItemsTheSame(antes: FilaOperacion, despues: FilaOperacion): Boolean = when {
            antes is FilaOperacion.EncabezadoDia && despues is FilaOperacion.EncabezadoDia -> antes.fecha == despues.fecha
            antes is FilaOperacion.Movimiento && despues is FilaOperacion.Movimiento ->
                antes.vista.idTransaccion == despues.vista.idTransaccion
            else -> false
        }

        override fun areContentsTheSame(antes: FilaOperacion, despues: FilaOperacion): Boolean = antes == despues
    }

    private companion object {
        const val TIPO_ENCABEZADO = 0
        const val TIPO_MOVIMIENTO = 1
    }
}
