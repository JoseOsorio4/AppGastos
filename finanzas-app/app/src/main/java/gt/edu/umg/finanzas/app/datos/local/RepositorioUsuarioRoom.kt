package gt.edu.umg.finanzas.app.datos.local

import gt.edu.umg.finanzas.app.datos.local.dao.UsuarioDao
import gt.edu.umg.finanzas.core.contratos.RepositorioUsuario
import gt.edu.umg.finanzas.core.dominio.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class RepositorioUsuarioRoom(private val dao: UsuarioDao) : RepositorioUsuario {

    // filterNotNull: antes de que InicializadorDatos cree al usuario no hay nada que mostrar.
    override fun observarUsuario(): Flow<Usuario> =
        dao.observarPorId(Usuario.ID_UNICO).filterNotNull().map { it.aDominio() }
}
