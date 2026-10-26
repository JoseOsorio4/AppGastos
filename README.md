# AppGastos — Finanzas personales en Kotlin

Proyecto de Programación 2 (Universidad Mariano Gálvez de Guatemala). Es una app Android para registrar ingresos y gastos en quetzales y ver de forma sencilla cuánto hay disponible, cuánto entró y cuánto salió en un período. Funciona sin conexión; las consultas por internet (tipo de cambio, clima, reflexión) son opcionales.

## Estructura

| Carpeta | Qué es | Herramienta |
|---|---|---|
| [`finanzas-core/`](finanzas-core/) | Modelos, reglas financieras e interfaces de almacenamiento. Kotlin/JVM, sin Android ni Room | Maven (IntelliJ IDEA) |
| [`finanzas-app/`](finanzas-app/) | Pantallas XML, ViewModels y persistencia con Room sobre SQLite. Consume el JAR del core | Gradle (Android Studio) |
| [`docs/`](docs/) | Documentación | — |

La app recibe el core como dependencia Maven `gt.edu.umg.finanzas:finanzas-core`, publicada en el repositorio Maven local (`~/.m2`).

La moneda base es el quetzal (GTQ). El conversor tiene únicamente **dólares a quetzales (USD → GTQ)** y **quetzales a dólares (GTQ → USD)**. Las reglas ya existen en el core (Inc. 1); la pantalla llega en el incremento 6.

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
3. **Pruebas instrumentadas (Room e interfaz):** con un emulador abierto, `.\gradlew.bat :app:connectedDebugAndroidTest` desde `finanzas-app`.

El core se instala **antes** de abrir la app, porque Gradle lo busca en `~/.m2`. Se necesita JDK 17 o superior. Las herramientas y dependencias que todavía no estén en caché requieren internet para descargarse.

## Estado real

Último incremento cerrado: **4 — Operaciones**. Siguiente: **5 — aviso de recálculo del saldo y selector de tema**.

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
| Aviso visible para recalcular el saldo y selector de tema | ⏳ Inc. 5 | — |

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
| Aviso visible para recalcular el saldo y selector de tema | ⏳ Inc. 5 | — |
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
| Movimientos en USD | ⏳ Pendiente (Inc. 6) | La v1 los rechaza con `ErrorNegocio` en el campo moneda |

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
