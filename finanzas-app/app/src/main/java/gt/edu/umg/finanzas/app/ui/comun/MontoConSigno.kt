package gt.edu.umg.finanzas.app.ui.comun

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.reglas.Dinero

/**
 * Cómo se muestra el importe de un movimiento: ingreso con "+" en verde, gasto con "−" en rojo.
 * El signo y la palabra Ingreso/Gasto acompañan al color, que nunca es la única pista.
 */
object MontoConSigno {

    fun texto(tipo: TipoMovimiento, centavos: Long): String = when (tipo) {
        TipoMovimiento.INGRESO -> "+${Dinero.formatear(centavos)}"
        TipoMovimiento.GASTO -> "-${Dinero.formatear(centavos)}"
    }

    @ColorRes
    fun color(tipo: TipoMovimiento): Int = when (tipo) {
        TipoMovimiento.INGRESO -> R.color.color_ingreso
        TipoMovimiento.GASTO -> R.color.color_gasto
    }

    @StringRes
    fun nombreTipo(tipo: TipoMovimiento): Int = when (tipo) {
        TipoMovimiento.INGRESO -> R.string.tipo_ingreso
        TipoMovimiento.GASTO -> R.string.tipo_gasto
    }
}
