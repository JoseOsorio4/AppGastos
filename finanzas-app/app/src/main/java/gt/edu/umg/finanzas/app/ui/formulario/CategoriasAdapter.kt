package gt.edu.umg.finanzas.app.ui.formulario

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.databinding.ItemCategoriaBinding
import gt.edu.umg.finanzas.app.ui.comun.RecursosCategoria
import gt.edu.umg.finanzas.core.dominio.Categoria

/** Cuadrícula de categorías: dibuja cada celda y avisa qué ID tocó el usuario. No guarda la selección. */
class CategoriasAdapter(
    private val alElegir: (Long) -> Unit,
) : RecyclerView.Adapter<CategoriasAdapter.Celda>() {

    private var categorias: List<Categoria> = emptyList()
    private var idElegida: Long? = null

    class Celda(val binding: ItemCategoriaBinding) : RecyclerView.ViewHolder(binding.root)

    // Son a lo sumo 8 celdas por tipo: redibujarlas todas es más simple que calcular diferencias.
    @SuppressLint("NotifyDataSetChanged")
    fun mostrar(categorias: List<Categoria>, idElegida: Long?) {
        if (categorias == this.categorias && idElegida == this.idElegida) return
        this.categorias = categorias
        this.idElegida = idElegida
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = categorias.size

    override fun onCreateViewHolder(padre: ViewGroup, tipoVista: Int): Celda =
        Celda(ItemCategoriaBinding.inflate(LayoutInflater.from(padre.context), padre, false))

    override fun onBindViewHolder(celda: Celda, posicion: Int) {
        val categoria = categorias[posicion]
        val contexto = celda.itemView.context
        val elegida = categoria.idCategoria == idElegida
        val colorCategoria = ContextCompat.getColor(contexto, RecursosCategoria.color(categoria.clave))

        with(celda.binding) {
            iconoCategoria.setImageResource(RecursosCategoria.icono(categoria.clave))
            // Elegida: círculo con el color de la categoría. Sin elegir: círculo suave e icono de color.
            val fondo = if (elegida) colorCategoria else ContextCompat.getColor(contexto, R.color.color_primario_contenedor)
            val icono = if (elegida) ContextCompat.getColor(contexto, android.R.color.white) else colorCategoria
            ViewCompat.setBackgroundTintList(iconoCategoria, ColorStateList.valueOf(fondo))
            ImageViewCompat.setImageTintList(iconoCategoria, ColorStateList.valueOf(icono))
            textoNombre.text = categoria.nombre
            textoNombre.setTypeface(null, if (elegida) Typeface.BOLD else Typeface.NORMAL)
            root.isSelected = elegida
            root.contentDescription =
                if (elegida) contexto.getString(R.string.categoria_seleccionada, categoria.nombre) else categoria.nombre
            root.setOnClickListener { alElegir(categoria.idCategoria) }
        }
    }
}
