package gt.edu.umg.finanzas.app.datos.local

import androidx.room.TypeConverter
import java.math.BigDecimal

class Convertidores {
    @TypeConverter
    fun aTexto(valor: BigDecimal): String = valor.toPlainString()

    @TypeConverter
    fun aDecimal(texto: String): BigDecimal = BigDecimal(texto)
}
