# AppGastos — Finanzas personales en Kotlin

Proyecto de Programación 2 (Universidad Mariano Gálvez de Guatemala). Es una app Android para registrar ingresos y gastos en quetzales y ver de forma sencilla cuánto hay disponible, cuánto entró y cuánto salió en un período. Funciona sin conexión; las consultas por internet (tipo de cambio, clima, reflexión) son opcionales.

## Estructura

| Carpeta | Qué es | Herramienta |
|---|---|---|
| [`finanzas-core/`](finanzas-core/) | Modelos, reglas financieras e interfaces de almacenamiento. Kotlin/JVM, sin Android ni Room | Maven (IntelliJ IDEA) |
| [`finanzas-app/`](finanzas-app/) | Pantallas XML, ViewModels y persistencia con Room sobre SQLite. Consume el JAR del core | Gradle (Android Studio) |
| [`docs/`](docs/) | Documentación | — |

La app **no** contiene los fuentes del core: los recibe como dependencia Maven `gt.edu.umg.finanzas:finanzas-core`, publicada en el repositorio Maven local (`~/.m2`).

## Documentación

- [Diseño](docs/DISENO_FINANZAS.md): alcance, decisiones, modelo de datos, flujos e incrementos.
- [Mapa del código](docs/MAPA_DEL_CODIGO.md): cada acción visible con su XML, Activity, ViewModel, regla y almacenamiento; preguntas de defensa.
- [Guía de Windows](docs/GUIA_WINDOWS.md): pasos detallados y solución de errores.
- [AGENTS.md](AGENTS.md): reglas de trabajo en este repositorio.

## Orden para construir

1. **Core:** abre `finanzas-core/pom.xml` en IntelliJ y ejecuta `mvn clean install`.
2. **App:** abre la carpeta `finanzas-app/` en Android Studio, sincroniza Gradle y ejecuta `app`.

El core se instala **antes** de abrir la app, porque Gradle lo busca en `~/.m2`. Se necesita JDK 17 o superior y, solo en la primera construcción, internet para descargar Gradle y las dependencias.

## Estado real

Incremento actual: **0 — conexión core → JAR → Android**.

| Comprobación | Estado | Detalle |
|---|---|---|
| Archivos de ambos proyectos, wrapper de Gradle 9.3.1 y documentación | ✅ Creados | — |
| `InfoCore` compila con destino JVM 11 y funciona desde un JAR | ⚠️ Solo con una comprobación sustituta | Compilador Kotlin 2.1.0 local (no Maven): bytecode 55 y texto correcto |
| `mvn clean install` | ❌ Pendiente | El entorno de desarrollo no tiene acceso a Maven Central |
| Gradle resuelve `finanzas-core:0.1.0` | ❌ Pendiente | No se pudo descargar Gradle 9.3.1 |
| `assembleDebug` | ❌ Pendiente | Sin Android SDK en el entorno |
| La app muestra el texto del core en el emulador | ❌ Pendiente | Sin emulador |
| Contraste con la maqueta de Canva | ❌ Pendiente | Las referencias visuales aún no están en el repositorio ([diseño §3.0](docs/DISENO_FINANZAS.md#30-contraste-con-las-referencias-visuales--pendiente)) |

Las pendientes se comprueban en Windows siguiendo la [guía](docs/GUIA_WINDOWS.md).
