# Continuar en Kiro IDE desde otra cuenta

El proyecto continúa en este repositorio:
https://github.com/JoseOsorio4/AppGastos

La cuenta anterior ya dejó la estructura y el incremento 0. Para continuar se abre el mismo código; no se crea otro proyecto Android ni otra librería.

## 1. Obtener y abrir el repositorio

Si todavía no tienes una copia local, abre una terminal en la carpeta donde guardas tus proyectos y ejecuta:

```powershell
git clone https://github.com/JoseOsorio4/AppGastos.git
```

En Kiro IDE, inicia sesión con la cuenta que utilizarás para programar y abre la carpeta AppGastos mediante File > Open Folder. La carpeta abierta debe contener AGENTS.md, README.md, docs, finanzas-core y finanzas-app.

Si ya lo habías clonado, abre esa copia. Comprueba git status; conserva los cambios locales antes de actualizar. Con la carpeta limpia, git pull --ff-only trae el avance remoto sin reescribir el historial.

Los archivos del repositorio proporcionan el contexto a la nueva cuenta. No es necesario trasladar todo el chat anterior ni pedir otra arquitectura.

## 2. Qué abrir en cada herramienta

| Herramienta | Qué se abre | Propósito |
|---|---|---|
| Kiro IDE | Carpeta AppGastos completa | Editar el core, la app y la documentación con el mismo contexto |
| IntelliJ IDEA | finanzas-core/pom.xml | Construir e instalar la librería Maven |
| Android Studio | Carpeta finanzas-app | Sincronizar Gradle, configurar SDK y ejecutar en emulador o dispositivo |

Los dos proyectos permanecen independientes como unidades de construcción, dentro de un único repositorio Git.

## 3. Primer mensaje para la nueva cuenta

Copia este mensaje en un chat de Kiro IDE con AppGastos abierto:

> Continúa el proyecto existente en https://github.com/JoseOsorio4/AppGastos.
>
> Lee AGENTS.md, README.md, docs/MAPA_DEL_CODIGO.md y docs/GUIA_WINDOWS.md. Consulta las secciones 13 a 15 de docs/DISENO_FINANZAS.md para el incremento actual. La referencia visual está en docs/referencias/INTERFAZ.md y docs/referencias/canva-inicio.png.
>
> Mi prioridad es una estructura fácil de explicar: cada vista, regla, consulta y operación debe tener un lugar claro. Usa comentarios breves sobre decisiones concretas y actualiza el mapa con los archivos reales.
>
> Solo se admiten GTQ y USD. El conversor tendrá exactamente dos direcciones: USD a GTQ y GTQ a USD. El saldo siempre se conserva en quetzales.
>
> Primero resume brevemente el estado real y continúa con el cierre del incremento 0 en esta computadora. Comprueba JDK, Maven y Android SDK. Construye e instala finanzas-core con mvn clean install; desde finanzas-app comprueba la dependencia con .\gradlew.bat :app:dependencyInsight --dependency finanzas-core --configuration debugRuntimeClasspath y construye con .\gradlew.bat :app:assembleDebug. Ejecuta la app en un emulador o dispositivo disponible y comprueba el mensaje recibido del JAR.
>
> Corrige los errores concretos que aparezcan conservando el trabajo existente. Ajusta versiones solo ante una incompatibilidad comprobada. Si falta una herramienta o un acceso, informa del paso exacto pendiente y completa las verificaciones independientes disponibles.
>
> Al terminar, actualiza README.md y el mapa con lo comprobado; guarda los cambios con commit y push normal al repositorio conectado. Entrega los resultados, la rama y el enlace del commit. Cierra esta tarea en el incremento 0; el incremento 1 se implementará en la siguiente tarea.

## 4. Punto de cierre

El incremento 0 está terminado cuando Maven instala el JAR, Gradle resuelve la coordenada correcta, assembleDebug termina correctamente y la pantalla Android muestra el mensaje del core.

La aplicación financiera, Room y las APIs aún se implementarán por incrementos. Una revisión de documentación o una comprobación de archivos no sustituye esos resultados.

La guía detallada de compilación y errores está en [GUIA_WINDOWS.md](GUIA_WINDOWS.md). El [README](../README.md#estado-real) conserva el estado de las verificaciones.
