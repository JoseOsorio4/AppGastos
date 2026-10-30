# Verificación de proveedores del incremento 7

Consultas del 30/10/2026, antes de fijar rutas y lectores. Los archivos de respuesta se conservan sin modificar en `finanzas-app/app/src/androidTest/assets/`; no alimentan la caché de producción.

## Jutiapa y Open-Meteo

- [Contrato del geocodificador](https://open-meteo.com/en/docs/geocoding-api). GET `https://geocoding-api.open-meteo.com/v1/search?name=Jutiapa&count=10&language=es&format=json&countryCode=GT`: HTTP 200. Archivo `openmeteo/jutiapa-geocodificacion.json`.
- Resultado: ID 3595069, Jutiapa, `country_code=GT`, `admin1=Departamento de Jutiapa`, `admin2=Jutiapa`, `feature_code=PPLA`, zona `America/Guatemala`, latitud **14.28491**, longitud **−89.88798**. Es la cabecera, no una ubicación obtenida del teléfono. Se fijan esas coordenadas en `ClienteOpenMeteo`; no se pide GPS.
- [Contrato del clima y códigos WMO](https://open-meteo.com/en/docs). GET `https://api.open-meteo.com/v1/forecast?latitude=14.28491&longitude=-89.88798&current=temperature_2m,weather_code&timezone=America%2FGuatemala`: HTTP 200. Archivo `openmeteo/clima-2026-10-30.json`.
- Respuesta guardada: `current.time=2026-10-30T00:45`, `temperature_2m=18.3`, `weather_code=0`; unidades `°C`, `wmo code`, `iso8601`. Las coordenadas devueltas (14.235501, −89.91684) son el centro de la celda meteorológica; el contrato advierte que puede quedar a unos kilómetros del punto solicitado. No se sustituyen por ellas las coordenadas de la cabecera.
- [Términos](https://open-meteo.com/en/terms) y [atribución](https://open-meteo.com/en/licence): acceso gratuito para uso no comercial, sin clave, datos CC BY 4.0. La tarjeta identifica «Datos del clima: Open-Meteo.com», enlaza al proveedor y muestra CC BY 4.0. El uso educativo de esta app y su caché de 30 minutos no requieren una clave comercial.
- El lector exige unidades, zona, hora, temperatura finita y código entero; no sustituye datos nulos por cero. La condición se traduce en recursos de UI. Un código desconocido muestra «Condición no disponible».

## Reflexión: proveedor remoto pendiente

- Se revisó [la documentación de Bolls](https://github.com/Bolls-Bible/bain/blob/master/docs/API.md), que remite al catálogo de traducciones y a rutas por traducción/libro/capítulo/versículo. La documentación contiene ejemplos de `get-verse` y una mención de `get-text`; no se fijó un lector suponiendo que fueran equivalentes.
- GET `https://bolls.life/static/bolls/app/views/languages.json`: HTTP 200, guardado como `bolls/idiomas-2026-10-30.json`. Su bloque `Spanish Español` contiene BTX3, RV1960, RV2004, PDT, NVI y NTV/LBLA, **ninguna Reina-Valera 1909**. No se atribuye a RV1960 la licencia de RV1909.
- La ruta candidata `https://bolls.life/get-verse/RV1909/19/23/1/` respondió **HTTP 404**, cuerpo `The verse is not found`. `RV1909` no es un código verificado del catálogo; esta petición demuestra el fallo, no un contrato viable. No se implementaron cliente ni lector Bolls ni se presentó una respuesta exitosa ficticia. Dabar no se integró: no se ha verificado su contrato y licencia en esta sesión.
- **Bloqueo:** no se pudo verificar un código y una respuesta exitosa RV1909 de Bolls. Queda pendiente elegir/verificar un único proveedor remoto compatible y añadir su respuesta real y pruebas. La app usa solo el catálogo local permitido por §11.4.

## Catálogo local y derecho de uso

[eBible identifica expresamente Reina-Valera 1909 como dominio público](https://ebible.org/spaRV1909/copyright.htm). Se verificaron los tres textos en sus capítulos, sin modernizar palabras ni inventar autores:

| Referencia | Texto del catálogo | Fuente |
|---|---|---|
| Salmos 23:1 | Salmo de David. JEHOVÁ es mi pastor; nada me faltará. | [Salmos 23](https://ebible.org/spaRV1909/PSA023.htm) |
| Proverbios 16:3 | Encomienda á Jehová tus obras, y tus pensamientos serán afirmados. | [Proverbios 16](https://ebible.org/spaRV1909/PRO16.htm) |
| Filipenses 4:13 | Todo lo puedo en Cristo que me fortalece. | [Filipenses 4](https://ebible.org/spaRV1909/PHP04.htm) |

`CatalogoReflexiones` conserva esas tres citas. `RepositorioReflexion` elige por fecha de Guatemala y guarda texto, referencia y fecha una vez al día. La pantalla dice **Catálogo local · Dominio público · Reina-Valera 1909**; nunca Bolls ni «descargado». Ocultar/Mostrar persiste sin borrar la cita. No hay consulta remota de reflexión que pueda fallar o bloquear las finanzas.

## Repetir las comprobaciones

Desde `finanzas-app`, con un emulador abierto y dependencias de construcción ya disponibles:

```powershell
# Suite habitual: fixtures, repositorios, Room y UI; puede correr con wifi/datos desactivados.
.\gradlew.bat -PdatosPrueba :app:connectedDebugAndroidTest

# Solo peticiones reales marcadas PruebaEnVivo: Banguat y Open-Meteo. Requiere internet.
.\gradlew.bat -PdatosPrueba -PpruebasEnVivo :app:connectedDebugAndroidTest

# Solo Banguat en vivo.
.\gradlew.bat -PdatosPrueba -PpruebasEnVivo '-Pandroid.testInstrumentationRunnerArguments.class=gt.edu.umg.finanzas.app.datos.remoto.BanguatEnVivoTest' :app:connectedDebugAndroidTest
```

El filtro por anotación está en `app/build.gradle.kts`. Sin `pruebasEnVivo`, el runner excluye las peticiones reales; con la propiedad ejecuta solo esas pruebas. Una caída del proveedor debe aparecer como fallo del recorrido en vivo, sin afectar a los fixtures. La instalación `.pruebas` y las comprobaciones de paquete de `AyudaInterfaz` protegen la base de la instalación normal. El XML de resultados se reemplaza en cada ejecución; los recuentos ejecutados quedan en el README.
