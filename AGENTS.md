# Reglas de trabajo

Al comenzar una sesión, lee [README.md](README.md) (estado), [docs/MAPA_DEL_CODIGO.md](docs/MAPA_DEL_CODIGO.md) (rutas) y las secciones del [diseño](docs/DISENO_FINANZAS.md) que correspondan al incremento actual. Para pasar a Kiro IDE o a otra cuenta, sigue [la guía de continuidad](docs/CONTINUAR_EN_KIRO_IDE.md).

## Organización
- `finanzas-core` (Maven): modelos, reglas e interfaces. Nunca importa `android.*`, Room, HTTP, XML ni JSON.
- `finanzas-app` (Gradle): pantallas, ViewModels, Room y clientes remotos. Usa el core por su coordenada Maven; no copia sus fuentes.
- Persistencia solo con Room sobre SQLite: tablas `Usuario`, `Categoria` y `Transaccion`. Un movimiento y su efecto sobre el saldo se guardan en el mismo `withTransaction`.
- Dinero en `Long` de centavos; `BigDecimal` para texto y conversiones. `Double` y `Float` se reservan para coordenadas, temperatura y dibujo; nunca para montos ni cálculos monetarios.
- Solo existen GTQ y USD. Las dos únicas conversiones son USD → GTQ y GTQ → USD; no añadas otras monedas ni combinaciones. El saldo se mantiene en GTQ.
- La referencia de interfaz está en [docs/referencias/INTERFAZ.md](docs/referencias/INTERFAZ.md). Conserva Inicio, Formulario y Operaciones; las categorías del formulario se presentan en una cuadrícula de iconos y texto.
- Pantallas agrupadas por función en `ui/inicio`, `ui/formulario` y `ui/operaciones`. La Activity presenta y recibe acciones, el ViewModel coordina, el core valida y calcula, y el DAO consulta.
- Nombres descriptivos en español sin tildes (`botonGuardar`, `OperacionesMovimientoRoom`), salvo los sufijos de Android (`Activity`, `ViewModel`, `Adapter`).
- Cada regla vive en un solo lugar. Interfaces y clases base solo si tienen un consumidor real. Usa modelos con campos nombrados para los totales; evita `Pair.first` y `Pair.second` para distinguir ingresos de gastos.
- Guarda movimiento y saldo final dentro de la misma transacción. Comprueba `saldoActual + delta` con `Math.addExact` antes de escribir; la reconstrucción asigna el saldo calculado y apaga su bandera en otra operación atómica.
- Los comentarios explican decisiones concretas (centavos, tasa histórica, reversión) con frases breves y vocabulario del proyecto. Evita encabezados decorativos, párrafos por cada método y planes futuros dentro de las clases. Conserva los avisos de licencia de archivos de terceros.
- El mapa es la guía de defensa: por cada función implementada debe señalar XML/ID, Activity, ViewModel, regla del core y acceso a datos, según corresponda.

## Por incrementos
- Un incremento a la vez, en el orden del diseño (§14). No adelantes pantallas, tablas ni APIs.
- Al terminar un incremento, actualiza en el mismo commit las filas del mapa y el estado del [README](README.md#estado-real). Un agente implementa el incremento y la revisión se hace sobre ese mismo cambio.
- Cambiar el core: subir `<version>` → `mvn clean install` → actualizar `finanzasCore` en `libs.versions.toml`.
- No cambies versiones de herramientas sin una incompatibilidad comprobada. No uses `fallbackToDestructiveMigration`.

## Verificación
- Distingue siempre **creado**, **compilado** y **ejecutado**. Una comprobación que no corrió se marca como pendiente, con su motivo.
- Core: `mvn clean install` (incluye las pruebas). App: `gradlew :app:assembleDebug` y la prueba en el emulador.
- Los casos con resultado conocido están en el diseño (§14.1).
- Git: commits descriptivos, sin force push; no se versionan compilaciones, `local.properties`, claves, bases de datos ni ZIP.
