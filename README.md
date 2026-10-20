This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Conectividad REST

La pantalla Productos consulta `GET /api/v1/productos?pagina=0&tamanio=20` del backend [PharmaMobile-Backend](https://github.com/07Ismael02/PharmaMobile-Backend). En el emulador Android la URL base es `http://10.0.2.2:8080/api/v1/`; en iOS se configura `http://localhost:8080/api/v1/`. El backend debe estar iniciado y accesible desde el dispositivo correspondiente.

El cliente Ktor compartido usa `ContentNegotiation` con JSON (`ignoreUnknownKeys = true`), `HttpTimeout` (15 segundos por petición y 10 para conectar) y `Logging` en nivel `HEADERS`. Android usa OkHttp y envía esos mensajes a Logcat con la etiqueta `PharmaMobilKtor`; iOS usa Darwin. `ProductoApi` recibe el envoltorio `PaginaResponseDto`, extrae `contenido` y el mapper convierte cada `ProductoResponseDto` al modelo de dominio, incluido `estado` → `activo`. La UI y el ViewModel no consumen DTO directamente.

En Sesión 07, `listar()` consulta REST mientras `registrar()` permanece en memoria; el CRUD REST completo corresponde a Sesión 08. La lista con tres productos del backend y el registro de Logcat se comprobaron en Android. iOS está configurado, pero no se ha compilado ni ejecutado desde este equipo Windows.

## CRUD REST de productos - Sesión 08

La rama `feature/crud-productos-logacho` conecta listar, obtener por ID, crear, actualizar y dar de baja lógica mediante el backend propio. La categoría se obtiene de `GET /api/v1/categorias` y se selecciona en el formulario; no se usa un ID supuesto. Tras cada mutación se recarga el listado. Una baja marca `estado=false`, por lo que deja de verse en Activos y permanece en Inactivos.

Los errores HTTP y de red se traducen en la capa de datos a `ErrorApi`: 400 por campo, 404, 409, 5xx, sin conexión y timeout. La cancelación de corrutinas se propaga. El estado `Operacion` separa guardar/eliminar de la fase de carga de la lista. El registro de pruebas y evidencias está en [docs/s08-guia-practica.md](docs/s08-guia-practica.md).

## Manejo de errores

`EjecutarLlamada` es el único punto que traduce respuestas y excepciones de Ktor al dominio: 400 → `Validacion`, 404 → `NoEncontrado`, 409 → `Conflicto`, 5xx → `Servidor`, fallo de E/S → `SinConexion` y timeout → `TiempoAgotado`. `CancellationException` se relanza; no se muestra como error. El ViewModel conserva la lista visible ante errores de una operación y coloca los mensajes de validación del backend bajo su campo. La bitácora de la actividad autónoma S08 está en [docs/s08-actividad-autonoma.md](docs/s08-actividad-autonoma.md).

## Capacidades nativas - Sesión 09

`formatearSoles` se declara con `expect` en `commonMain` y se implementa con `actual` en `androidMain`; el mapeo a `ProductoUi` prepara el precio para mostrarlo. El texto para compartir se construye en código común desde `Producto` y reutiliza ese formateador. `Compartidor` es un contrato común; Koin proporciona `CompartidorAndroid`, que abre el selector nativo mediante `ACTION_SEND` desde el contexto de aplicación.

Como esta versión no tiene una pantalla de detalle independiente, el botón **Compartir** se ubica en cada tarjeta del listado de Productos, junto a las acciones CRUD. El selector nativo se verificó manualmente en Pixel 9a con el texto de Paracetamol 500 mg; la captura se conserva fuera del repositorio. Por indicación docente y por trabajar en Windows, no se compila, ejecuta ni presenta evidencia iOS; se conserva su estructura existente.

### Código específico de plataforma

| Capacidad | Código común | Android | iOS |
| --- | --- | --- | --- |
| Formato de soles | `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.kt` | `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.android.kt` (`NumberFormat`, locale peruano) | `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/Formato.ios.kt` (`NSNumberFormatter`, locale `es_PE`) |
| Compartir producto | `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/domain/platform/Compartidor.kt` | `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorAndroid.kt` (`Intent.ACTION_SEND`) | `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/CompartidorIos.kt` (`UIActivityViewController`, registrado en `PlatformModule.ios.kt`) |
| Módulo de plataforma | `shared/src/commonMain/kotlin/pe/edu/upeu/di/AppModule.kt` | `shared/src/androidMain/kotlin/pe/edu/upeu/di/PlatformModule.android.kt` (OkHttp, URL de emulador y `CompartidorAndroid`) | `shared/src/iosMain/kotlin/pe/edu/upeu/di/PlatformModule.ios.kt` (Darwin y URL local) |
| Información del dispositivo | `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.kt` | `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.android.kt` (`Build.VERSION.RELEASE`) | `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobil/platform/InfoDispositivo.ios.kt` (`UIDevice.currentDevice.systemVersion`) |

La opción **Acerca de** del menú lateral y del carril de navegación muestra sistema operativo y versión mediante `InfoDispositivo`. Solo se han ejecutado pruebas y compilación Android en este equipo Windows. La existencia del código iOS de `InfoDispositivo` no constituye evidencia de compilación ni ejecución iOS.

## Autenticación - Sesión 10

PharmaMobil se autentica contra `POST /api/v1/auth/login` de PharmaSoft y guarda el JWT recibido mediante la interfaz común `AlmacenSeguro`:

| Plataforma | Implementación | Mecanismo |
| --- | --- | --- |
| Android | `shared/src/androidMain/kotlin/pe/edu/upeu/data/auth/AlmacenSeguroAndroid.kt` | AES-256-GCM con llave no exportable en Android Keystore; en preferencias solo queda el texto cifrado. |
| iOS | `shared/src/iosMain/kotlin/pe/edu/upeu/data/auth/AlmacenSeguroIos.kt` | Keychain (`kSecClassGenericPassword`, `AfterFirstUnlockThisDeviceOnly`). Compilado para `iosArm64`/`iosSimulatorArm64` desde Windows; no ejecutado por falta de macOS/Xcode. |

- `crearHttpClient` instala el plugin **Auth** (`bearer`): `loadTokens` lee el almacén y `sendWithoutRequest` envía el token desde la primera petición salvo en `/auth/login`. Ninguna llamada escribe `Authorization` a mano.
- El plugin **Logging** usa `LogLevel.HEADERS` solo en compilaciones depurables y `sanitizeHeader` muestra `Authorization: ***`.
- Tras el login y al cerrar sesión se invoca `clearToken()` del proveedor bearer para no reutilizar un token en caché.
- Al arrancar, `sesionGuardada()` valida el token con `GET /api/v1/auth/me`; si es válido la sesión se restaura sin pedir credenciales.
- **401**: borra el token y vuelve al login con el aviso “La sesión expiró”. **403**: conserva la sesión y muestra “Acceso denegado para esta operación”.
- No existe endpoint de renovación: al expirar el token (3600 s) se repite el login.
- La interfaz oculta acciones según permisos: registrar/editar/eliminar productos requiere `CATALOGO_ESCRIBIR`; Clientes requiere `CLIENTES_LEER`; Pedidos requiere `VENTAS_LEER`.

### Usuarios de prueba (cargados por Flyway V3 en PharmaSoft)

| Usuario | Rol | Permisos |
| --- | --- | --- |
| `admin` | ADMIN | Los ocho permisos del sistema. |
| `vendedor` | VENDEDOR | `CATALOGO_LEER`, `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `VENTAS_LEER`, `VENTAS_REGISTRAR`. |
| `almacen` | ALMACENERO | `CATALOGO_LEER`, `CATALOGO_ESCRIBIR`. |

Las contraseñas de prueba son las publicadas en la Guía Práctica N.° 10 (sección 4.1); son exclusivas del entorno local de laboratorio.

## Persistencia offline-first

PharmaMobil guarda el catálogo en una base SQLite local mediante **SQLDelight 2.1.0** (`app.cash.sqldelight`, base `PharmaDb`, paquete `pe.edu.upeu.pharmamobil.db`). La interfaz siempre lee la base local; la red solo sincroniza.

### Esquema local

| Tabla | Archivo | Contenido |
| --- | --- | --- |
| `producto` | `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq` | Productos del servidor y registros locales. Tiene `pendiente` (1 = creado en el dispositivo y aún no confirmado) e `intentoEnvio` (1 = ya se inició un POST). |
| `categoria` | `Categoria.sq` | Categorías descargadas, para usarlas en el formulario sin red. |
| `estado_sync` | `EstadoSync.sq` | Fila única con el instante de la última sincronización correcta. |

Migraciones (`.sqm` en el mismo directorio):
- `1.sqm` crea `categoria`.
- `2.sqm` crea `estado_sync`.
- `3.sqm` añade `producto.intentoEnvio` con valor por defecto 1. Así, los pendientes creados antes de esta migración quedan **en revisión** y no se reenvían automáticamente.

### Funcionamiento

- **IDs negativos:** un producto registrado sin confirmación del servidor recibe un ID local negativo (-1, -2, …) con `pendiente=1` e `intentoEnvio=0`. No se puede editar ni dar de baja hasta tener un ID real.
- **Flow:** `ProductoLocal.observar()` expone la tabla como `Flow<List<Producto>>`. La pantalla de productos se actualiza con cada cambio local y muestra el aviso offline y la fecha de la última sincronización.
- **Sincronización:** la realiza `ProductoRepositorioOffline.sincronizar()`, serializada con un `Mutex` por ámbito, en tres pasos:
  1. Sube los pendientes.
  2. Descarga el catálogo paginado (100 por página, validando páginas y duplicados).
  3. Reemplaza solo los registros sincronizados (`pendiente=0`), conservando los pendientes locales.
- **Ámbito por cuenta:** `CoordinadorAmbitoProductos` abre una base distinta por cuenta validada (`pharma-cuenta-<32 hex>.db`, nombre aleatorio vinculado en el almacén seguro). Cada ámbito usa un cliente HTTP con el token fijo de esa sesión. La base histórica `pharma.db` queda en cuarentena y nunca se abre.
- **JWT:** se reutiliza la autenticación de la Sesión 10. Un 401 de la sesión vigente revoca el ámbito y vuelve al login.

### Protección ante respuestas POST perdidas

Que un GET responda no garantiza que el POST posterior llegue al servidor ni que su respuesta vuelva. Por eso cada producto pasa por estos estados:

| `pendiente` | `intentoEnvio` | Estado | Comportamiento |
| --- | --- | --- | --- |
| 1 | 0 | Pendiente sin enviar | Se envía en la siguiente sincronización. |
| 1 | 1 | Envío incierto: en revisión | **No se reenvía automáticamente** ni se reconcilia por nombre. |
| 0 | — | Confirmado y reconciliado | Reemplazado por el registro del servidor con ID positivo. |

Reglas:
1. Antes del lote se hace un GET de solo lectura. Si falla, no se marca nada y el pendiente sigue listo para enviarse.
2. La marca `intentoEnvio=1` se guarda en SQLite **antes** del POST.
3. Con una respuesta POST válida, el ID negativo se reemplaza por el ID del servidor (`pendiente=0`).
4. Si se pierde la respuesta, hay timeout, 409 o 5xx, el producto queda en revisión.
5. Un 401/403 en el POST (lo rechaza Spring Security antes del controlador, así que no hubo creación) devuelve el pendiente a la cola.

### Compilar y probar

```bash
./gradlew.bat :androidApp:assembleDebug
./gradlew.bat :shared:testAndroidHostTest
```

`testAndroidHostTest` compila y ejecuta en la JVM las pruebas de `shared/src/commonTest` junto con las de `androidHostTest`. Las 8 pruebas comunes de persistencia y sincronización están en `shared/src/commonTest/kotlin/pe/edu/upeu/data/local/PersistenciaSincronizacionCommonTest.kt`. Usan SQLite en memoria (`crearDriverPrueba()`, con versión `actual` para Android-host e iOS) y un servidor simulado `MockEngine`, sin acceder a Oracle. Para ejecutar solo esa clase:

```bash
./gradlew.bat :shared:testAndroidHostTest --tests "pe.edu.upeu.data.local.PersistenciaSincronizacionCommonTest"
```

Reporte: `shared/build/reports/tests/testAndroidHostTest/index.html`. En la última ejecución completa, `:shared:testAndroidHostTest` corrió 83 pruebas con 0 fallos.

### Escenarios offline

| # | Escenario | Procedimiento | Resultado esperado | Resultado verificado |
| --- | --- | --- | --- | --- |
| 1 | Registro sin conexión | Desactivar la red del emulador y registrar un producto. | Se guarda localmente con ID negativo y `pendiente=1`; aparece en la lista con el aviso offline. | **Verificado manualmente** en el emulador: "S11 SYNC FINAL 20261020" con ID -4 y `pendiente=1`. |
| 2 | Reconexión y sincronización | Reactivar la red y sincronizar. | El pendiente se sube una sola vez y se reconcilia con un ID positivo y `pendiente=0`. | **Verificado manualmente**: ID 43 con `pendiente=0` en SQLDelight; `GET` en Swagger con HTTP 200. |
| 3 | Migración 3 sobre una base existente | Abrir la app con una base de cuenta previa a la migración 3. | Se añade `intentoEnvio`; los pendientes previos quedan en revisión sin borrarse. | **Verificado manualmente** en el emulador: -1, -2 y -3 conservados en revisión. Además, prueba automatizada `CategoriaLocalTest`. |
| 4 | Respuesta POST perdida | El servidor crea el producto pero la respuesta no llega. | El pendiente queda en revisión y no se reenvía en la siguiente sincronización. | **Solo prueba automatizada** con servidor simulado (`respuestaPostPerdidaQuedaEnRevisionSinReenvio`). No ejecutado manualmente. |
| 5 | Reinicio de la app sin internet | Iniciar sesión, cerrar la app, desactivar la red y reabrirla. | Restaurar la sesión autenticada y mostrar el catálogo local. | **No ejecutado.** La restauración autenticada offline tras reinicio no está implementada. |

Las categorías persistidas en SQLDelight se verificaron manualmente en el emulador.

### Limitaciones

- **Pendientes históricos en revisión:** los productos -1, -2 y -3 quedan en revisión. No hay resolución automática ni una pantalla para resolverlos; tampoco se emparejan por nombre con los IDs 41/42 del servidor.
- **Restauración autenticada offline:** al reiniciar la app sin internet todavía no se restaura la sesión.
- **iOS:** no compilado ni probado en esta sesión. `:shared:iosSimulatorArm64Test` requiere macOS. Al compilar el target iOS en Windows se observaron errores en `shared/src/iosMain/kotlin/pe/edu/upeu/data/auth/AlmacenSeguroIos.kt` (llamadas a `conConsulta`, líneas 76, 88 y 99). La versión iOS de `crearDriverPrueba()` no se ha compilado.
