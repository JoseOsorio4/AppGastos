package gt.edu.umg.finanzas.core.fakes

import gt.edu.umg.finanzas.core.contratos.RepositorioUsuario
import gt.edu.umg.finanzas.core.dominio.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RepositorioUsuarioEnMemoria(private val almacen: AlmacenEnMemoria) : RepositorioUsuario {

    override fun observarUsuario(): Flow<Usuario> =
        almacen.estado.map { it.usuario }.distinctUntilChanged()
}
