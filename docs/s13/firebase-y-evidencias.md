# Ficha de integración y evidencias S13

Autor: Richard Ismael Logacho Rivera.

## Firebase

- Contrato: `Telemetria` en dominio; adaptador GitLive Crashlytics en `data/firebase`.
- El módulo Koin utiliza `TelemetriaPendiente` mientras no exista configuración real. PharmaSoft REST sigue siendo la fuente de productos y autenticación.
- Para activar Android: crear proyecto Firebase propio, registrar la app **`com.edu.pe`**, descargar `google-services.json` y colocarlo en `androidApp/`. El archivo está excluido de Git. Verificar el recurso generado `google_app_id`, compilar e iniciar en un dispositivo con Google Play Services.
- No se han configurado credenciales Firebase ni se ha comprobado recepción en consola. La implementación iOS de Firebase y su archivo `GoogleService-Info.plist` quedan pendientes de macOS/Xcode.
- La telemetría descarta mensajes originales de excepciones y limita el contexto a `pantalla`, `operacion` y `codigo`. Nunca registrar JWT, contraseñas ni datos personales.

## Evidencias manuales pendientes

1. En Pixel 9a, abrir **Acerca de** y capturar el estado inicial del permiso de notificaciones.
2. Pulsar **Solicitar permiso**; capturar el diálogo Android y el estado concedido o denegado real. Una denegación permanente requiere repetirla según la versión del sistema; si no se reproduce, marcarla pendiente.
3. Con una cuenta validada y categoría cacheada, activar modo avión, registrar un producto y capturar contador de pendientes y antigüedad de datos.
4. Recuperar conexión y pulsar **Sincronizar**. Capturar el resultado solo tras verificar el ID real en el backend. No reenviar pendientes «En revisión» sin cotejo manual.
5. Para filas fallidas, usar pruebas MockEngine y SQLite en memoria. No provocar fallos reales que alteren Oracle.

No se han creado ni modificado capturas. La verificación visual y Firebase requieren intervención del estudiante.
