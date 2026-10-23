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
3. **Pruebas de Room:** con un emulador abierto, `.\gradlew.bat :app:connectedDebugAndroidTest` desde `finanzas-app`.

El core se instala **antes** de abrir la app, porque Gradle lo busca en `~/.m2`. Se necesita JDK 17 o superior. Las herramientas y dependencias que todavía no estén en caché requieren internet para descargarse.

## Estado real

Último incremento cerrado: **2 — Room v1 y operaciones atómicas**. Siguiente: **3 — Inicio y Formulario**.

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
| Arranque de la app real crea y siembra `finanzas.db` | ⏳ Pendiente | La app se instaló y abrió (`Status: ok`), pero no pude capturar la lista de bases ni la consulta con `sqlite3`; lo cubren las pruebas, no la app instalada |
| Forzar el cierre de la app y conservar datos desde la interfaz | ⏳ Pendiente (Inc. 3) | Todavía no hay pantalla que registre movimientos |
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
