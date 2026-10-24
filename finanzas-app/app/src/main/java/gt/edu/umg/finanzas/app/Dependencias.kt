package gt.edu.umg.finanzas.app

import android.content.Context
import gt.edu.umg.finanzas.app.datos.local.AppDatabase
import gt.edu.umg.finanzas.app.datos.local.InicializadorDatos
import gt.edu.umg.finanzas.app.datos.local.OperacionesMovimientoRoom
import gt.edu.umg.finanzas.app.datos.local.ReconstructorSaldoRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioCategoriaRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioTransaccionRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioUsuarioRoom
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas

/**
 * Construcción manual: aquí se decide qué implementación Room cumple cada contrato del core.
 * No contiene reglas ni SQL. Las pantallas solo reciben [servicio].
 */
class Dependencias(context: Context) {

    val baseDatos: AppDatabase = AppDatabase.crear(context)

    val inicializador = InicializadorDatos(baseDatos)

    val servicio = ServicioFinanzas(
        usuarios = RepositorioUsuarioRoom(baseDatos.usuarioDao()),
        categorias = RepositorioCategoriaRoom(baseDatos.categoriaDao()),
        transacciones = RepositorioTransaccionRoom(baseDatos.transaccionDao()),
        operaciones = OperacionesMovimientoRoom(baseDatos),
        reconstructor = ReconstructorSaldoRoom(baseDatos),
    )
}
