# Continuar en Kiro IDE desde otra cuenta

El proyecto continúa en este repositorio:
https://github.com/JoseOsorio4/AppGastos

Los incrementos 0 a 7 tienen sus funciones verificadas en API 37: core, Room, pantallas, recálculo/tema, GTQ/USD con Banguat, clima y reflexión RV1909 con Dabar y respaldo local. No hay un incremento 8 definido. Consulta `git log -1` y el [estado real](../README.md#estado-real) para identificar las comprobaciones y los pendientes generales antes de editar.

Para continuar se abre el mismo código; no se crea otro proyecto Android ni otra librería.

Fuentes actuales, en este orden:
- [README.md](../README.md#estado-real): estado de cada comprobación (ejecutada o pendiente).
- [MAPA_DEL_CODIGO.md](MAPA_DEL_CODIGO.md): qué archivo, ID, Activity, ViewModel, regla y consulta corresponde a cada función.
- [DISENO_FINANZAS.md](DISENO_FINANZAS.md): alcance, modelo de datos e incrementos (§14); el Inc. 5 se apoya también en §9.6 y §12.
- [INTERFAZ.md](referencias/INTERFAZ.md): referencia visual y comparaciones en el emulador.

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

Copia este mensaje en un chat nuevo de Kiro IDE con AppGastos abierto:

> Continúa AppGastos (https://github.com/JoseOsorio4/AppGastos), rama main. Los incrementos 0–7 tienen sus funciones verificadas en API 37. Revisa git status y git log -1 antes de editar.
>
> Lee AGENTS.md, README.md (Estado real), docs/MAPA_DEL_CODIGO.md, docs/DISENO_FINANZAS.md y docs/referencias/INTERFAZ.md. Resume en pocas líneas el estado y los pendientes que figuran en el README.
>
> El incremento 7 ya tiene clima Open-Meteo y reflexión RV1909 desde Dabar con caché diaria y respaldo local. No vuelvas a implementar esa integración ni crees otro proveedor. Revisa los pendientes del README y limita el cambio a lo que solicite el usuario. Conserva el estilo y las capas: XML dibuja, Activity recibe y pinta, ViewModel coordina el estado, el core decide, Room guarda. No rediseñes las finanzas ni cambies tasas históricas.
>
> Antes de cambiar la app, comprueba mvn clean install y .\gradlew.bat :app:assembleDebug. Usa -PdatosPrueba al instalar o ejecutar pruebas de interfaz para proteger los datos de la app normal. Distingue lo ejecutado de lo pendiente. No subas rutas locales, .bak, configuración del IDE ni finanzas-app/app/img.png. Al terminar, actualiza README y mapa, haz commit y push normal y dame el enlace.

## 4. Punto de cierre

Cada incremento termina cuando se observa lo indicado en la tabla de §14 del diseño. Las funciones de los incrementos 0 a 7 se verificaron en API 37; la continuación de reflexión terminó con 65 pruebas habituales sin red y una petición Dabar real separada. Esto no declara resueltas las comprobaciones generales pendientes.

Pendientes del incremento 4, sin ejecutar todavía:
- **Segundo detalle que se cerró solo:** en el Inc. 5 se revisó de nuevo con acciones individuales de ADB: tras cancelar, el segundo detalle permaneció abierto 372 s y otra reapertura, 48 s. No se reprodujo, no se cambió Operaciones y no se identificó una causa. La comprobación humana con el dedo sigue pendiente.
- **Rotación con diálogos abiertos:** rotar a mano con el detalle o con la confirmación de eliminar abiertos. Son `DialogFragment`, pero no se ha rotado el emulador con ellos.
- **Muerte real del proceso con el formulario abierto:** solo se probó la recuperación desde un `SavedStateHandle` precargado (`FormularioViewModelTest`).
- **API 24–25:** solo existe la imagen API 37 del emulador.

El archivo `finanzas-app/app/img.png` es local y no identificado: no se modifica ni se incluye en ningún commit.

La tasa Banguat se consulta y conserva desde el Inc. 6. El Inc. 7 añade clima Open-Meteo y reflexión diaria RV1909 de Dabar con respaldo local. Bolls quedó descartado; Dabar se verificó con documentación, licencia, tres respuestas reales guardadas y una petición desde Android. Las fuentes y comandos están en [la verificación de servicios](referencias/SERVICIOS_INCREMENTO_7.md). Una revisión de documentación no sustituye las ejecuciones. El fallo forzado de escritura de preferencias de reflexión también continúa pendiente.

La guía detallada de compilación y errores está en [GUIA_WINDOWS.md](GUIA_WINDOWS.md). El [README](../README.md#estado-real) conserva el estado de las verificaciones.
