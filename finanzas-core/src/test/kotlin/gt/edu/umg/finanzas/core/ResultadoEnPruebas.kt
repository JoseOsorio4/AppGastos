package gt.edu.umg.finanzas.core

import gt.edu.umg.finanzas.core.resultado.Resultado
import kotlin.test.fail

/** Extrae el valor de un Ok o hace fallar la prueba mostrando qué llegó. */
fun <T> Resultado<T>.esperarOk(): T = when (this) {
    is Resultado.Ok -> valor
    is Resultado.Fallo -> fail("Se esperaba Ok y llegó $this")
}

fun Resultado<*>.esperarErrorNegocio(): Resultado.ErrorNegocio =
    this as? Resultado.ErrorNegocio ?: fail("Se esperaba ErrorNegocio y llegó $this")
