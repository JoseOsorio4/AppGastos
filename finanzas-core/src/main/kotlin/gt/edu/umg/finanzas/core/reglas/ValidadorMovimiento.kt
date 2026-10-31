package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.ConversionAplicada
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Gasto
import gt.edu.umg.finanzas.core.dominio.Ingreso
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.resultado.Resultado
import java.time.LocalDate

/**
 * Única puerta entre los datos crudos del formulario y una [Transaccion].
 * Se llama dentro de la operación de guardado, con la categoría leída en esa misma transacción.
 */
object ValidadorMovimiento {

    /**
     * [anterior] conserva la categoría archivada y la fotografía USD dentro de la misma operación.
     */
    fun validar(
        datos: DatosMovimiento,
        categoria: Categoria,
        hoy: LocalDate,
        anterior: Transaccion? = null,
    ): Resultado<Transaccion> {
        require(categoria.idCategoria == datos.idCategoria) { "Se recibió otra categoría que la elegida" }

        if (categoria.tipo != datos.tipo) {
            return rechazo("La categoría no corresponde al tipo de movimiento", CampoMovimiento.CATEGORIA)
        }
        if (!categoria.activa && categoria.idCategoria != anterior?.idCategoria) {
            return rechazo("Esa categoría ya no está disponible", CampoMovimiento.CATEGORIA)
        }

        val montoOriginal = when (val monto = Dinero.desdeTexto(datos.montoTexto)) {
            is Resultado.Ok -> monto.valor
            is Resultado.ErrorNegocio -> return monto.copy(campo = CampoMovimiento.MONTO)
            is Resultado.Fallo -> return monto
        }

        // En GTQ no hay conversión: una tasa recibida se ignora.
        val conversion = when (datos.moneda) {
            Moneda.GTQ -> null
            Moneda.USD -> ConversionAplicada(
                montoOriginal,
                anterior?.conversion?.tasa ?: datos.tasa
                    ?: return rechazo("Sin tasa disponible. Conéctate para obtenerla", CampoMovimiento.MONEDA),
                ConversorMoneda.recargoPara(datos.tipo, anterior?.tipo, anterior?.conversion?.recargoGtqPorUsd),
            )
        }

        // El monto base en Q se calcula aquí, con la tasa recibida; la pantalla no lo envía.
        val montoCentavos = when (val base = montoEnQuetzales(montoOriginal, conversion)) {
            is Resultado.Ok -> base.valor
            is Resultado.Fallo -> return base
        }

        if (datos.fecha.isAfter(hoy)) {
            return rechazo("La fecha no puede ser futura", CampoMovimiento.FECHA)
        }

        val descripcion = datos.descripcion.trim()
        if (descripcion.isEmpty()) {
            return rechazo("Escribe una descripción", CampoMovimiento.DESCRIPCION)
        }
        if (descripcion.length > Transaccion.DESCRIPCION_MAXIMA) {
            return rechazo(
                "La descripción admite hasta ${Transaccion.DESCRIPCION_MAXIMA} caracteres",
                CampoMovimiento.DESCRIPCION,
            )
        }

        val transaccion = when (datos.tipo) {
            TipoMovimiento.INGRESO ->
                Ingreso(Transaccion.SIN_ID, categoria.idCategoria, montoCentavos, descripcion, datos.fecha, conversion)
            TipoMovimiento.GASTO ->
                Gasto(Transaccion.SIN_ID, categoria.idCategoria, montoCentavos, descripcion, datos.fecha, conversion)
        }
        return Resultado.Ok(transaccion)
    }

    // El límite por movimiento también se aplica al equivalente en Q de un monto USD.
    private fun montoEnQuetzales(montoOriginal: Long, conversion: ConversionAplicada?): Resultado<Long> {
        if (conversion == null) return Resultado.Ok(montoOriginal)
        val enQuetzales = try {
            ConversorMoneda.usdAGtq(conversion)
        } catch (e: ArithmeticException) {
            Long.MAX_VALUE
        }
        return when {
            enQuetzales > Transaccion.MONTO_MAXIMO_CENTAVOS ->
                rechazo("El equivalente en quetzales supera el máximo por movimiento", CampoMovimiento.MONTO)
            enQuetzales < 1 ->
                rechazo("El equivalente en quetzales es menor que Q 0.01", CampoMovimiento.MONTO)
            else -> Resultado.Ok(enQuetzales)
        }
    }

    private fun rechazo(mensaje: String, campo: CampoMovimiento) = Resultado.ErrorNegocio(mensaje, campo)
}
