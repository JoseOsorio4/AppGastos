package gt.edu.umg.finanzas.core.contratos

import gt.edu.umg.finanzas.core.dominio.Categoria
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import kotlinx.coroutines.flow.Flow

interface RepositorioCategoria {
    /** Categorías que se ofrecen en la cuadrícula del formulario. */
    fun observarActivasPorTipo(tipo: TipoMovimiento): Flow<List<Categoria>>

    /** Incluye las archivadas: un movimiento antiguo puede seguir usándolas. */
    suspend fun obtenerPorId(id: Long): Categoria?
}
