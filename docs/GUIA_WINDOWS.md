# Construir y abrir los proyectos en Windows

Objetivo del Incremento 0: que la app muestre `finanzas-core 0.1.0 conectado: este texto lo generó el JAR del core.`

## 0. Obtener el código
- Con Git: `git clone https://github.com/JoseOsorio4/AppGastos.git` en una ruta corta y sin espacios, por ejemplo `C:\proyectos\AppGastos`.
- O descomprime el ZIP de entrega en esa misma ruta.

Dentro hay dos proyectos hermanos: `finanzas-core\` (Maven) y `finanzas-app\` (Gradle). Para editarlos desde Kiro IDE se abre la raíz `AppGastos`, siguiendo [la guía de continuidad](CONTINUAR_EN_KIRO_IDE.md).

## Requisitos
- JDK 17 o posterior para ejecutar Maven y Gradle (AGP 9.1.1 exige JDK 17 o más). El bytecode generado sigue siendo 11.
- IntelliJ IDEA para el core y una versión de Android Studio que admita AGP 9.1.1.
- Android SDK Platform 37 (SDK Manager) y un emulador o dispositivo con Android 7.0 (API 24) o superior.
- Internet para descargar herramientas y dependencias que todavía no estén en caché (Gradle, AGP, appcompat y plugin Kotlin/Maven). La pantalla instalada del incremento 0 no usa red.

## 1. Construir e instalar el core (IntelliJ)
1. `File > Open` y elige `finanzas-core\pom.xml` → **Open as Project**.
2. `File > Project Structure > SDK`: JDK 17 o superior.
3. Ventana **Maven** → `finanzas-core > Lifecycle`: ejecuta `clean` y luego `install` (o en la terminal: `mvn clean install`).
4. Debe terminar en `BUILD SUCCESS` y crear:
   `%USERPROFILE%\.m2\repository\gt\edu\umg\finanzas\finanzas-core\0.1.0\finanzas-core-0.1.0.jar`

## 2. Abrir y ejecutar la app (Android Studio)
1. `File > Open` y elige la carpeta **`finanzas-app`** (no `app`).
2. `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`: 17 o superior.
3. Si Android Studio propone actualizar AGP o Gradle, **no aceptes** en este incremento.
4. Espera la sincronización. Android Studio crea `local.properties` con la ruta de tu SDK; ese archivo no se comparte.
5. Comprueba qué versión del core resolvió Gradle en PowerShell, desde `finanzas-app`:
   `.\gradlew.bat :app:dependencyInsight --dependency finanzas-core --configuration debugRuntimeClasspath`
   Debe mencionar `gt.edu.umg.finanzas:finanzas-core:0.1.0`.
6. `.\gradlew.bat :app:assembleDebug`, luego ejecuta `app` en el emulador.

## 3. Qué enviar después
La salida final (o el primer error completo) de `mvn clean install`, `dependencyInsight` y `assembleDebug`, y una captura del emulador. Con eso se marca el Incremento 0 como comprobado en el [README](../README.md#estado-real).

## Si algo falla
| Síntoma | Causa probable |
|---|---|
| `Could not find gt.edu.umg.finanzas:finanzas-core:0.1.0` | No ejecutaste `install`, o la versión del `pom.xml` y la de `gradle/libs.versions.toml` no coinciden. |
| La pantalla dice `desconocida` | El recurso `finanzas-core.properties` no quedó dentro del JAR. |
| La pantalla dice `${project.version}` | Se perdió `<filtering>true</filtering>` en el `pom.xml`. |
| Error `Unresolved reference: kotlinOptions` | Configuración antigua: con AGP 9 se usa `kotlin { compilerOptions { … } }`. |
| Maven y Android Studio parecen usar otra `.m2` | Revisa si `%USERPROFILE%\.m2\settings.xml` define un `localRepository` distinto. |

## Al cambiar el core
Sube `<version>` en `pom.xml` → `mvn clean install` → cambia `finanzasCore` en `libs.versions.toml` → **Sync** → reconstruye.

> El `gradle-wrapper.jar` se generó con Gradle 8.14.5 y descarga Gradle 9.3.1. Si se necesita regenerarlo con el propio 9.3.1: `.\gradlew.bat wrapper --gradle-version 9.3.1`.
