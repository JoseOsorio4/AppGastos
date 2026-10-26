# Construir y abrir los proyectos en Windows

Resultado esperado hoy: el core pasa sus pruebas y la app abre en Inicio, con `finanzas-core 0.3.0` al pie de la pantalla.

## 0. Obtener el código
- Con Git: `git clone https://github.com/JoseOsorio4/AppGastos.git` en una ruta corta y sin espacios, por ejemplo `C:\proyectos\AppGastos`.
- O descomprime el ZIP de entrega en esa misma ruta.

Dentro hay dos proyectos hermanos: `finanzas-core\` (Maven) y `finanzas-app\` (Gradle). Para editarlos desde Kiro IDE se abre la raíz `AppGastos`, siguiendo [la guía de continuidad](CONTINUAR_EN_KIRO_IDE.md).

## Requisitos
- JDK 17 o posterior para ejecutar Maven y Gradle (AGP 9.1.1 exige JDK 17 o más). El bytecode generado sigue siendo 11. Un JDK 11 en el `PATH` no sirve para Gradle; IntelliJ IDEA y Android Studio traen su propio JDK (carpeta `jbr`), que sí sirve.
- IntelliJ IDEA para el core y una versión de Android Studio que admita AGP 9.1.1.
- Android SDK Platform 37 (SDK Manager) y un emulador o dispositivo con Android 7.0 (API 24) o superior.
- Internet para descargar herramientas y dependencias que todavía no estén en caché (Gradle, AGP, appcompat, coroutines, JUnit y plugin Kotlin/Maven). La app instalada no usa red.

## 1. Construir, probar e instalar el core (IntelliJ)
1. `File > Open` y elige `finanzas-core\pom.xml` → **Open as Project**.
2. `File > Project Structure > SDK`: JDK 17 o superior.
3. Ventana **Maven** → `finanzas-core > Lifecycle`: ejecuta `clean` y luego `install`. `install` también corre las pruebas de `src/test/kotlin`.
4. Debe terminar con `Tests run: 55, Failures: 0, Errors: 0` y `BUILD SUCCESS`, y crear:
   `%USERPROFILE%\.m2\repository\gt\edu\umg\finanzas\finanzas-core\0.3.0\finanzas-core-0.3.0.jar`

**Desde PowerShell sin Maven en el `PATH`:** IntelliJ trae Maven en `<carpeta de IntelliJ>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd`. Basta con indicar el JDK solo para esa terminal:

```powershell
$env:JAVA_HOME = "<carpeta de IntelliJ>\jbr"
& "<carpeta de IntelliJ>\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" -f finanzas-core\pom.xml clean install
```

Para correr una sola clase de pruebas: `... mvn.cmd -f finanzas-core\pom.xml test -Dtest=CalculoSaldoTest`. En IntelliJ también se puede usar el triángulo verde junto a cada prueba.

## 2. Abrir y ejecutar la app (Android Studio)
1. `File > Open` y elige la carpeta **`finanzas-app`** (no `app`).
2. `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`: 17 o superior.
3. Si Android Studio propone actualizar AGP o Gradle, **no aceptes**.
4. Espera la sincronización. Android Studio crea `local.properties` con la ruta de tu SDK; ese archivo no se comparte.
5. Comprueba qué versión del core resolvió Gradle en PowerShell, desde `finanzas-app` (con `$env:JAVA_HOME` apuntando a `<carpeta de Android Studio>\jbr` si tu `java` es anterior a 17):
   `.\gradlew.bat :app:dependencyInsight --dependency finanzas-core --configuration debugRuntimeClasspath`
   Debe mencionar `gt.edu.umg.finanzas:finanzas-core:0.3.0`.
6. `.\gradlew.bat :app:assembleDebug`, luego ejecuta `app` en el emulador (o **Run**).
7. Pruebas instrumentadas (Room e interfaz), con el emulador abierto: `.\gradlew.bat :app:connectedDebugAndroidTest`. Deben pasar 33 pruebas; el informe queda en `app\build\reports\androidTests\connected\`. En Android Studio también se puede usar clic derecho sobre `app/src/androidTest` → **Run 'Tests in…'**.

## 3. Qué enviar después
La salida final (o el primer error completo) de `mvn clean install`, `dependencyInsight` y `assembleDebug`, y una captura del emulador. Con eso se actualiza el [README](../README.md#estado-real).

## Si algo falla
| Síntoma | Causa probable |
|---|---|
| `"mvn" no se reconoce como un comando` | Maven no está en el `PATH`: usa la ventana Maven de IntelliJ o su `mvn.cmd` (sección 1). |
| Gradle pide JDK 17 o falla al iniciar | El `java` del `PATH` es más antiguo: define `$env:JAVA_HOME` con el `jbr` de Android Studio para esa terminal. |
| `Could not find gt.edu.umg.finanzas:finanzas-core:0.3.0` | No ejecutaste `install`, o la versión del `pom.xml` y la de `gradle/libs.versions.toml` no coinciden. |
| Una prueba falla en `install` | Maven no instala el JAR: corrige la regla o la prueba y repite. El informe está en `finanzas-core\target\surefire-reports\`. |
| La pantalla dice `desconocida` | El recurso `finanzas-core.properties` no quedó dentro del JAR. |
| La pantalla dice `${project.version}` | Se perdió `<filtering>true</filtering>` en el `pom.xml`. |
| Error `Unresolved reference: kotlinOptions` | Configuración antigua: con AGP 9 se usa `kotlin { compilerOptions { … } }`. |
| Las pruebas de pantalla fallan con `RootViewWithoutFocusException` y la captura de error sale negra | El emulador dejó de dibujar. Ciérralo y ábrelo en frío (**Cold Boot Now** en el Device Manager) y repite las pruebas. |
| Maven y Android Studio parecen usar otra `.m2` | Revisa si `%USERPROFILE%\.m2\settings.xml` define un `localRepository` distinto. |

## Al cambiar el core
Sube `<version>` en `pom.xml` → `mvn clean install` → cambia `finanzasCore` en `libs.versions.toml` → **Sync** → reconstruye.

> El `gradle-wrapper.jar` se generó con Gradle 8.14.5 y descarga Gradle 9.3.1. Si se necesita regenerarlo con el propio 9.3.1: `.\gradlew.bat wrapper --gradle-version 9.3.1`.
