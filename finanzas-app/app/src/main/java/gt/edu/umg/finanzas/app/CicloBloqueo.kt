package gt.edu.umg.finanzas.app

import android.app.Activity
import android.app.Application
import android.os.Bundle

class CicloBloqueo(private val app: FinanzasApp) : Application.ActivityLifecycleCallbacks {
    private var visibles = 0
    override fun onActivityStarted(activity: Activity) { visibles++ }
    override fun onActivityStopped(activity: Activity) {
        visibles--
        if (visibles == 0 && !activity.isChangingConfigurations) app.dependencias.bloqueo.bloquear()
    }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
