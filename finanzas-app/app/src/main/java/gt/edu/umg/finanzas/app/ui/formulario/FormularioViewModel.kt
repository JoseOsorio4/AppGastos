package gt.edu.umg.finanzas.app.ui.formulario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.reglas.Dinero
import gt.edu.umg.finanzas.core.resultado.Resultado
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Estado del formulario. Recibe cada cambio de campo, los copia a SavedStateHandle y, al guardar,
 * envía los datos crudos al servicio: la validación y el cálculo del saldo los hace el core.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioViewModel(
    private val servicio: ServicioFinanzas,
    private val estadoGuardado: SavedStateHandle,
    private val hoy: () -> LocalDate = { Periodo.hoy() },
) : ViewModel() {

    private val _estado = MutableStateFlow(leerCampos())
    val estado: StateFlow<FormularioUiState> = _estado.asStateFlow()

    init {
        // Las categorías se leen de Room según el tipo elegido; no hace falta guardarlas en SavedStateHandle.
        viewModelScope.launch {
            _estado.map { it.tipo }
                .distinctUntilChanged()
                .flatMapLatest { tipo -> servicio.observarCategorias(tipo) }
                .collect { categorias -> _estado.update { it.copy(categorias = categorias) } }
        }
    }

    fun elegirTipo(tipo: TipoMovimiento) {
        if (tipo == _estado.value.tipo) return
        // Las categorías son de un solo tipo: al cambiarlo, la elegida deja de ser válida.
        cambiarCampos {
            it.copy(
                tipo = tipo,
                idCategoria = null,
                categorias = emptyList(),
                errores = it.errores.copy(categoria = null, faltaCategoria = false),
            )
        }
    }

    fun onMontoCambiado(texto: String) {
        if (texto == _estado.value.montoTexto) return
        cambiarCampos { it.copy(montoTexto = texto, errores = it.errores.copy(monto = null)) }
    }

    /** Botones +5/+10/+20/+50: suman al monto escrito. Un monto inválido no se toca y se explica por qué. */
    fun sumar(quetzales: Int) {
        val texto = _estado.value.montoTexto
        val actual = if (texto.isBlank()) {
            0L
        } else {
            when (val monto = Dinero.desdeTexto(texto)) {
                is Resultado.Ok -> monto.valor
                is Resultado.ErrorNegocio -> {
                    _estado.update { it.copy(errores = it.errores.copy(monto = monto.mensaje)) }
                    return
                }
                is Resultado.Fallo -> return
            }
        }
        // Si la suma pasa del máximo, el core lo rechaza al guardar como cualquier otro monto.
        val suma = actual + quetzales * CENTAVOS_POR_QUETZAL
        cambiarCampos { it.copy(montoTexto = Dinero.aTexto(suma), errores = it.errores.copy(monto = null)) }
    }

    fun elegirCategoria(idCategoria: Long) {
        cambiarCampos {
            it.copy(idCategoria = idCategoria, errores = it.errores.copy(categoria = null, faltaCategoria = false))
        }
    }

    fun elegirFecha(fecha: LocalDate) {
        cambiarCampos { it.copy(fecha = fecha, errores = it.errores.copy(fecha = null)) }
    }

    fun onDescripcionCambiada(texto: String) {
        if (texto == _estado.value.descripcion) return
        cambiarCampos { it.copy(descripcion = texto, errores = it.errores.copy(descripcion = null)) }
    }

    fun guardar() {
        val actual = _estado.value
        // Doble toque: la segunda llamada llega con `guardando` o `guardadoExitoso` ya en true y se ignora.
        if (actual.guardando || actual.guardadoExitoso) return
        // Sin categoría no hay un ID que enviar; es el único aviso que no viene del core.
        val idCategoria = actual.idCategoria ?: run {
            _estado.update { it.copy(errores = ErroresFormulario(faltaCategoria = true)) }
            return
        }

        _estado.update { it.copy(guardando = true, errores = ErroresFormulario()) }
        val datos = DatosMovimiento(
            tipo = actual.tipo,
            montoTexto = actual.montoTexto,
            moneda = Moneda.GTQ,
            tasa = null,
            idCategoria = idCategoria,
            descripcion = actual.descripcion,
            fecha = actual.fecha,
        )
        viewModelScope.launch {
            val resultado = servicio.registrar(datos, hoy())
            _estado.update {
                when (resultado) {
                    is Resultado.Ok -> it.copy(guardando = false, guardadoExitoso = true)
                    is Resultado.ErrorNegocio -> it.copy(guardando = false, errores = erroresDe(resultado))
                    is Resultado.Fallo -> it.copy(guardando = false, errores = ErroresFormulario(falloAlGuardar = true))
                }
            }
        }
    }

    private fun erroresDe(error: Resultado.ErrorNegocio): ErroresFormulario = when (error.campo) {
        CampoMovimiento.MONTO -> ErroresFormulario(monto = error.mensaje)
        CampoMovimiento.CATEGORIA -> ErroresFormulario(categoria = error.mensaje)
        CampoMovimiento.FECHA -> ErroresFormulario(fecha = error.mensaje)
        CampoMovimiento.DESCRIPCION -> ErroresFormulario(descripcion = error.mensaje)
        // MONEDA no se usa en GTQ; un error sin campo (por ejemplo, desborde del saldo) va al aviso general.
        CampoMovimiento.MONEDA, null -> ErroresFormulario(general = error.mensaje)
    }

    // Cada cambio de campo se copia a SavedStateHandle para sobrevivir a la muerte del proceso.
    private fun cambiarCampos(transformar: (FormularioUiState) -> FormularioUiState) {
        _estado.update(transformar)
        val campos = _estado.value
        estadoGuardado[CLAVE_TIPO] = campos.tipo.name
        estadoGuardado[CLAVE_MONTO] = campos.montoTexto
        estadoGuardado[CLAVE_CATEGORIA] = campos.idCategoria
        estadoGuardado[CLAVE_FECHA] = campos.fecha.toEpochDay()
        estadoGuardado[CLAVE_DESCRIPCION] = campos.descripcion
    }

    private fun leerCampos() = FormularioUiState(
        tipo = estadoGuardado.get<String>(CLAVE_TIPO)?.let { TipoMovimiento.valueOf(it) } ?: TipoMovimiento.GASTO,
        montoTexto = estadoGuardado.get<String>(CLAVE_MONTO).orEmpty(),
        idCategoria = estadoGuardado.get<Long>(CLAVE_CATEGORIA),
        fecha = estadoGuardado.get<Long>(CLAVE_FECHA)?.let { LocalDate.ofEpochDay(it) } ?: hoy(),
        descripcion = estadoGuardado.get<String>(CLAVE_DESCRIPCION).orEmpty(),
    )

    companion object {
        internal const val CLAVE_TIPO = "tipo"
        internal const val CLAVE_MONTO = "montoTexto"
        internal const val CLAVE_CATEGORIA = "idCategoria"
        internal const val CLAVE_FECHA = "fechaEpochDay"
        internal const val CLAVE_DESCRIPCION = "descripcion"
        private const val CENTAVOS_POR_QUETZAL = 100L

        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FinanzasApp
                FormularioViewModel(app.dependencias.servicio, createSavedStateHandle())
            }
        }
    }
}
