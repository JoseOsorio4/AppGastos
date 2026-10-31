package gt.edu.umg.finanzas.app

import android.content.Context
import gt.edu.umg.finanzas.app.datos.local.AppDatabase
import gt.edu.umg.finanzas.app.datos.local.InicializadorDatos
import gt.edu.umg.finanzas.app.datos.local.OperacionesMovimientoRoom
import gt.edu.umg.finanzas.app.datos.local.ReconstructorSaldoRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioCategoriaRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioTransaccionRoom
import gt.edu.umg.finanzas.app.datos.local.RepositorioUsuarioRoom
import gt.edu.umg.finanzas.app.datos.preferencias.CacheServicios
import gt.edu.umg.finanzas.app.datos.preferencias.PreferenciaTema
import gt.edu.umg.finanzas.app.datos.repositorios.RepositorioTipoCambio
import gt.edu.umg.finanzas.app.datos.repositorios.RepositorioClima
import gt.edu.umg.finanzas.app.datos.repositorios.RepositorioReflexion
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas

/**
 * Construcción manual: aquí se decide qué implementación Room cumple cada contrato del core.
 * No contiene reglas ni SQL. Las fábricas de ViewModel toman de aquí el servicio y las preferencias.
 */
class Dependencias(context: Context) {

    val bloqueo = gt.edu.umg.finanzas.app.datos.preferencias.BloqueoLocal(context.noBackupFilesDir)

    val preferenciaTema = PreferenciaTema(context)
    val cacheServicios = CacheServicios(context)
    val tipoCambio = RepositorioTipoCambio(cacheServicios)
    val clima = RepositorioClima(cacheServicios)
    val reflexion = RepositorioReflexion(cacheServicios)

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
