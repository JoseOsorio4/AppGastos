# Mapa del código

Relaciona cada función visible con el archivo que la dibuja, el que recibe la acción y el que guarda o consulta los datos. **Regla:** solo se escriben rutas reales en las filas "Existe"; las demás llevan los nombres planificados en [DISENO_FINANZAS.md](DISENO_FINANZAS.md) y se actualizan al construir su incremento.

Rutas relativas a `finanzas-app/app/src/main/` (app) y `finanzas-core/src/main/kotlin/gt/edu/umg/finanzas/core/` (core), ambas en la raíz del repositorio.

> Los IDs de las filas pendientes siguen el diseño actual. Pueden cambiar al contrastar con la maqueta de Canva, que todavía no se ha recibido ([diseño §3.0](DISENO_FINANZAS.md#30-contraste-con-las-referencias-visuales--pendiente)).

## 1. Funciones visibles

| Función visible | Estado | XML / ID | Activity | ViewModel | Core | Persistencia o red |
|---|---|---|---|---|---|---|
| Texto de conexión con el core | **Existe, sin compilar** (Inc. 0) | `res/layout/activity_inicio.xml` · `textoConexionCore` | `ui/inicio/InicioActivity.onCreate` | — | `InfoCore.mensajeConexion()` | — (versión leída de `finanzas-core.properties` dentro del JAR) |
| Ver saldo disponible | Pendiente (Inc. 3) | `activity_inicio.xml` · `textoSaldoDisponible` | `InicioActivity` | `InicioViewModel.estado` | `ServicioFinanzas.observarResumen` | `UsuarioDao.observarUsuario` |
| Filtrar período en Inicio | Pendiente (Inc. 3) | `chipDia` … `chipPersonalizado` | `InicioActivity` | `InicioViewModel.seleccionarFiltro` | `Periodo.de` | `TransaccionDao.observarTotales` |
| Dibujar anillo de gastos | Pendiente (Inc. 3) | `graficoGastos` (`GraficoAnilloView`) | `InicioActivity` | `InicioViewModel.estado` | `DistribucionGastos.calcular` | `TransaccionDao.observarGastosPorCategoria` |
| Registrar movimiento | Pendiente (Inc. 3) | `botonAgregarMovimiento` → `activity_formulario.xml` · `botonGuardar` | `FormularioActivity` | `FormularioViewModel.guardar` | `ValidadorMovimiento.validar`, `CalculoSaldo.deltaCrear` | `OperacionesMovimientoRoom.crear` |
| Editar movimiento | Pendiente (Inc. 4) | `dialogo_detalle_movimiento.xml` · `botonEditar` → `botonGuardar` | `OperacionesActivity` → `FormularioActivity` | `FormularioViewModel.guardar` | `CalculoSaldo.deltaEditar` | `OperacionesMovimientoRoom.editar` |
| Eliminar movimiento | Pendiente (Inc. 4) | `botonEliminar` + diálogo de confirmación | `OperacionesActivity` | `OperacionesViewModel.eliminar` | `CalculoSaldo.deltaEliminar` | `OperacionesMovimientoRoom.eliminar` |
| Filtrar operaciones | Pendiente (Inc. 4) | `chipTodos/chipIngresos/chipGastos`, rango | `OperacionesActivity` | `OperacionesViewModel.seleccionarFiltro` | `Periodo.de` | `TransaccionDao.observarMovimientos` |
| Recalcular saldo | Pendiente (Inc. 5) | aviso `botonRecalcularSaldo` | `InicioActivity` | `InicioViewModel.recalcularSaldo` | `CalculoSaldo.saldoDesde` | `ReconstructorSaldoRoom` |
| Cambiar tema | Pendiente (Inc. 5) | menú `opcionTema` | `InicioActivity` | — | — | `PreferenciaTema` |
| Consultar tasa | Pendiente (Inc. 6) | `tarjeta_tasa.xml` · `botonReintentarTasa` | `InicioActivity` | `InicioViewModel.estadoTasa` | `TasaCambio` | `RepositorioTipoCambio` → `ClienteBanguat` |
| Convertir moneda | Pendiente (Inc. 6) | `dialogo_conversor.xml`; `textoConversionPrevia` en el formulario | `ConversorDialogo`, `FormularioActivity` | `FormularioViewModel.onMontoCambiado` | `ConversorMoneda` | — (usa la tasa ya cargada) |
| Clima / Reflexión | Pendiente (Inc. 7) | `tarjeta_clima.xml`, `tarjeta_reflexion.xml` | `InicioActivity` | `estadoClima`, `estadoReflexion` | — | `RepositorioClima`, `RepositorioReflexion` |

## 2. Recorrido real: Incremento 0
1. `finanzas-core/pom.xml` copia `<version>0.1.0</version>` en `finanzas-core.properties` (filtrado de recursos) y compila `InfoCore.kt` con destino JVM 11.
2. `mvn clean install` deja `finanzas-core-0.1.0.jar` en `~/.m2/repository/gt/edu/umg/finanzas/finanzas-core/0.1.0/`.
3. `finanzas-app/settings.gradle.kts` solo busca el grupo `gt.edu.umg.finanzas` en `mavenLocal()` (`exclusiveContent`). `gradle/libs.versions.toml` fija `finanzasCore = "0.1.0"`, y `app/build.gradle.kts` lo declara con `implementation(libs.finanzas.core)`.
4. `AndroidManifest.xml` abre `ui.inicio.InicioActivity`, que infla `activity_inicio.xml` con View Binding y escribe `InfoCore.mensajeConexion()` en `textoConexionCore`.

Estado: archivos creados; **Maven, Gradle y la ejecución siguen pendientes** de comprobarse en tu computadora.

## 3. Preguntas de defensa

| Pregunta | Respuesta breve | Dónde mirar | Estado |
|---|---|---|---|
| ¿Cómo llega código del core a la app? | Maven instala el JAR en `.m2`; Gradle lo resuelve por sus coordenadas | `pom.xml`, `settings.gradle.kts`, `libs.versions.toml` | Existe |
| ¿Cómo sé que la app usa el JAR correcto? | La pantalla muestra la versión que Maven copió del `pom.xml` | `InfoCore.version` | Existe |
| ¿Dónde se dibuja el botón Guardar? | En el XML del formulario, con ID `botonGuardar` | `activity_formulario.xml` | Inc. 3 |
| ¿Qué pasa al presionarlo? | La Activity llama a `guardar()`; el ViewModel ignora toques repetidos y llama al servicio | `FormularioActivity`, `FormularioViewModel.guardar` | Inc. 3 |
| ¿Quién valida el monto? | El core: `Dinero.desdeTexto` dentro de `ValidadorMovimiento.validar` | `reglas/` | Inc. 1 |
| ¿Quién guarda? | `OperacionesMovimientoRoom` dentro de un `withTransaction` | `datos/local/` | Inc. 2 |
| ¿Cómo cambia el saldo? | Se suma `impactoEnSaldo()` del nuevo y se resta el del anterior, en la misma transacción | `Transaccion`, `CalculoSaldo` | Inc. 1–2 |
| ¿Qué pasa si falla a mitad? | La excepción revierte la inserción y el saldo | `OperacionesMovimientoRoom` | Inc. 2 |
| ¿Qué pasa sin internet? | Las finanzas funcionan; USD usa la última tasa guardada o queda deshabilitado | `RepositorioTipoCambio` | Inc. 6 |

Al terminar cada incremento: cambia "Pendiente" por la ruta y la función reales, y añade el recorrido del incremento en la §2.
