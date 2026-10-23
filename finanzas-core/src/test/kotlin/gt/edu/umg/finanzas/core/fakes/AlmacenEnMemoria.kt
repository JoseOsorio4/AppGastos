package gt.edu.umg.finanzas.core.fakes

import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.CategoriasIniciales
import gt.edu.umg.finanzas.core.dominio.Transaccion
import gt.edu.umg.finanzas.core.dominio.Usuario
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas
import kotlinx.coroutines.flow.MutableStateFlow

/** Las tres tablas en un solo valor inmutable. */
data class EstadoAlmacen(
    val usuario: Usuario,
    val categorias: List<Categoria>,
    val transacciones: Map<Long, Transaccion> = emptyMap(),
    val siguienteId: Long = 1,
) {
    fun categoria(id: Long): Categoria? = categorias.find { it.idCategoria == id }
}

/**
 * Doble de la base de datos que comparten todos los fakes. Reemplazar el estado completo
 * de una vez imita el COMMIT de withTransaction: se ve todo el cambio o nada.
 * Las pruebas corren en un solo hilo, por eso no se sincroniza.
 */
class AlmacenEnMemoria(inicial: EstadoAlmacen) {

    val estado = MutableStateFlow(inicial)

    val saldo: Long get() = estado.value.usuario.saldoCentavos
    val cantidadMovimientos: Int get() = estado.value.transacciones.size

    fun idCategoria(clave: String): Long = estado.value.categorias.first { it.clave == clave }.idCategoria

    fun crearServicio(): ServicioFinanzas = ServicioFinanzas(
        usuarios = RepositorioUsuarioEnMemoria(this),
        categorias = RepositorioCategoriaEnMemoria(this),
        transacciones = RepositorioTransaccionEnMemoria(this),
        operaciones = OperacionesMovimientoEnMemoria(this),
        reconstructor = ReconstructorSaldoEnMemoria(this),
    )

    companion object {
        /** Igual que el inicializador de la app: usuario 1 y las 12 categorías con IDs 1 a 12. */
        fun conCategoriasIniciales(saldoInicialCentavos: Long = 0): AlmacenEnMemoria {
            val categorias = CategoriasIniciales.lista.mapIndexed { indice, inicial ->
                Categoria(indice + 1L, inicial.clave, inicial.nombre, inicial.tipo)
            }
            val usuario = Usuario(Usuario.ID_UNICO, "Principal", saldoInicialCentavos)
            return AlmacenEnMemoria(EstadoAlmacen(usuario, categorias))
        }
    }
}
