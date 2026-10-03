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

## Evidencia gráfica pendiente

No se fabricaron capturas. El emulador Android no estaba conectado al realizar esta verificación (`adb devices -l` sin dispositivos). Falta incorporar:

1. [INSERTAR CAPTURA ANDROID - listado de Productos con categoría]
2. [INSERTAR CAPTURA ANDROID - creación 201 y producto visible]
3. [INSERTAR CAPTURA ANDROID - actualización 200]
4. [INSERTAR CAPTURA ANDROID - baja lógica 204: Activos e Inactivos]
5. [INSERTAR CAPTURA ANDROID - nombre de dos caracteres: error 400 bajo Nombre]
6. [INSERTAR CAPTURA LOGCAT - Ktor petición/respuesta exitosa]
7. [INSERTAR CAPTURA LOGCAT - Ktor petición/respuesta fallida]
8. [PENDIENTE iOS - requiere macOS y Xcode; no verificado en Windows]
