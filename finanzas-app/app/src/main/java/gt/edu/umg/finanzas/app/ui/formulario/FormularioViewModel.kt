package gt.edu.umg.finanzas.app.ui.formulario

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import gt.edu.umg.finanzas.app.FinanzasApp
import gt.edu.umg.finanzas.app.datos.repositorios.RepositorioTipoCambio
import gt.edu.umg.finanzas.core.dominio.CampoMovimiento
import gt.edu.umg.finanzas.core.dominio.DatosMovimiento
import gt.edu.umg.finanzas.core.dominio.Moneda
import gt.edu.umg.finanzas.core.dominio.Periodo
import gt.edu.umg.finanzas.core.dominio.TasaCambio
import gt.edu.umg.finanzas.core.dominio.TipoMovimiento
import gt.edu.umg.finanzas.core.reglas.Dinero
import gt.edu.umg.finanzas.core.resultado.Resultado
import gt.edu.umg.finanzas.core.servicio.ServicioFinanzas
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del formulario para registrar y para editar. Recibe cada cambio de campo, los copia a
 * SavedStateHandle y, al guardar, envía los datos crudos al servicio: la validación y el cálculo
 * del saldo los hace el core. Si el Intent trae un ID, carga ese movimiento y guarda con editar().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FormularioViewModel(
    private val servicio: ServicioFinanzas,
    private val estadoGuardado: SavedStateHandle,
    private val hoy: () -> LocalDate = { Periodo.hoy() },
    private val tipoCambio: RepositorioTipoCambio? = null,
) : ViewModel() {

    // Llega como extra del Intent (FormularioActivity.paraEditar); SavedStateHandle lo recibe solo.
    private val idEnEdicion: Long? = estadoGuardado.get<Long>(CLAVE_ID_EN_EDICION)

    private val _estado = MutableStateFlow(leerCampos())
    val estado: StateFlow<FormularioUiState> = _estado.asStateFlow()

    init {
        if (tipoCambio != null) {
            viewModelScope.launch { tipoCambio.estado.collect { tasa -> _estado.update { it.copy(estadoTasa = tasa) } } }
            if (idEnEdicion == null) actualizarTasa()
        }
        // Al recrear se relee la fotografía de Room, sin pisar los campos que el usuario ya cambió.
        if (idEnEdicion != null) cargarMovimiento(idEnEdicion, estadoGuardado.get<Boolean>(CLAVE_CARGADO) != true)
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

    fun elegirMoneda(moneda: Moneda) {
        if (moneda == Moneda.USD && !_estado.value.usdDisponible) return
        cambiarCampos { it.copy(moneda = moneda, errores = ErroresFormulario()) }
    }

    fun actualizarTasa() {
        if (tipoCambio == null || tipoCambio.estado.value.cargando) return
        viewModelScope.launch { tipoCambio.actualizar() }
    }

    fun onMontoCambiado(texto: String) {
        if (texto == _estado.value.montoTexto) return
        cambiarCampos { it.copy(montoTexto = texto, errores = it.errores.copy(monto = null)) }
    }

    /** Botones +5/+10/+20/+50: suman al monto escrito. Un monto inválido no se toca y se explica por qué. */
    fun sumar(unidades: Int) {
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
        val suma = actual + unidades * CENTAVOS_POR_UNIDAD
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

    private fun cargarMovimiento(id: Long, completarCampos: Boolean) {
        _estado.update { it.copy(cargandoMovimiento = true) }
        viewModelScope.launch {
            when (val leido = servicio.obtenerParaEditar(id)) {
                is Resultado.Ok -> {
                    val movimiento = leido.valor
                    val conversion = movimiento.conversion
                    cambiarCampos {
                        val campos = if (completarCampos) it.copy(
                            tipo = movimiento.tipo,
                            montoTexto = Dinero.aTexto(conversion?.montoOriginalCentavos ?: movimiento.montoCentavos),
                            moneda = if (conversion == null) Moneda.GTQ else Moneda.USD,
                            idCategoria = movimiento.idCategoria,
                            fecha = movimiento.fecha,
                            descripcion = movimiento.descripcion,
                        ) else it
                        campos.copy(tasaHistorica = conversion?.tasa, recargoHistorico = conversion?.recargoGtqPorUsd,
                            tipoHistorico = movimiento.tipo, cargandoMovimiento = false)
                    }
                    estadoGuardado[CLAVE_CARGADO] = true
                }
                Resultado.NoEncontrado -> _estado.update {
                    it.copy(cargandoMovimiento = false, errores = ErroresFormulario(movimientoNoEncontrado = true))
                }
                is Resultado.Fallo -> _estado.update {
                    it.copy(cargandoMovimiento = false, errores = ErroresFormulario(falloAlCargar = true))
                }
            }
        }
    }

    fun guardar() {
        val actual = _estado.value
        // Doble toque: la segunda llamada llega con `guardando` o `guardadoExitoso` ya en true y se ignora.
        if (actual.guardando || actual.guardadoExitoso || actual.cargandoMovimiento) return
        // Sin categoría no hay un ID que enviar; es el único aviso que no viene del core.
        val idCategoria = actual.idCategoria ?: run {
            _estado.update { it.copy(errores = ErroresFormulario(faltaCategoria = true)) }
            return
        }

        _estado.update { it.copy(guardando = true, errores = ErroresFormulario()) }
        val datos = DatosMovimiento(
            tipo = actual.tipo,
            montoTexto = actual.montoTexto,
            moneda = actual.moneda,
            tasa = actual.tasa,
            idCategoria = idCategoria,
            descripcion = actual.descripcion,
            fecha = actual.fecha,
        )
        viewModelScope.launch {
            // Mismo formulario, dos operaciones: crear o editar; ambas recalculan el saldo en Room.
            val resultado: Resultado<*> =
                if (idEnEdicion == null) servicio.registrar(datos, hoy()) else servicio.editar(idEnEdicion, datos, hoy())
            _estado.update {
                when (resultado) {
                    is Resultado.Ok -> it.copy(guardando = false, guardadoExitoso = true)
                    is Resultado.ErrorNegocio -> it.copy(guardando = false, errores = erroresDe(resultado))
                    Resultado.NoEncontrado ->
                        it.copy(guardando = false, errores = ErroresFormulario(movimientoNoEncontrado = true))
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
        // Los errores de moneda y de saldo van al aviso general, sin borrar lo escrito.
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
        estadoGuardado[CLAVE_MONEDA] = campos.moneda.name
        estadoGuardado[CLAVE_TASA_HISTORICA] = campos.tasaHistorica?.gtqPorUsd?.toPlainString()
        estadoGuardado[CLAVE_FECHA_TASA] = campos.tasaHistorica?.fechaReferencia?.toString()
        estadoGuardado[CLAVE_FUENTE_TASA] = campos.tasaHistorica?.fuente
        estadoGuardado[CLAVE_RECARGO_HISTORICO] = campos.recargoHistorico?.toPlainString()
        estadoGuardado[CLAVE_TIPO_HISTORICO] = campos.tipoHistorico?.name
    }

    private fun leerCampos() = FormularioUiState(
        tipo = estadoGuardado.get<String>(CLAVE_TIPO)?.let { TipoMovimiento.valueOf(it) } ?: TipoMovimiento.GASTO,
        montoTexto = estadoGuardado.get<String>(CLAVE_MONTO).orEmpty(),
        idCategoria = estadoGuardado.get<Long>(CLAVE_CATEGORIA),
        fecha = estadoGuardado.get<Long>(CLAVE_FECHA)?.let { LocalDate.ofEpochDay(it) } ?: hoy(),
        descripcion = estadoGuardado.get<String>(CLAVE_DESCRIPCION).orEmpty(),
        enEdicion = idEnEdicion != null,
        moneda = estadoGuardado.get<String>(CLAVE_MONEDA)?.let(Moneda::valueOf) ?: Moneda.GTQ,
        tasaHistorica = estadoGuardado.get<String>(CLAVE_TASA_HISTORICA)?.let {
            TasaCambio(BigDecimal(it), LocalDate.parse(estadoGuardado[CLAVE_FECHA_TASA]), checkNotNull(estadoGuardado[CLAVE_FUENTE_TASA]))
        },
        recargoHistorico = estadoGuardado.get<String>(CLAVE_RECARGO_HISTORICO)?.let(::BigDecimal),
        tipoHistorico = estadoGuardado.get<String>(CLAVE_TIPO_HISTORICO)?.let(TipoMovimiento::valueOf),
    )

    companion object {
        /** Extra del Intent con el ID a editar; sin él, el formulario registra un movimiento nuevo. */
        const val CLAVE_ID_EN_EDICION = "idEnEdicion"
        private const val CLAVE_CARGADO = "movimientoCargado"
        private const val CLAVE_MONEDA = "moneda"
        private const val CLAVE_TASA_HISTORICA = "tasaHistorica"
        private const val CLAVE_FECHA_TASA = "fechaTasa"
        private const val CLAVE_FUENTE_TASA = "fuenteTasa"
        private const val CLAVE_RECARGO_HISTORICO = "recargoHistorico"
        private const val CLAVE_TIPO_HISTORICO = "tipoHistorico"
        internal const val CLAVE_TIPO = "tipo"
        internal const val CLAVE_MONTO = "montoTexto"
        internal const val CLAVE_CATEGORIA = "idCategoria"
        internal const val CLAVE_FECHA = "fechaEpochDay"
        internal const val CLAVE_DESCRIPCION = "descripcion"
        private const val CENTAVOS_POR_UNIDAD = 100L

        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as FinanzasApp
                FormularioViewModel(app.dependencias.servicio, createSavedStateHandle(), tipoCambio = app.dependencias.tipoCambio)
            }
        }
    }
}
