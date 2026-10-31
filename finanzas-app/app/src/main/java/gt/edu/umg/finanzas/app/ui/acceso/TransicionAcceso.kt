package gt.edu.umg.finanzas.app.ui.acceso

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import gt.edu.umg.finanzas.app.R

/** Solo anima la presentación; el acceso nunca espera a que termine. */
class TransicionAcceso(private val vistas: List<View>) {
    private var animacion: AnimatorSet? = null

    fun mostrar() {
        detener()
        val contexto = vistas.first().context
        val habilitadas = if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
        else Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        if (!habilitadas) return
        val distancia = contexto.resources.getDimension(R.dimen.acceso_desplazamiento)
        animacion = AnimatorSet().apply {
            playTogether(vistas.filter { it.visibility == View.VISIBLE }.flatMap {
                listOf(ObjectAnimator.ofFloat(it, View.ALPHA, 0.3f, 1f),
                    ObjectAnimator.ofFloat(it, View.TRANSLATION_Y, distancia, 0f))
            })
            duration = contexto.resources.getInteger(R.integer.acceso_duracion).toLong()
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun detener() {
        animacion?.cancel()
        animacion = null
        vistas.forEach { it.alpha = 1f; it.translationY = 0f }
    }
}
