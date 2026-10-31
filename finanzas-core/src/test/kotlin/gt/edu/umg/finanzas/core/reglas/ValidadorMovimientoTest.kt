package gt.edu.umg.finanzas.core.reglas

import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Gasto
import gt.edu.umg.finanzas.core.dominio.Ingreso
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.esperarErrorNegocio
import gt.edu.umg.finanzas.core.esperarOk
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ValidadorMovimientoTest {

    private val hoy = LocalDate.of(2026, 10, 22)
    private val alimentacion = Categoria(1, "ALIMENTACION", "Alimentación", TipoMovimiento.GASTO)
    private val salario = Categoria(9, "SALARIO", "Salario", TipoMovimiento.INGRESO)
    private val tasa = TasaCambio(BigDecimal("7.66000"), LocalDate.of(2026, 10, 21), "Prueba")

    private fun datos(
        tipo: TipoMovimiento = TipoMovimiento.GASTO,
        monto: String = "30",
        moneda: Moneda = Moneda.GTQ,
        tasa: TasaCambio? = null,
        categoria: Categoria = alimentacion,
        descripcion: String = "Almuerzo",
        fecha: LocalDate = hoy,
    ) = DatosMovimiento(tipo, monto, moneda, tasa, categoria.idCategoria, descripcion, fecha)

    @Test
    fun `gasto valido en GTQ construye un Gasto en centavos`() {
        val gasto = ValidadorMovimiento.validar(datos(descripcion = "  Almuerzo  "), alimentacion, hoy).esperarOk()

        assertIs<Gasto>(gasto)
        assertEquals(3_000L, gasto.montoCentavos)
        assertEquals(-3_000L, gasto.impactoEnSaldo())
        assertEquals("Almuerzo", gasto.descripcion)
        assertNull(gasto.conversion)
    }

    @Test
    fun `ingreso valido construye un Ingreso`() {
        val ingreso = ValidadorMovimiento.validar(
            datos(tipo = TipoMovimiento.INGRESO, monto = "100", categoria = salario),
            salario,
            hoy,
        ).esperarOk()

        assertIs<Ingreso>(ingreso)
        assertEquals(10_000L, ingreso.impactoEnSaldo())
    }

    @Test
    fun `GTQ ignora una tasa recibida`() {
        val gasto = ValidadorMovimiento.validar(datos(tasa = tasa), alimentacion, hoy).esperarOk()
        assertEquals(3_000L, gasto.montoCentavos)
        assertNull(gasto.conversion)
    }

    @Test
    fun `caso e USD calcula el monto base y guarda la fotografia de la tasa`() {
        val gasto = ValidadorMovimiento.validar(datos(monto = "10.00", moneda = Moneda.USD, tasa = tasa), alimentacion, hoy)
            .esperarOk()

        assertEquals(7_960L, gasto.montoCentavos)
        val conversion = checkNotNull(gasto.conversion)
        assertEquals(1_000L, conversion.montoOriginalCentavos)
        assertEquals("7.66000", conversion.tasa.gtqPorUsd.toPlainString())
    }

    @Test
    fun `USD sin tasa se rechaza en el campo moneda`() {
        val error = ValidadorMovimiento.validar(datos(moneda = Moneda.USD), alimentacion, hoy).esperarErrorNegocio()
        assertEquals(CampoMovimiento.MONEDA, error.campo)
    }

    @Test
    fun `USD cuyo equivalente en Q supera el maximo se rechaza`() {
        val error = ValidadorMovimiento.validar(datos(monto = "9999999.99", moneda = Moneda.USD, tasa = tasa), alimentacion, hoy)
            .esperarErrorNegocio()
        assertEquals(CampoMovimiento.MONTO, error.campo)
    }

    @Test
    fun `monto invalido marca el campo monto con el mensaje de Dinero`() {
        val error = ValidadorMovimiento.validar(datos(monto = "12,50"), alimentacion, hoy).esperarErrorNegocio()
        assertEquals(CampoMovimiento.MONTO, error.campo)
        assertEquals("Usa punto para los decimales", error.mensaje)
    }

    @Test
    fun `categoria de otro tipo se rechaza`() {
        val error = ValidadorMovimiento.validar(
            datos(tipo = TipoMovimiento.INGRESO, categoria = alimentacion),
            alimentacion,
            hoy,
        ).esperarErrorNegocio()
        assertEquals(CampoMovimiento.CATEGORIA, error.campo)
    }

    @Test
    fun `categoria archivada solo se acepta si el movimiento ya la tenia`() {
        val archivada = alimentacion.copy(activa = false)

        ValidadorMovimiento.validar(datos(), archivada, hoy).esperarErrorNegocio()
        val anterior = Gasto(1, archivada.idCategoria, 3000, "Anterior", hoy)
        ValidadorMovimiento.validar(datos(), archivada, hoy, anterior).esperarOk()
    }

    @Test
    fun `fecha futura se rechaza y la de hoy se acepta`() {
        val error = ValidadorMovimiento.validar(datos(fecha = hoy.plusDays(1)), alimentacion, hoy).esperarErrorNegocio()
        assertEquals(CampoMovimiento.FECHA, error.campo)

        ValidadorMovimiento.validar(datos(fecha = hoy), alimentacion, hoy).esperarOk()
    }

    @Test
    fun `descripcion obligatoria de 1 a 60 caracteres`() {
        assertEquals(
            CampoMovimiento.DESCRIPCION,
            ValidadorMovimiento.validar(datos(descripcion = "   "), alimentacion, hoy).esperarErrorNegocio().campo,
        )
        assertEquals(
            CampoMovimiento.DESCRIPCION,
            ValidadorMovimiento.validar(datos(descripcion = "a".repeat(61)), alimentacion, hoy).esperarErrorNegocio().campo,
        )
        ValidadorMovimiento.validar(datos(descripcion = "a".repeat(60)), alimentacion, hoy).esperarOk()
    }
}
