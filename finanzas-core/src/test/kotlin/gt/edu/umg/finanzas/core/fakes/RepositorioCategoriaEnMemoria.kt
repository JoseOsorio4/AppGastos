package gt.edu.umg.finanzas.core.fakes

import gt.edu.umg.finanzas.core.contratos.RepositorioCategoria
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepositorioCategoriaEnMemoria(private val almacen: AlmacenEnMemoria) : RepositorioCategoria {

    override fun observarActivasPorTipo(tipo: TipoMovimiento): Flow<List<Categoria>> =
        almacen.estado.map { estado -> estado.categorias.filter { it.activa && it.tipo == tipo } }

    override suspend fun obtenerPorId(id: Long): Categoria? = almacen.estado.value.categoria(id)
}
