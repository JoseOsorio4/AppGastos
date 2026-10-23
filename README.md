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

El core se instala **antes** de abrir la app, porque Gradle lo busca en `~/.m2`. Se necesita JDK 17 o superior. Las herramientas y dependencias que todavía no estén en caché requieren internet para descargarse.

## Estado real

Último incremento cerrado: **1 — reglas del core**. Siguiente: **2 — Room v1 y operaciones atómicas**.

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
| Uso de `java.time` del core en Android 7.0–7.1 (API 24–25) | ⏳ Pendiente (Inc. 2) | Necesita `coreLibraryDesugaring`; la app todavía no llama a esas clases |
| Implementaciones Room de los contratos | ⏳ Pendiente (Inc. 2) | Las pruebas usan dobles en memoria |

Cómo se ejecutaron las comprobaciones en esta computadora: Maven 3.9.16 incluido en IntelliJ IDEA con su JDK 25, y Gradle con el JDK de Android Studio. Los pasos están en la [guía](docs/GUIA_WINDOWS.md).
