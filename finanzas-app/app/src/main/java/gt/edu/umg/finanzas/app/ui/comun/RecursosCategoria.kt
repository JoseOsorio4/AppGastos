package gt.edu.umg.finanzas.app.ui.comun

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import gt.edu.umg.finanzas.app.R

/**
 * Clave de categoría → icono y color. Una clave sin recurso (por ejemplo, una categoría
 * agregada más adelante) usa el icono genérico; el movimiento y su nombre no cambian.
 */
object RecursosCategoria {

    @DrawableRes
    fun icono(clave: String): Int = when (clave) {
        "ALIMENTACION" -> R.drawable.ic_categoria_alimentacion
        "TRANSPORTE" -> R.drawable.ic_categoria_transporte
        "VIVIENDA" -> R.drawable.ic_categoria_vivienda
        "SERVICIOS" -> R.drawable.ic_categoria_servicios
        "SALUD" -> R.drawable.ic_categoria_salud
        "EDUCACION" -> R.drawable.ic_categoria_educacion
        "ENTRETENIMIENTO" -> R.drawable.ic_categoria_entretenimiento
        "OTROS_GASTOS", "OTROS_INGRESOS" -> R.drawable.ic_categoria_otros
        "SALARIO" -> R.drawable.ic_categoria_salario
        "NEGOCIO" -> R.drawable.ic_categoria_negocio
        "REGALO" -> R.drawable.ic_categoria_regalo
        else -> R.drawable.ic_categoria_generica
    }

    @ColorRes
    fun color(clave: String): Int = when (clave) {
        "ALIMENTACION" -> R.color.color_categoria_alimentacion
        "TRANSPORTE" -> R.color.color_categoria_transporte
        "VIVIENDA" -> R.color.color_categoria_vivienda
        "SERVICIOS" -> R.color.color_categoria_servicios
        "SALUD" -> R.color.color_categoria_salud
        "EDUCACION" -> R.color.color_categoria_educacion
        "ENTRETENIMIENTO" -> R.color.color_categoria_entretenimiento
        "OTROS_GASTOS" -> R.color.color_categoria_otros_gastos
        "SALARIO" -> R.color.color_categoria_salario
        "NEGOCIO" -> R.color.color_categoria_negocio
        "REGALO" -> R.color.color_categoria_regalo
        "OTROS_INGRESOS" -> R.color.color_categoria_otros_ingresos
        else -> R.color.color_categoria_generica
    }
}
