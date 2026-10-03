# Guía práctica S08 - CRUD REST de Producto

Fecha de verificación: 2026-10-03. Rama: `feature/crud-productos-logacho`.
Backend usado: [PharmaMobile-Backend](https://github.com/07Ismael02/PharmaMobile-Backend), no la copia temporal del profesor.

## Arquitectura implementada

- `ProductoApi` consume GET lista, GET por ID, POST, PUT y DELETE con Ktor. DELETE no deserializa el 204.
- `ProductoRepositorioRest` traduce la respuesta paginada y usa una categoría seleccionada del catálogo real. El dominio conserva `activo` y `categoriaId`.
- `EjecutarLlamada` centraliza 400 `Validacion`, 404 `NoEncontrado`, 409 `Conflicto`, 5xx `Servidor`, `SinConexion` y `TiempoAgotado`; relanza `CancellationException`.
- `ProductoViewModel` separa `Fase` y `Operacion`, muestra validaciones del backend bajo cada campo y recarga del servidor tras crear, editar o dar de baja. La pestaña Bajo stock sigue usando `stock <= 5`.
- Android usa `10.0.2.2:8080`; iOS está configurado para `localhost:8080` con motor Darwin, sin ejecución verificada desde Windows.

## Verificación HTTP real

Se obtuvo `categoriaId=1` mediante `GET /api/v1/categorias` (categoría activa `Analgésicos`). Antes de escribir había tres productos originales, IDs 1, 2 y 3. Se creó únicamente el producto de prueba `S08-Prueba-Codex-20261003-142215`, ID 21; se actualizó y después se dio de baja lógicamente. Los originales permanecieron presentes.

| Solicitud | Resultado HTTP observado | Resultado observado |
| --- | ---: | --- |
| GET `/api/health` | 200 | Servicio disponible. |
| GET `/api/v1/categorias` | 200 | Una categoría activa; ID 1. |
| GET `/api/v1/productos` | 200 | Tres productos originales antes de la prueba. |
| GET `/api/v1/productos/21` | 200 | Producto de prueba obtenido por ID. |
| POST `/api/v1/productos` válido | 201 | Creado ID 21 con categoría real. |
| PUT `/api/v1/productos/21` válido | 200 | Nombre, precio y stock de prueba actualizados. |
| POST con `nombre=AB` | 400 | `validationErrors.nombre` presente. |
| GET `/api/v1/productos/987654321` | 404 | ID inexistente. |
| PUT `/api/v1/productos/987654321` | 404 | ID inexistente. |
| POST con nombre duplicado | 409 | Regla de negocio del backend. |
| DELETE `/api/v1/productos/21` | 204 | Baja lógica exitosa, sin cuerpo. |
| Segundo DELETE `/api/v1/productos/21` | 409 | El backend rechaza dar de baja un producto ya inactivo; no devuelve 404. |
| GET `/api/v1/productos` posterior | 200 | IDs 1-3 presentes; ID 21 con `estado=false`. |

## Pruebas y compilación

En Windows, `:shared:testAndroidHostTest` terminó `BUILD SUCCESSFUL`: 20 pruebas, 0 fallos y 0 errores. Las pruebas nuevas cubren verbos y paginación, DELETE 204 sin body, 400/404/409/500, red, timeout, cancelación y transiciones del ViewModel en creación, validación y eliminación. `:androidApp:assembleDebug` terminó `BUILD SUCCESSFUL` tras conectar la UI.

En el cierre del 2026-10-03 se ejecutó de nuevo `gradlew.bat :shared:testAndroidHostTest :androidApp:assembleDebug --no-daemon`: **BUILD SUCCESSFUL** en Windows. Los 20 resultados XML registrados permanecen con 0 fallos y 0 errores; Gradle marcó la tarea de pruebas `UP-TO-DATE`. La tarea `assembleDebug` finalizó correctamente. La prueba `iosSimulatorArm64Test` está deshabilitada en Windows porque requiere macOS; no se ejecutó.

## Evidencias Android reales de la guía

El 2026-10-03 se ejecutó la aplicación `com.edu.pe` en el emulador Pixel 9a (Android 17, API 37.1). Estas capturas fueron obtenidas por el integrante y se conservan aquí sin alterar. Corresponden a una prueba manual posterior e independiente de la verificación HTTP del producto de prueba ID 21 descrita arriba. En Android se usó el producto **Producto S08 Ismael**, ID real **22**, categoría **Analgésicos**, `categoriaId=1`.

| Requisito | Evidencia observable | Capturas |
| --- | --- | --- |
| Listado | El backend alimenta la pestaña Activos con Paracetamol 500 mg, Ibuprofeno 400 mg y Naproxeno 250 mg. | [Paracetamol](evidencias-s08/listado-paracetamol.png), [Ibuprofeno](evidencias-s08/listado-ibuprofeno.png), [Naproxeno](evidencias-s08/listado-naproxeno.png) |
| Creación | Se registró Producto S08 Ismael con precio S/ 4.50, stock 7 y categoría Analgésicos. Ktor registró POST `/api/v1/productos` **201** y GET de refresco **200**. | [Android](evidencias-s08/creacion-android.png), [POST 201](evidencias-s08/creacion-post-201-logcat.png), [GET 200](evidencias-s08/creacion-get-200-logcat.png) |
| Actualización | El producto ID 22 cambió de S/ 4.50 a S/ 5.00. La UI mostró el mensaje de éxito y la tarjeta con el precio nuevo. Ktor registró PUT `/api/v1/productos/22` **200** y GET de refresco **200**. | [Android](evidencias-s08/actualizacion-android.png), [PUT 200](evidencias-s08/actualizacion-put-200-logcat.png) |
| Eliminación lógica | Se dio de baja el ID 22. La UI confirmó la operación y lo mostró en Inactivos con estado Inactivo. Ktor registró DELETE `/api/v1/productos/22` **204** y GET de refresco **200**. | [Android](evidencias-s08/baja-logica-android.png), [DELETE 204](evidencias-s08/baja-logica-delete-204-logcat.png) |
| Validación del servidor | Con nombre `AB`, precio 4.50, stock 7 y categoría Analgésicos, el POST llegó al backend y recibió **400**. El mensaje «El nombre debe tener entre 3 y 150 caracteres» se mostró debajo del campo Nombre. | [Android](evidencias-s08/validacion-400-android.png), [POST 400](evidencias-s08/validacion-post-400-logcat.png) |

El filtro de Logcat fue `package:com.edu.pe tag:PharmaMobilKtor`; las peticiones usaron `http://10.0.2.2:8080/api/v1/productos`. No se infieren operaciones distintas de las mostradas en las capturas.

## Contraste con la lista de cotejo de la guía

| N.º | Criterio | Estado |
| ---: | --- | --- |
| 1 | `ProductoApi` implementa listar, obtener, crear, actualizar y eliminar con los verbos correctos. | Cumple: código y pruebas automatizadas. |
| 2 | Listado con `PaginaResponseDto`; DELETE no lee el cuerpo del 204. | Cumple: código y prueba DELETE 204. |
| 3 | Repositorio de dominio con cinco operaciones e implementación REST en Koin. | Cumple: `ProductoRepository` y `dataModule`. |
| 4 | Traducción central de excepciones Ktor a `ErrorApi`. | Cumple: `EjecutarLlamada`. |
| 5 | Presentation no importa `io.ktor`. | Cumple: inspección de imports. |
| 6 | Estado de fase separado del estado de operación. | Cumple: `Fase` y `Operacion`. |
| 7 | Tratamiento exhaustivo de las cuatro fases. | Cumple: `Cargando`, `SinProductos`, `ConProductos` y `Error`. |
| 8 | Errores del servidor bajo el campo correspondiente. | Cumple en Android: nombre `AB`, POST 400 y mensaje bajo Nombre. |
| 9 | Refresco tras crear, actualizar y eliminar. | Cumple en Android: GET 200 tras cada mutación. |
| 10 | Cuatro operaciones verificadas en Android **e iOS** con capturas. | Parcial: Android demostrado; iOS no ejecutado ni capturado en Windows. |

La rama contiene al menos tres commits propios distribuidos de la sesión 08. Queda pendiente únicamente la verificación y captura iOS desde macOS/Xcode para cumplir literalmente el entregable multiplataforma. No se afirma que iOS haya compilado o funcionado.
