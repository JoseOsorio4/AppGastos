# Verificación de proveedores del incremento 7

Consultas del 30/10/2026, antes de fijar rutas y lectores. Los archivos de respuesta se conservan sin modificar en `finanzas-app/app/src/androidTest/assets/`; no alimentan la caché de producción.

## Jutiapa y Open-Meteo

- [Contrato del geocodificador](https://open-meteo.com/en/docs/geocoding-api). GET `https://geocoding-api.open-meteo.com/v1/search?name=Jutiapa&count=10&language=es&format=json&countryCode=GT`: HTTP 200. Archivo `openmeteo/jutiapa-geocodificacion.json`.
- Resultado: ID 3595069, Jutiapa, `country_code=GT`, `admin1=Departamento de Jutiapa`, `admin2=Jutiapa`, `feature_code=PPLA`, zona `America/Guatemala`, latitud **14.28491**, longitud **−89.88798**. Es la cabecera, no una ubicación obtenida del teléfono. Se fijan esas coordenadas en `ClienteOpenMeteo`; no se pide GPS.
- [Contrato del clima y códigos WMO](https://open-meteo.com/en/docs). GET `https://api.open-meteo.com/v1/forecast?latitude=14.28491&longitude=-89.88798&current=temperature_2m,weather_code&timezone=America%2FGuatemala`: HTTP 200. Archivo `openmeteo/clima-2026-10-30.json`.
- Respuesta guardada: `current.time=2026-10-30T00:45`, `temperature_2m=18.3`, `weather_code=0`; unidades `°C`, `wmo code`, `iso8601`. Las coordenadas devueltas (14.235501, −89.91684) son el centro de la celda meteorológica; el contrato advierte que puede quedar a unos kilómetros del punto solicitado. No se sustituyen por ellas las coordenadas de la cabecera.
- [Términos](https://open-meteo.com/en/terms) y [atribución](https://open-meteo.com/en/licence): acceso gratuito para uso no comercial, sin clave, datos CC BY 4.0. La tarjeta identifica «Datos del clima: Open-Meteo.com», enlaza al proveedor y muestra CC BY 4.0. El uso educativo de esta app y su caché de 30 minutos no requieren una clave comercial.
- El lector exige unidades, zona, hora, temperatura finita y código entero; no sustituye datos nulos por cero. La condición se traduce en recursos de UI. Un código desconocido muestra «Condición no disponible».

## Reflexión: comprobación anterior de Bolls

- Se revisó [la documentación de Bolls](https://github.com/Bolls-Bible/bain/blob/master/docs/API.md), que remite al catálogo de traducciones y a rutas por traducción/libro/capítulo/versículo. La documentación contiene ejemplos de `get-verse` y una mención de `get-text`; no se fijó un lector suponiendo que fueran equivalentes.
- GET `https://bolls.life/static/bolls/app/views/languages.json`: HTTP 200, guardado como `bolls/idiomas-2026-10-30.json`. Su bloque `Spanish Español` contiene BTX3, RV1960, RV2004, PDT, NVI y NTV/LBLA, **ninguna Reina-Valera 1909**. No se atribuye a RV1960 la licencia de RV1909.
- La ruta candidata `https://bolls.life/get-verse/RV1909/19/23/1/` respondió **HTTP 404**, cuerpo `The verse is not found`. `RV1909` no es un código verificado del catálogo; esta petición demuestra el fallo, no un contrato viable. No se implementaron cliente ni lector Bolls ni se presentó una respuesta exitosa ficticia. Dabar no se integró: no se ha verificado su contrato y licencia en esta sesión.
- **Resultado de aquella comprobación:** no se pudo verificar un código y una respuesta exitosa RV1909 de Bolls. No se integra Bolls. La continuación siguiente verifica Dabar como único proveedor remoto.

## Reflexión: Dabar verificado antes de implementar

Comprobación del 30/10/2026 en la continuación de `4df6fa8`, antes de escribir cliente o lector:

- [Documentación y términos de Dabar](https://dabar.io/): consulta de una referencia por `BOOK CHAPTER:VERSE`, parámetro `translation`, respuestas JSON. Publica el identificador **`rv1909`** y permite usar el servicio gratuitamente sin abusar del servidor; indica un límite de 15 peticiones por 30 segundos y disponibilidad sin garantía. La cabecera de esa página conserva el título bible-api.com; las URLs comprobadas y la integración usan **solo dabar.io**.
- [Catálogo real](https://dabar.io/data), HTTP 200: `identifier=rv1909`, `name=Reina Valera 1909`, `language=Spanish`, `language_code=spa`, `license=Public Domain`. Respuesta guardada sin sustituir campos en `app/src/androidTest/assets/dabar/catalogo-2026-10-30.json`. Se corroboró el dominio público de esa traducción en [eBible](https://ebible.org/spaRV1909/copyright.htm); no se transfieren esos derechos a RV1960 ni a otra revisión.
- Ruta exacta de una cita breve: `https://dabar.io/Filipenses%204%3A13?translation=rv1909`, HTTP 200. El nombre inglés `philippians` devolvió HTTP 404; el cliente codifica **la referencia en español**, no deduce rutas de aquel intento fallido.
- Se comprobaron también `https://dabar.io/Salmos%2023%3A1?translation=rv1909` y `https://dabar.io/Proverbios%2016%3A3?translation=rv1909`, ambas HTTP 200. Salmos tuvo un timeout de 20 s en la primera comprobación de escritorio; una repetición devolvió la respuesta real. No se oculta esa inestabilidad ni se inventa un éxito para el intento fallido.

| Respuesta guardada en `app/src/androidTest/assets/dabar/` | Campos reales relevantes |
|---|---|
| `filipenses-2026-10-30.json` | `reference=Filipenses 4:13`, un elemento en `verses`, `book_id=PHP`, `book_name=Filipenses`, `chapter=4`, `verse=13`, texto «Todo lo puedo en Cristo que me fortalece.» con saltos exteriores |
| `salmos-2026-10-30.json` | `reference=Salmos 23:1`, un versículo, `book_id=PSA`, capítulo 23, versículo 1 |
| `proverbios-2026-10-30.json` | `reference=Proverbios 16:3`, un versículo, `book_id=PRO`, capítulo 16, versículo 3 |

Las tres respuestas incluyen `text` del pasaje, `translation_id=rv1909`, `translation_name=Reina Valera 1909` y `translation_note=Public Domain`. `LectorRespuestaReflexion` exige esos metadatos, la referencia solicitada, un solo versículo con capítulo/número enteros y texto coherente entre pasaje y versículo. Rechaza nulos, campos ausentes, HTML, texto vacío o mayor de 300 caracteres. Solo recorta espacios/saltos exteriores; no moderniza ni reescribe la cita. La fuente visible se fija como **Dabar.io** después de validar la respuesta.

`ClienteDabar` usa el `ClienteHttp` existente y procesa el JSON fuera del hilo principal. `RepositorioReflexion` conserva el selector diario del catálogo, consulta ese mismo pasaje y guarda texto, referencia, traducción, fuente y fecha con las claves existentes. No añade tablas, migraciones ni dependencias.

Una cita ya guardada para hoy, **local o remota**, se conserva al reabrir y evita otra consulta automática. Esto también respeta la cita local que tuviera instalada la versión anterior; **Actualizar reflexión** permite consultar explícitamente ese día. Si falla la petición, se conserva la cita válida de hoy o se guarda la local de hoy. Un nuevo día sin red elige el catálogo de ese día, sin poner la fecha de hoy ni la atribución local sobre una cita remota antigua. El botón pasa a **Reintentar reflexión** ante error; una petición explícita puede recuperar el contenido remoto. La visibilidad sigue siendo independiente y persistente.

## Catálogo local y derecho de uso

[eBible identifica expresamente Reina-Valera 1909 como dominio público](https://ebible.org/spaRV1909/copyright.htm). Se verificaron los tres textos en sus capítulos, sin modernizar palabras ni inventar autores:

| Referencia | Texto del catálogo | Fuente |
|---|---|---|
| Salmos 23:1 | Salmo de David. JEHOVÁ es mi pastor; nada me faltará. | [Salmos 23](https://ebible.org/spaRV1909/PSA023.htm) |
| Proverbios 16:3 | Encomienda á Jehová tus obras, y tus pensamientos serán afirmados. | [Proverbios 16](https://ebible.org/spaRV1909/PRO16.htm) |
| Filipenses 4:13 | Todo lo puedo en Cristo que me fortalece. | [Filipenses 4](https://ebible.org/spaRV1909/PHP04.htm) |

`CatalogoReflexiones` conserva esas tres citas como respaldo. Cuando se usa, la pantalla identifica **Catálogo local · eBible · Dominio público · Reina-Valera 1909**. Una respuesta remota válida identifica **Dabar.io · Reina Valera 1909**, con su referencia y fecha. Ocultar/Mostrar persiste sin borrar la cita. Un fallo de reflexión no cancela clima, tasa ni consultas financieras.

## Repetir las comprobaciones

Desde `finanzas-app`, con un emulador abierto y dependencias de construcción ya disponibles:

```powershell
# Suite habitual: fixtures, repositorios, Room y UI; puede correr con wifi/datos desactivados.
.\gradlew.bat -PdatosPrueba :app:connectedDebugAndroidTest

# Solo peticiones reales marcadas PruebaEnVivo: Banguat, Open-Meteo y Dabar. Requiere internet.
.\gradlew.bat -PdatosPrueba -PpruebasEnVivo :app:connectedDebugAndroidTest

# Solo Dabar en vivo (ejecutado en esta continuación).
.\gradlew.bat -PdatosPrueba -PpruebasEnVivo '-Pandroid.testInstrumentationRunnerArguments.class=gt.edu.umg.finanzas.app.datos.remoto.DabarEnVivoTest' :app:connectedDebugAndroidTest

# Solo Banguat en vivo.
.\gradlew.bat -PdatosPrueba -PpruebasEnVivo '-Pandroid.testInstrumentationRunnerArguments.class=gt.edu.umg.finanzas.app.datos.remoto.BanguatEnVivoTest' :app:connectedDebugAndroidTest
```

El filtro por anotación está en `app/build.gradle.kts`. Sin `pruebasEnVivo`, el runner excluye las peticiones reales; con la propiedad ejecuta solo esas pruebas. Una caída del proveedor debe aparecer como fallo del recorrido en vivo, sin afectar a los fixtures. La instalación `.pruebas` y las comprobaciones de paquete de `AyudaInterfaz` protegen la base de la instalación normal. El XML de resultados se reemplaza en cada ejecución; los recuentos ejecutados quedan en el README.
