package gt.edu.umg.finanzas.app.datos.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.dominio.Usuario
import gt.edu.umg.finanzas.core.resultado.Resultado
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas
import org.junit.Assert.fail
import java.time.LocalDate

/** Utilidades compartidas por las pruebas de Room. */
object BaseDePrueba {

    val hoy: LocalDate = LocalDate.of(2026, 10, 22)

    val contexto: Context get() = ApplicationProvider.getApplicationContext()

    /** Base en memoria: cada prueba empieza vacía y no deja archivos en el emulador. */
    fun enMemoria(): AppDatabase =
        Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()

    /** Recorrido completo servicio → contrato → Room, armado igual que en Dependencias. */
    fun servicioSobre(baseDatos: AppDatabase): ServicioFinanzas = ServicioFinanzas(
        usuarios = RepositorioUsuarioRoom(baseDatos.usuarioDao()),
        categorias = RepositorioCategoriaRoom(baseDatos.categoriaDao()),
        transacciones = RepositorioTransaccionRoom(baseDatos.transaccionDao()),
        operaciones = OperacionesMovimientoRoom(baseDatos),
        reconstructor = ReconstructorSaldoRoom(baseDatos),
    )

    suspend fun idCategoria(baseDatos: AppDatabase, clave: String): Long =
        baseDatos.categoriaDao().obtenerTodas().first { it.clave == clave }.idCategoria

    suspend fun datos(
        baseDatos: AppDatabase,
        tipo: TipoMovimiento,
        monto: String,
        clave: String,
        fecha: LocalDate = hoy,
    ) = DatosMovimiento(tipo, monto, Moneda.GTQ, null, idCategoria(baseDatos, clave), "Movimiento de prueba", fecha)

    suspend fun ingreso(baseDatos: AppDatabase, monto: String, clave: String = "SALARIO", fecha: LocalDate = hoy) =
        datos(baseDatos, TipoMovimiento.INGRESO, monto, clave, fecha)

    suspend fun gasto(baseDatos: AppDatabase, monto: String, clave: String = "ALIMENTACION", fecha: LocalDate = hoy) =
        datos(baseDatos, TipoMovimiento.GASTO, monto, clave, fecha)

    suspend fun saldo(baseDatos: AppDatabase): Long =
        checkNotNull(baseDatos.usuarioDao().obtenerPorId(Usuario.ID_UNICO)).saldoCentavos

    fun <T> Resultado<T>.esperarOk(): T = when (this) {
        is Resultado.Ok -> valor
        is Resultado.Fallo -> fail("Se esperaba Ok y llegó $this").let { error("inalcanzable") }
    }
}
