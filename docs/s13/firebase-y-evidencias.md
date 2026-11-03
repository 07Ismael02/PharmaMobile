# Ficha de integración y evidencias S13

Autor: Richard Ismael Logacho Rivera.

## Firebase

- Contrato: `Telemetria` en dominio; adaptador GitLive Analytics/Crashlytics en `data/firebase`.
- El proyecto Firebase «PharmaMobile-UPeU Copiar» tiene la app Android **`com.edu.pe`**. `androidApp/google-services.json` existe localmente, coincide con el paquete y está excluido de Git. Google Services generó recursos y el plugin Crashlytics generó el ID de compilación necesario. El módulo Koin activa `TelemetriaFirebase`; PharmaSoft REST sigue siendo la fuente de productos y autenticación.
- Abrir «Acerca de» registra `s13_acerca_abierta` sin parámetros personales. Se observó en Logcat `Logging event: origin=app,name=s13_acerca_abierta` y subida correcta de Google Analytics HTTP 204. **No se observó todavía en Firebase Console DebugView** ni hay captura de esa pantalla; el estudiante debe abrir Analytics → DebugView y adjuntar su captura auténtica si aparece.
- La implementación iOS de Firebase y su archivo `GoogleService-Info.plist` quedan pendientes de macOS/Xcode.
- La telemetría descarta mensajes originales de excepciones y limita el contexto a `pantalla`, `operacion` y `codigo`. Nunca registrar JWT, contraseñas ni datos personales.

## Evidencias Android observadas

La bitácora y las capturas auténticas están en `validacion-android.md` y `evidencias/`.
Se comprobaron denegado, denegado permanentemente y concedido en Pixel 9a/Android 17.

## Evidencias funcionales aún pendientes

1. Con PharmaSoft disponible y una cuenta validada online, registrar un producto sin red y capturar el nuevo pendiente. La restauración offline actual es de solo lectura.
2. Recuperar conexión y pulsar **Sincronizar**. Capturar el resultado solo tras verificar el ID real en el backend. No reenviar pendientes «En revisión» sin cotejo manual.
3. Verificar login y logout reales cuando el backend esté disponible. No invalidar la sesión previa sin posibilidad de reingreso.
4. Para filas fallidas, usar pruebas MockEngine y SQLite en memoria. No provocar fallos reales que alteren Oracle.

Las capturas de esta sesión proceden de ADB. La captura Firebase de la app es `evidencias/12-firebase-acerca-real.png`; la de DebugView sigue pendiente.
