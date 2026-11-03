# Ficha de integración y evidencias S13

Autor: Richard Ismael Logacho Rivera.

## Firebase

- Contrato: `Telemetria` en dominio; adaptador GitLive Crashlytics en `data/firebase`.
- El módulo Koin utiliza `TelemetriaPendiente` mientras no exista configuración real. PharmaSoft REST sigue siendo la fuente de productos y autenticación.
- Para activar Android: crear proyecto Firebase propio, registrar la app **`com.edu.pe`**, descargar `google-services.json` y colocarlo en `androidApp/`. El archivo está excluido de Git. Verificar el recurso generado `google_app_id`, compilar e iniciar en un dispositivo con Google Play Services.
- No se han configurado credenciales Firebase ni se ha comprobado recepción en consola. La implementación iOS de Firebase y su archivo `GoogleService-Info.plist` quedan pendientes de macOS/Xcode.
- La telemetría descarta mensajes originales de excepciones y limita el contexto a `pantalla`, `operacion` y `codigo`. Nunca registrar JWT, contraseñas ni datos personales.

## Evidencias Android observadas

La bitácora y las capturas auténticas están en `validacion-android.md` y `evidencias/`.
Se comprobaron denegado, denegado permanentemente y concedido en Pixel 9a/Android 17.

## Evidencias funcionales aún pendientes

1. Con PharmaSoft disponible y una cuenta validada online, registrar un producto sin red y capturar el nuevo pendiente. La restauración offline actual es de solo lectura.
2. Recuperar conexión y pulsar **Sincronizar**. Capturar el resultado solo tras verificar el ID real en el backend. No reenviar pendientes «En revisión» sin cotejo manual.
3. Verificar login y logout reales cuando el backend esté disponible. No invalidar la sesión previa sin posibilidad de reingreso.
4. Para filas fallidas, usar pruebas MockEngine y SQLite en memoria. No provocar fallos reales que alteren Oracle.

Las capturas de esta sesión proceden de ADB. Firebase aún requiere configuración externa.
