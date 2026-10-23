# Reglas de trabajo

Antes de cambiar algo, lee [docs/DISENO_FINANZAS.md](docs/DISENO_FINANZAS.md) (decisiones) y [docs/MAPA_DEL_CODIGO.md](docs/MAPA_DEL_CODIGO.md) (dónde está cada cosa).

## Organización
- `finanzas-core` (Maven): modelos, reglas e interfaces. Nunca importa `android.*`, Room, HTTP, XML ni JSON.
- `finanzas-app` (Gradle): pantallas, ViewModels, Room y clientes remotos. Usa el core por su coordenada Maven; no copia sus fuentes.
- Persistencia solo con Room sobre SQLite: tablas `Usuario`, `Categoria` y `Transaccion`. Un movimiento y su efecto sobre el saldo se guardan en el mismo `withTransaction`.
- Dinero en `Long` de centavos; `BigDecimal` para texto y conversiones; nunca `Double` ni `Float`.
- Pantallas agrupadas por función en `ui/inicio`, `ui/formulario` y `ui/operaciones`. La Activity presenta y recibe acciones, el ViewModel coordina, el core valida y calcula, y el DAO consulta.
- Nombres descriptivos en español sin tildes (`botonGuardar`, `OperacionesMovimientoRoom`), salvo los sufijos de Android (`Activity`, `ViewModel`, `Adapter`).
- Cada regla vive en un solo lugar. Interfaces y clases base solo si tienen un consumidor real.
- Los comentarios explican decisiones (centavos, tasa histórica, reversión), no repiten el código.

## Por incrementos
- Un incremento a la vez, en el orden del diseño (§14). No adelantes pantallas, tablas ni APIs.
- Al terminar un incremento, actualiza en el mismo commit las filas del mapa y el estado del [README](README.md#estado-real).
- Cambiar el core: subir `<version>` → `mvn clean install` → actualizar `finanzasCore` en `libs.versions.toml`.
- No cambies versiones de herramientas sin una incompatibilidad comprobada. No uses `fallbackToDestructiveMigration`.

## Verificación
- Distingue siempre **creado**, **compilado** y **ejecutado**. Una comprobación que no corrió se marca como pendiente, con su motivo.
- Core: `mvn clean install` (incluye las pruebas). App: `gradlew :app:assembleDebug` y la prueba en el emulador.
- Los casos con resultado conocido están en el diseño (§14.1).
- Git: commits descriptivos, sin force push; no se versionan compilaciones, `local.properties`, claves, bases de datos ni ZIP.
