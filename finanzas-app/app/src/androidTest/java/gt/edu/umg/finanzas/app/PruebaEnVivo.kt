package gt.edu.umg.finanzas.app

/** Se ejecuta solo al pasar -PpruebasEnVivo; la suite habitual usa respuestas guardadas. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class PruebaEnVivo
