package gt.edu.umg.finanzas.core.contratos

import gt.edu.umg.finanzas.core.dominio.Usuario
import kotlinx.coroutines.flow.Flow

/** Lectura del único usuario. El saldo solo se escribe desde OperacionesMovimiento o ReconstructorSaldo. */
interface RepositorioUsuario {
    fun observarUsuario(): Flow<Usuario>
}
