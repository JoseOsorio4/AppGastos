package gt.edu.umg.finanzas.app.datos.local

import gt.edu.umg.finanzas.app.datos.local.dao.CategoriaDao
import gt.edu.umg.finanzas.core.contratos.RepositorioCategoria
import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepositorioCategoriaRoom(private val dao: CategoriaDao) : RepositorioCategoria {

    override fun observarActivasPorTipo(tipo: TipoMovimiento): Flow<List<Categoria>> =
        dao.observarActivasPorTipo(tipo).map { filas -> filas.map { it.aDominio() } }

    override suspend fun obtenerPorId(id: Long): Categoria? = dao.obtenerPorId(id)?.aDominio()
}
