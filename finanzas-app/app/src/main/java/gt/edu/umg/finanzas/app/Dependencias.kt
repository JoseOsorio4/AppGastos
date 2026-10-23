package gt.edu.umg.finanzas.app

import android.content.Context
import gt.edu.umg.finanzas.app.datos.local.AppDatabase
import gt.edu.umg.finanzas.app.datos.local.InicializadorDatos
import gt.edu.umg.finanzas.app.datos.local.OperacionesMovimientoRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioCategoriaRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioTransaccionRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioUsuarioRoom
import gt.edu.umg.finanzas.core.contratos.OperacionesMovimiento
import gt.edu.umg.finanzas.core.contratos.RepositorioCategoria
import gt.edu.umg.finanzas.core.contratos.RepositorioTransaccion
import gt.edu.umg.finanzas.core.contratos.RepositorioUsuario

/**
 * Construcción manual: aquí se decide qué implementación Room cumple cada contrato del core.
 * No contiene reglas ni SQL. ServicioFinanzas se arma en el Inc. 3, con el primer ViewModel.
 */
class Dependencias(context: Context) {

    val baseDatos: AppDatabase = AppDatabase.crear(context)

    val repositorioUsuario: RepositorioUsuario = RepositorioUsuarioRoom(baseDatos.usuarioDao())
    val repositorioCategoria: RepositorioCategoria = RepositorioCategoriaRoom(baseDatos.categoriaDao())
    val repositorioTransaccion: RepositorioTransaccion = RepositorioTransaccionRoom(baseDatos.transaccionDao())
    val operacionesMovimiento: OperacionesMovimiento = OperacionesMovimientoRoom(baseDatos)
    val inicializador = InicializadorDatos(baseDatos)
}
