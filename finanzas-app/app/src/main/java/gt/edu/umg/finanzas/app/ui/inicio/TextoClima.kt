package gt.edu.umg.finanzas.app.ui.inicio

import android.content.Context
import gt.edu.umg.finanzas.app.R
import gt.edu.umg.finanzas.app.datos.repositorios.EstadoClima
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TextoClima {
    private val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val zona = ZoneId.of("America/Guatemala")

    fun valor(contexto: Context, estado: EstadoClima): String = estado.dato?.clima?.let {
        contexto.getString(R.string.clima_valor, it.temperaturaC, contexto.getString(condicion(it.codigoWmo)))
    } ?: contexto.getString(if (estado.cargando) R.string.clima_cargando else R.string.clima_sin_datos)

    fun detalle(contexto: Context, estado: EstadoClima): String = buildList {
        estado.dato?.let {
            add(contexto.getString(if (estado.esGuardado) R.string.clima_guardado else R.string.clima_actualizado))
            add(contexto.getString(R.string.clima_fecha, formato.format(it.clima.medidoEn),
                formato.format(it.consultadoEn.atZone(zona))))
        }
        if (estado.cargando && estado.dato != null) add(contexto.getString(R.string.clima_cargando))
        if (estado.error) add(contexto.getString(R.string.clima_error))
    }.joinToString("\n")

    private fun condicion(codigo: Int): Int = when (codigo) {
        0 -> R.string.clima_despejado
        1 -> R.string.clima_casi_despejado
        2 -> R.string.clima_parcial_nublado
        3 -> R.string.clima_nublado
        45, 48 -> R.string.clima_niebla
        51, 53, 55 -> R.string.clima_llovizna
        56, 57 -> R.string.clima_llovizna_helada
        61, 63, 65 -> R.string.clima_lluvia
        66, 67 -> R.string.clima_lluvia_helada
        71, 73, 75, 77 -> R.string.clima_nieve
        80, 81, 82 -> R.string.clima_chubascos
        85, 86 -> R.string.clima_chubascos_nieve
        95, 97 -> R.string.clima_tormenta
        96, 99 -> R.string.clima_tormenta_granizo
        else -> R.string.clima_desconocido
    }
}
