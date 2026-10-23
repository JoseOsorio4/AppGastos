package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.SegmentoGasto
import gt.edu.umg.finanzas.core.dominio.TotalCategoria
import java.math.BigInteger

/**
 * Porcentajes del anillo de gastos en puntos básicos que suman exactamente 10 000.
 * Usa el método del mayor residuo: primero la parte entera y luego reparte los puntos que faltan.
 */
object DistribucionGastos {

    const val PUNTOS_TOTALES = 10_000
    private val PUNTOS = BigInteger.valueOf(PUNTOS_TOTALES.toLong())

    /** Devuelve los segmentos del mayor al menor gasto; sin gastos, una lista vacía (no se dibuja anillo). */
    fun calcular(totales: List<TotalCategoria>): List<SegmentoGasto> {
        // sortedByDescending es estable: los empates conservan el orden recibido.
        val conGasto = totales.filter { it.totalCentavos > 0 }.sortedByDescending { it.totalCentavos }
        if (conGasto.isEmpty()) return emptyList()

        // BigInteger: total × 10 000 podría no caber en Long.
        val suma = conGasto.fold(BigInteger.ZERO) { acumulado, total -> acumulado + BigInteger.valueOf(total.totalCentavos) }
        val partes = conGasto.map { total ->
            val (cociente, residuo) = BigInteger.valueOf(total.totalCentavos).multiply(PUNTOS).divideAndRemainder(suma)
            Parte(total, cociente.toInt(), residuo)
        }

        val faltantes = PUNTOS_TOTALES - partes.sumOf { it.puntos }
        val reciben = partes.indices
            .sortedWith(compareByDescending<Int> { partes[it].residuo }.thenBy { it })
            .take(faltantes)
            .toSet()

        return partes.mapIndexed { indice, parte ->
            SegmentoGasto(
                clave = parte.total.clave,
                nombre = parte.total.nombre,
                totalCentavos = parte.total.totalCentavos,
                puntosBasicos = parte.puntos + if (indice in reciben) 1 else 0,
            )
        }
    }

    private class Parte(val total: TotalCategoria, val puntos: Int, val residuo: BigInteger)
}
