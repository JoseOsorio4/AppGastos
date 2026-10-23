package gt.edu.umg.finanzas.core.dominio

/** Categoría antes de insertarse: todavía no tiene ID. */
data class CategoriaInicial(
    val clave: String,
    val nombre: String,
    val tipo: TipoMovimiento,
)

/** Las 12 categorías que siembra el inicializador de datos. La lista vive aquí para probarla sin Android. */
object CategoriasIniciales {

    val lista: List<CategoriaInicial> = listOf(
        CategoriaInicial("ALIMENTACION", "Alimentación", TipoMovimiento.GASTO),
        CategoriaInicial("TRANSPORTE", "Transporte", TipoMovimiento.GASTO),
        CategoriaInicial("VIVIENDA", "Vivienda", TipoMovimiento.GASTO),
        CategoriaInicial("SERVICIOS", "Servicios", TipoMovimiento.GASTO),
        CategoriaInicial("SALUD", "Salud", TipoMovimiento.GASTO),
        CategoriaInicial("EDUCACION", "Educación", TipoMovimiento.GASTO),
        CategoriaInicial("ENTRETENIMIENTO", "Entretenimiento", TipoMovimiento.GASTO),
        CategoriaInicial("OTROS_GASTOS", "Otros gastos", TipoMovimiento.GASTO),
        CategoriaInicial("SALARIO", "Salario", TipoMovimiento.INGRESO),
        CategoriaInicial("NEGOCIO", "Negocio", TipoMovimiento.INGRESO),
        CategoriaInicial("REGALO", "Regalo", TipoMovimiento.INGRESO),
        CategoriaInicial("OTROS_INGRESOS", "Otros ingresos", TipoMovimiento.INGRESO),
    )
}
