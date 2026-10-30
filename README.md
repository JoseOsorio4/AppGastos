# AppGastos — Finanzas personales en Kotlin

Proyecto de Programación 2 (Universidad Mariano Gálvez de Guatemala). Es una app Android para registrar ingresos y gastos en quetzales y ver de forma sencilla cuánto hay disponible, cuánto entró y cuánto salió en un período. Funciona sin conexión; las consultas por internet (tipo de cambio, clima, reflexión) son opcionales.

## Estructura

| Carpeta | Qué es | Herramienta |
|---|---|---|
| [`finanzas-core/`](finanzas-core/) | Modelos, reglas financieras e interfaces de almacenamiento. Kotlin/JVM, sin Android ni Room | Maven (IntelliJ IDEA) |
| [`finanzas-app/`](finanzas-app/) | Pantallas XML, ViewModels y persistencia con Room sobre SQLite. Consume el JAR del core | Gradle (Android Studio) |
| [`docs/`](docs/) | Documentación | — |

La app recibe el core como dependencia Maven `gt.edu.umg.finanzas:finanzas-core`, publicada en el repositorio Maven local (`~/.m2`).

La moneda base es el quetzal (GTQ). El conversor tiene únicamente **dólares a quetzales (USD → GTQ)** y **quetzales a dólares (GTQ → USD)**. Desde el incremento 6 la app consulta la referencia Banguat, conserva su última tasa y registra movimientos USD con su fotografía histórica; saldo y reportes siguen en GTQ.

## Documentación

- [Diseño](docs/DISENO_FINANZAS.md): alcance, decisiones, modelo de datos, flujos e incrementos.
- [Mapa del código](docs/MAPA_DEL_CODIGO.md): cada acción visible con su XML, Activity, ViewModel, regla y almacenamiento; preguntas de defensa.
- [Guía de Windows](docs/GUIA_WINDOWS.md): pasos detallados y solución de errores.
- [Continuar en Kiro IDE](docs/CONTINUAR_EN_KIRO_IDE.md): abrir este mismo repositorio desde otra cuenta y seguir con el incremento pendiente.
- [Referencia visual](docs/referencias/INTERFAZ.md): captura de Canva, distribución de pantallas y decisiones de adaptación.
- [AGENTS.md](AGENTS.md): reglas de trabajo en este repositorio.

## Orden para construir

1. **Core:** abre `finanzas-core/pom.xml` en IntelliJ y ejecuta `mvn clean install`.
2. **App:** abre la carpeta `finanzas-app/` en Android Studio, sincroniza Gradle y ejecuta `app`.
3. **Pruebas instrumentadas (Room e interfaz):** con un emulador abierto, `.\gradlew.bat -PdatosPrueba :app:connectedDebugAndroidTest` desde `finanzas-app`. La instalación `.pruebas` protege los datos de la app normal. La prueba sin red desactiva y restaura wifi/datos del emulador; la prueba Banguat real requiere conexión.

El core se instala **antes** de abrir la app, porque Gradle lo busca en `~/.m2`. Se necesita JDK 17 o superior. Las herramientas y dependencias que todavía no estén en caché requieren internet para descargarse.

## Estado real

Último incremento cerrado: **6 — tasa Banguat, GTQ/USD y migración Room v1 → v2**. Siguiente: **7 — clima y reflexión**, todavía sin implementar.

### Incremento 6 — referencia Banguat, USD histórico y migración

| Comprobación | Estado | Detalle |
|---|---|---|
| Core y app antes de editar | ✅ Ejecutados | `mvn clean install`: 55 pruebas, 0 fallos, `BUILD SUCCESS`. `:app:assembleDebug`: `BUILD SUCCESSFUL`. Rama main, último commit inicial `5f9a940`; solo `img.png` sin seguimiento |
| Contrato y respuesta Banguat antes del lector | ✅ Comprobados | Contrato oficial de `TipoCambioDia` y POST SOAP 1.1 HTTP 200 el 29/10/2026: referencia 7.64382, fecha 29/10/2026. Cuerpo sin modificar y procedencia en `app/src/androidTest/assets/banguat/`; el fixture no rellena la caché de producción |
| Core | ✅ Conservado | 0.3.0 sin cambios: `Dinero`, `ConversorMoneda`, `ValidadorMovimiento` y `ConversionAplicada` ya proporcionaban las reglas. Ninguna fórmula ni redondeo se copia en la UI |
| Persistencia v2 | ✅ Creada y compilada | Mismas 3 tablas. `TransaccionEntity`, `Convertidores`, `Mapeos` y `MovimientoFila` conservan original USD, equivalente GTQ y tasa histórica (valor TEXT, fecha y fuente). Esquema `app/schemas/…/2.json` exportado y versionado; v1 intacto |
| Migración con datos v1 | ✅ Ejecutada | `Migracion1a2Test`: `MigrationTestHelper` crea otra base con Q 70, bandera activa, 12 categorías (una archivada) y 2 movimientos. Valida v2, importes originales GTQ, IDs, tipo, fecha, descripción, usuario, categoría, bandera, saldo y FK. Reabre con los DAO actuales y repetir la siembra conserva los datos |
| Caché y consulta | ✅ Creadas y probadas | `ClienteHttp` (HTTPS, 10 s por conexión/lectura, 256 KiB), `ClienteBanguat`, lector de XML por ruta y espacios; `CacheServicios` conserva tasa, fuente, fecha del proveedor y momento de consulta. `RepositorioTipoCambio` usa caché del día y mantiene el dato anterior ante fallo |
| Compilación final | ✅ Ejecutada | `:app:assembleDebug`, `:app:assembleDebugAndroidTest` con `-PdatosPrueba`; también compilación normal sin el sufijo y `installDebug` de la instalación de prueba. Sin cambios de AGP, Gradle, Kotlin ni Room |
| Suite instrumentada final API 37 | ✅ Ejecutada | `-PdatosPrueba :app:connectedDebugAndroidTest`: **48/48**, 0 fallos, 0 errores, 0 omitidas en el XML de `app/build/outputs/androidTest-results/connected/debug/` |
| Caso e y edición histórica | ✅ Ejecutados en Room y UI | US$ 10 × tasa ficticia 7.66000 → 7660 centavos GTQ y original 1000. Guardar en caché 7.70000 no altera fila ni saldo. Editar a US$ 12 conserva 7.66000 y deja Q 91.92. USD → GTQ elimina la conversión; borrar USD revierte el saldo |
| Caso i y conversor | ✅ Ejecutados en UI | El diálogo tiene exactamente USD → GTQ y GTQ → USD. Con tasa ficticia 7.66000: US$ 10 → Q 76.60 y Q 100 → US$ 13.05. Recrear Inicio con el conversor abierto conserva monto, sentido y resultado |
| Sin red | ✅ Ejecutado en emulador | `RecorridoUsdTest` desactiva wifi y datos: sin caché USD queda deshabilitado y GTQ registra Q 100; con tasa de prueba guardada anterior registra US$ 10 y deja Q 176.60. Muestra fuente/fecha, «Última disponible» y fallo de actualización; restaura la conectividad |
| Banguat real desde Android | ✅ Ejecutado | `BanguatTest.consulta_el_servicio_oficial_desde_android` completó la petición real. En el recorrido visual del 30/10/2026 se obtuvo 7.64187 del 30/10/2026; US$ 10 registrados desde Formulario dejaron Q 76.42 en saldo/ingresos y «Original: US$ 10.00» en Operaciones |
| Comparación visual | ✅ Revisada | Tarjeta, conversor, Formulario USD y detalle con capturas claras/oscuras. Conservan cabeceras verdes, acento amarillo, tres destinos y cuadrícula de categorías. Cambiar el tema conservó el monto y el resultado. El detalle USD de prueba también cabe con `font_scale` 2.0; se restauró la escala |
| Reabrir sin red con la tasa real | ✅ Revisado por ADB | `force-stop` y nuevo arranque de `.pruebas` conservan Q 76.42 y la referencia real 7.64187; Reintentar sin red muestra que se conserva la tasa guardada. Al restaurar la red, Reintentar vuelve a completar la consulta |
| Incompatibilidad encontrada y corregida | ✅ Verificada | `MigrationTestHelper` falló antes de migrar por core de serialización 1.7.3 mezclado con json 1.8.1. `dependencyInsight` confirmó el conflicto; una restricción alinea core en 1.8.1, requerido por Room migration. La prueba de migración y después la suite completa pasaron |
| Protección de datos y archivo local | ✅ Comprobada | Las pruebas usan `gt.edu.umg.finanzas.app.pruebas`; `AyudaInterfaz` rechaza limpiar el paquete normal. Hashes de `finanzas.db` y su WAL de la instalación original iguales antes/después. `img.png` conserva su hash y no se incluye. No se reinstaló ni se borró la app normal |
| Otros pendientes | ⏳ Conservados | API 24–25; muerte real del proceso con Formulario abierto; rotación manual con detalle/confirmación; comprobación humana con el dedo del segundo detalle. «Usar tasa actual» es opcional y no se implementó: al editar USD siempre se conserva la histórica. Clima y reflexión siguen en Inc. 7 |

Capturas y comparación: [INTERFAZ → Incremento 6](docs/referencias/INTERFAZ.md#incremento-6-tasa-y-movimientos-usd-emulador-api-37). La revisión visual fue por capturas, jerarquías y acciones de ADB, sin datos reales. La respuesta de Banguat se comprobó contra [el contrato oficial](https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx?op=TipoCambioDia).

### Incremento 5 — aviso de recálculo y tema persistente

| Comprobación | Estado | Detalle |
|---|---|---|
| Compilaciones antes de editar | ✅ Ejecutadas | Core 0.3.0: `mvn clean install`, 55 pruebas sin fallos. App: `:app:assembleDebug`, `BUILD SUCCESSFUL` |
| Controles y responsabilidades | ✅ Creados | `activity_inicio.xml`: `opcionTema`, `avisoRecalculo`, `textoAvisoRecalculo`, `textoErrorRecalculo` y `botonRecalcularSaldo`. Activity conecta y pinta; ViewModel conserva período, tema, bandera, recálculo en curso y error. [Recorrido por archivo](docs/MAPA_DEL_CODIGO.md#1-funciones-visibles) |
| Core, tablas y herramientas | ✅ Conservados | Core 0.3.0 sin cambios; Room v1 con las mismas 3 tablas. Se reutiliza `ReconstructorSaldoRoom`; no se añaden dependencias ni APIs |
| Compilación del cambio | ✅ Ejecutada | `:app:assembleDebug`, `:app:assembleDebugAndroidTest` e instalación `:app:installDebug`: `BUILD SUCCESSFUL` |
| Suite instrumentada final en API 37 | ✅ Ejecutada | `:app:connectedDebugAndroidTest`: **37/37**, 0 fallos, 0 errores, 0 omitidas, confirmado en `app/build/outputs/androidTest-results/connected/debug/TEST-…xml` |
| Aviso y recálculo desde la interfaz | ✅ Probados | `RecorridoSaldoTemaTest`: Q 1.23 erróneo con bandera pendiente sobrevive a recrear Inicio; el botón deja Q 70.00, oculta el aviso y apaga la bandera. Conserva los 2 movimientos; repetir el servicio devuelve Q 70.00. Sin bandera y con saldo cero no ofrece recálculo |
| Fallo real y reintento | ✅ Probados | Trigger SQLite de prueba impide apagar la bandera después de asignar saldo: rollback conserva Q 1.23 y bandera activa; aparece el error y el mismo botón permite reintentar hasta Q 70.00. El trigger solo existe en la prueba y se elimina al terminar |
| Tema Sistema/Claro/Oscuro | ✅ Probado | La prueba elige y vuelve a leer las 3 preferencias, verifica la opción marcada al reabrir y los recursos claros/oscuros de Inicio y Formulario. `PreferenciaTema` guarda una sola clave en SharedPreferences; `FinanzasApp` aplica el modo al iniciar |
| App real: saldo y persistencia del tema | ✅ Revisados en emulador | BD de prueba preparada fuera de la app: ingreso Q 100 y gasto Q 30, saldo erróneo Q 1.23 y bandera activa. Aviso visible en claro y oscuro; tocar Recalcular deja Q 70.00. Las 3 opciones sobreviven a `am force-stop` y nuevo arranque; Sistema sigue `cmd uimode night yes/no`, restaurado a `no` al terminar |
| Segundo detalle tras cancelar Eliminar | ✅ Revisado por acciones individuales de ADB; no reproducido | Se abrió Almuerzo → Eliminar → Cancelar → Almuerzo. El segundo detalle seguía abierto después de 372 s sin entrada. Tras otra cancelación y reapertura seguía abierto a los 48 s. No se cambió el código de Operaciones ni se declara corregida la causa; queda pendiente una comprobación humana con el dedo |
| Ajustes de pruebas existentes | ✅ Ejecutados | La primera suite dio 36/37: la prueba del resumen aceptaba un total nuevo con saldo anterior porque combina dos Flow; ahora espera ambos valores finales. La segunda se interrumpió con Room cerrado mientras un ViewModel de prueba seguía leyendo; ahora cancela y espera sus corrutinas antes de cerrar la base. La suite final pasó completa |
| Revisión del cambio | ✅ Realizada | Recorrido XML → Activity/diálogo → ViewModel → servicio/Room o preferencias; sin cálculos monetarios nuevos en UI, sin migración, sin cambios de Operaciones. `git diff --check` y hash de `img.png` comprobados |
| Otros pendientes anteriores | ⏳ Conservados | Rotación manual con detalle o confirmación abiertos, muerte real del proceso con el formulario abierto y ejecución en API 24–25 siguen sin probarse |

Capturas: [aviso claro](docs/referencias/capturas/inc5-aviso-claro.png) · [selector](docs/referencias/capturas/inc5-selector-tema.png) · [aviso oscuro](docs/referencias/capturas/inc5-aviso-oscuro.png) · [Q 70.00 tras recalcular](docs/referencias/capturas/inc5-saldo-recalculado.png) · [segundo detalle aún abierto](docs/referencias/capturas/inc5-segundo-detalle.png).

La revisión visual se hizo leyendo capturas y jerarquías de Android entre acciones individuales de ADB; no fue una sesión con el dedo. El control de Windows falló al capturar la ventana (`window crop is outside captured monitor`). Al arrancar apareció también un aviso de cierre inesperado del emulador; el AVD arrancado en frío con render por software permitió ejecutar la suite. El `logcat` del recorrido final, separado de los fallos provocados por las pruebas, no contiene excepciones fatales de la app.

### Incremento 4 — Operaciones: lista, filtros, detalle, editar y eliminar

| Comprobación | Estado | Detalle |
|---|---|---|
| `assembleDebug` y `assembleDebugAndroidTest` | ✅ Ejecutado | `BUILD SUCCESSFUL`, sin avisos del compilador de Kotlin. Core sin cambios (0.3.0); las 3 tablas de Room no cambian |
| `connectedDebugAndroidTest` en emulador API 37 | ✅ Ejecutado | 33/33 pruebas, 0 fallos, 0 omitidas (informe XML de Gradle), con el código final, incluidos los ajustes de letra grande |
| Lista vacía, filtros por tipo y período combinados | ✅ Probado | `RecorridoOperacionesTest`: aviso sin movimientos; Ingresos, Gastos, Día y Todos |
| Editar importe y tipo desde el detalle con el mismo Formulario | ✅ Probado | Gasto Q 30 → ingreso Q 45: la fila cambia e Inicio muestra Q 145.00; sigue habiendo 2 movimientos |
| Caso a del diseño desde la interfaz | ✅ Probado | Q 70 → editar el gasto a Q 20 → Q 80 → eliminarlo → Q 100 |
| Cancelar una eliminación no borra; confirmar sí | ✅ Probado | 3 movimientos tras Cancelar; 2 tras Eliminar e Inicio vuelve a Q 70.00 |
| Barra inferior sin apilar pantallas | ✅ Probado | Tocar Operaciones en Operaciones no hace nada; Inicio la cierra |
| Edición en el ViewModel | ✅ Probado | `FormularioViewModelTest`: carga y guarda con `editar`; un ID borrado avisa y no inserta |
| App real: crear, ver en Operaciones, editar importe y tipo | ✅ Ejecutado en el emulador | Desde la interfaz: Pago Q 100, Almuerzo Q 30 y Bus Q 20 → Inicio Q 50.00. Almuerzo pasa a ingreso Q 45 en Regalo: la fila muestra +Q 45.00 e Inicio Q 125.00 (ingresos Q 145.00, gastos Q 20.00). La descripción no cambió porque `adb input text` corta en el espacio; el cambio de descripción lo cubre la prueba instrumentada |
| App real: cancelar una eliminación y confirmar otra | ✅ Ejecutado en el emulador | Cancelar dejó Bus en la lista. En el segundo intento se abrió el detalle, se confirmó Eliminar y Bus desapareció; Inicio quedó en Q 145.00. En dos recorridos anteriores con `adb`, el segundo detalle se cerró antes de que el script tocara Eliminar; no se reprodujo en el tercer recorrido ni en las pruebas de Espresso, que hacen esa misma secuencia. Queda como observación por revisar a mano |
| App real: filtros y estado vacío | ✅ Ejecutado en el emulador | Sin datos: "No hay movimientos con estos filtros" y Operaciones marcada en la barra. Ingresos muestra solo Pago; Gastos, Almuerzo y Bus; Día muestra los tres movimientos de hoy |
| Calendarios (rango y fecha) | ✅ Ejecutado en el emulador | Rango: cancelar conserva el chip anterior; aceptar marca Período. Fecha: los días futuros salen deshabilitados y OK conserva la fecha. Los textos propios del selector ("Save", "October") siguen el idioma del sistema del emulador (inglés); el título es nuestro ("Elige el período") |
| Tema oscuro en Operaciones | ✅ Revisado en capturas | Mismos layouts con los colores de `values-night` |
| Errores de ejecución | ✅ Ninguno | `logcat` sin entradas de `AndroidRuntime` en los recorridos |
| Letra grande del sistema (`font_scale` 1.3 y 2.0) en Inicio, Formulario, Operaciones y detalle | ✅ Revisado y corregido en el emulador | Se partían "Entretenimiento", "Alimentación" y "Educación" en la cuadrícula, "Operaciones" en la barra y "Gastos del período" se salía del anillo. Ahora la cuadrícula usa 4, 3 o 2 columnas según la escala, la etiqueta de la barra queda en una línea y el centro del anillo reduce su texto dentro del hueco. Se deja así: la fila de períodos se desplaza de lado y "Q 1,250.00" ocupa dos líneas con 2.0. [Detalle y capturas](docs/referencias/INTERFAZ.md#letra-grande-del-sistema-escala-13-y-20) |
| Rotación a mano con el detalle o la confirmación abiertos | ⏳ Pendiente de revisión manual | Son `DialogFragment`; no se rotó el emulador con ellos abiertos |
| Muerte real del proceso con el formulario abierto, API 24–25 | ⏳ Pendientes | Igual que en el Inc. 3 |
| Aviso visible para recalcular el saldo y selector de tema | ✅ Inc. 5 | Ver la tabla del incremento 5 |

Capturas: [Operaciones](docs/referencias/capturas/inc4-operaciones-lista.png) · [vacía](docs/referencias/capturas/inc4-operaciones-vacia.png) · [detalle](docs/referencias/capturas/inc4-detalle.png) · [editar](docs/referencias/capturas/inc4-formulario-edicion.png) · [confirmar](docs/referencias/capturas/inc4-confirmar-eliminar.png) · [Inicio tras eliminar](docs/referencias/capturas/inc4-inicio-tras-eliminar.png) · [oscuro](docs/referencias/capturas/inc4-operaciones-oscuro.png) · [rango](docs/referencias/capturas/inc4-calendario-rango.png) · [fecha](docs/referencias/capturas/inc4-calendario-fecha.png).

El emulador se cerró solo dos veces durante las pruebas y una vez dejó de dibujar (pantallas negras, `RootViewWithoutFocusException` en todas las pruebas de pantalla). Tras arrancarlo en frío con render por software, las pruebas pasaron. La [guía](docs/GUIA_WINDOWS.md#si-algo-falla) explica qué hacer si ocurre.

### Incremento 3 — Inicio, Formulario y reconstrucción del saldo

| Comprobación | Estado | Detalle |
|---|---|---|
| `ReconstructorSaldoRoom` adelantado y conectado en `Dependencias` | ✅ Creado y probado | Saldo erróneo corregido, segunda llamada sin cambios, sin bandera no toca el saldo, rollback ante fallo |
| Core 0.3.0 (`Dinero.aTexto` para los botones +5…+50) | ✅ Ejecutado | `mvn clean install`: 55 pruebas, 0 fallos, `BUILD SUCCESS` |
| `assembleDebug` con Material 1.14.0 y Lifecycle 2.11.0 | ✅ Ejecutado | `BUILD SUCCESSFUL` (también tras los ajustes visuales) |
| `connectedDebugAndroidTest` en emulador API 37 | ✅ Ejecutado | 25/25 pruebas, `BUILD SUCCESSFUL` (confirmado en PowerShell). Corrió antes de dos ajustes solo de XML (estilo de chips y celda de categoría); después de ellos se ejecutaron `assembleDebug` y el recorrido en la app real, no las 25 pruebas |
| Rotación y cambio a tema oscuro sin perder lo escrito | ✅ Probado | `RecorridoInicioFormularioTest`: horizontal y `MODE_NIGHT_YES` conservan monto, descripción, tipo y categoría |
| Doble toque en Guardar | ✅ Probado | `doubleClick` → 1 movimiento |
| Errores claros y categorías filtradas por tipo | ✅ Probado | "Elige una categoría", "Usa punto para los decimales"; Gasto/Ingreso muestran solo sus categorías |
| Recuperación del formulario desde estado guardado | ✅ Probado | `FormularioViewModelTest` crea un ViewModel con un `SavedStateHandle` precargado y recupera los campos. No es una muerte real del proceso con el formulario abierto; esa prueba sigue pendiente |
| App real: ingreso Q 100 y gasto Q 30 desde la interfaz | ✅ Ejecutado en el emulador | Inicio muestra saldo Q 70.00, ingresos Q 100.00, gastos Q 30.00, resultado +Q 70.00 y anillo con Q 30.00 |
| App real: forzar cierre (`am force-stop`) y reabrir | ✅ Ejecutado en el emulador | Saldo Q 70.00, ingresos Q 100.00 y gastos Q 30.00 se conservan |
| Comparación con la referencia de Canva (claro y oscuro) | ✅ Revisado en capturas | [Inicio](docs/referencias/capturas/inc3-inicio-con-datos.png) · [Formulario](docs/referencias/capturas/inc3-formulario-vacio.png) · [Inicio oscuro](docs/referencias/capturas/inc3-inicio-oscuro.png) · [Formulario oscuro](docs/referencias/capturas/inc3-formulario-oscuro.png). Se corrigieron chips de período invisibles y un nombre de categoría cortado |
| Errores de ejecución durante el recorrido | ✅ Ninguno | `logcat` sin entradas de `AndroidRuntime` |
| Letra grande del sistema | ✅ Resuelto en el Inc. 4 | Ver la tabla del incremento 4 |
| Selector de rango "Período" y de fecha | ✅ Resuelto en el Inc. 4 | Abiertos y usados en el emulador con toques de `adb` (no con el dedo) |
| Rotación de la app real a mano | ⏳ Pendiente de revisión manual | Cubierta por la prueba instrumentada, no por el recorrido con `adb` |
| Ejecución en API 24–25 | ⏳ Pendiente | Solo hay imagen API 37 |
| Aviso visible para recalcular el saldo y selector de tema | ✅ Inc. 5 | Ver la tabla del incremento 5 |
| Operaciones (lista, editar, eliminar) | ✅ Inc. 4 | Ver la tabla del incremento 4 |

### Incremento 2 — Room v1 sobre SQLite

| Comprobación | Estado | Detalle |
|---|---|---|
| Tablas `Usuario`, `Categoria`, `Transaccion`, DAO e implementaciones Room de los contratos | ✅ Creados y compilados | Recorrido y clases en el [mapa](docs/MAPA_DEL_CODIGO.md#3-app-persistencia-room-incremento-2) |
| `mvn clean install` del core | ✅ Ejecutado | 54 pruebas, 0 fallos, `BUILD SUCCESS` (core sin cambios: 0.2.0) |
| `assembleDebug` con Room 2.8.5, KSP 2.3.12 y desugaring | ✅ Ejecutado | `BUILD SUCCESSFUL` |
| Esquema v1 exportado | ✅ Generado y versionado | `finanzas-app/app/schemas/gt.edu.umg.finanzas.app.datos.local.AppDatabase/1.json`: 3 tablas, 2 FK `RESTRICT`, 5 índices |
| `connectedDebugAndroidTest` en emulador API 37 | ✅ Ejecutado | 14 pruebas, 0 fallos, 0 errores (informe XML de Gradle) |
| Caso a (Q 70 → Q 80 → Q 100) y caso b (… → Q 0) | ✅ Probados en Room | `ServicioSobreRoomTest`, recorrido servicio → contrato → Room → SQLite |
| Caso c: falla al guardar el saldo después de escribir el movimiento | ✅ Probado | Crear, editar y eliminar devuelven `ErrorAlmacenamiento`; movimientos y saldo sin cambios |
| Caso d: cerrar y volver a abrir el archivo SQLite | ✅ Probado | `PersistenciaTest`: 2 movimientos y saldo Q 70 tras reabrir; sin duplicar usuario ni categorías |
| Caso j: desborde del saldo en Room | ✅ Probado | `ErrorNegocio` antes de escribir; 0 movimientos y saldo intacto |
| 1 usuario y 12 categorías aunque se inicialice varias veces | ✅ Probado | `InicializadorDatosTest` y `PersistenciaTest`; repetir no reinicia el saldo |
| Claves foráneas activas | ✅ Probado | Rechazan `idCategoria` inexistente y borrar una categoría con movimientos |
| Flow de saldo, totales, anillo y movimientos | ✅ Probado | Incluye una nueva emisión del resumen después de guardar |
| Desugaring de `java.time` para minSdk 24 | ✅ Comprobado en el APK | `dexdump`: `Periodo` del core y `Mapeos` llaman a `j$.time.LocalDate` |
| `java.time` ejecutándose en un dispositivo API 24–25 | ⏳ Pendiente | Solo hay imagen API 37 y no está instalado `sdkmanager` para descargar otra |
| Arranque de la app real crea y siembra `finanzas.db` | ✅ Resuelto en el Inc. 3 | Con los datos borrados (`pm clear`), la app real abrió Inicio con Q 0.00 y el formulario mostró las categorías leídas de Room. No se inspeccionó el archivo con `sqlite3` |
| Forzar el cierre de la app y conservar datos desde la interfaz | ✅ Resuelto en el Inc. 3 | Tras registrar Q 100 y Q 30 en la app real, `am force-stop` y reabrir conservó saldo Q 70.00 |
| Movimientos en USD | ✅ Inc. 6 | La v2 guarda la fotografía histórica y convierte con las reglas del core; ver resultados del Inc. 6 |

### Incremento 0 — conexión core → JAR → Android

| Comprobación | Estado | Detalle |
|---|---|---|
| `mvn clean install` con `0.1.0` | ✅ Ejecutado | En Windows, por la persona que desarrolla |
| Gradle resuelve el core, `assembleDebug` y emulador | ✅ Ejecutado | La app mostró el texto del JAR; también funciona **Run** de Android Studio |
| Referencia visual de Canva | ✅ Revisada y documentada | [Captura y contrato de interfaz](docs/referencias/INTERFAZ.md); la interfaz nativa llega en los incrementos 3–4 |

### Incremento 1 — reglas del core (`finanzas-core` 0.2.0)

| Comprobación | Estado | Detalle |
|---|---|---|
| Dominio, reglas, contratos, servicio y `Resultado` | ✅ Creados y compilados | Descripción de cada clase en el [mapa](docs/MAPA_DEL_CODIGO.md#2-core-qué-hace-cada-clase) |
| `mvn clean install` (incluye pruebas) | ✅ Ejecutado | 54 pruebas, 0 fallos; `BUILD SUCCESS`; JAR 0.2.0 instalado en `~/.m2` |
| Casos del diseño §14.1 | ✅ Probados en el core | a, b, f, g, h; parte pura de e; i en el core; parte pura de j |
| Bytecode del JAR | ✅ Comprobado | Versión de clase 55 (Java 11) con `javap` |
| El core no importa Android, Room, HTTP, XML ni JSON | ✅ Comprobado | Búsqueda de `import` en `finanzas-core/` |
| `dependencyInsight` resuelve `finanzas-core:0.2.0` | ✅ Ejecutado | Trae `kotlinx-coroutines-core` 1.10.2 de forma transitiva |
| `assembleDebug` con 0.2.0 | ✅ Ejecutado | `BUILD SUCCESSFUL` |
| El emulador muestra `finanzas-core 0.2.0 conectado…` | ✅ Ejecutado | AVD API 37 |
| Desugaring e implementaciones Room | ➡️ Resueltos en el Inc. 2 | Ver la tabla del incremento 2 |

Cómo se ejecutaron las comprobaciones en esta computadora: Maven 3.9.16 incluido en IntelliJ IDEA con su JDK 25, y Gradle con el JDK de Android Studio. Los pasos están en la [guía](docs/GUIA_WINDOWS.md).
