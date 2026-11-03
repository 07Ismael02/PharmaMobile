# S13 - bitácora de validación Android

Autor: Richard Ismael Logacho Rivera. Fecha de ejecución: 3 de noviembre de 2026.

## Entorno y alcance

- Rama `feature/integrador-13`, HEAD inicial `a3f6dea`; `Estudio_U2/` permaneció intacto.
- Emulador conectado `emulator-5554`, Android 17 (`sdk_gphone16k_x86_64`). APK `androidApp-debug.apk` instalado con `adb install -r`; no se borraron datos.
- Al comenzar, el emulador ya estaba en modo avión y la app conservaba una identidad validada previamente para acceso local de solo lectura.
- `http://localhost:8080/api/health` no respondió. No se inició ni modificó PharmaMobile-Backend.

## Recorrido observado

| Escenario | Resultado real | Evidencia |
| --- | --- | --- |
| Apertura con sesión previa y sin red | Inicio mostró identidad previa y aviso de catálogo local de solo lectura. No se realizó login nuevo. | `evidencias/01-inicio-real.png` |
| Navegación | Menú permitió abrir Productos y Acerca de. | `evidencias/02-menu-real.png` |
| Catálogo offline | Mostró datos locales, «Actualizado hace 11 minutos», 0 cambios por enviar y 3 que requieren revisión. Las filas antiguas permanecieron marcadas «En revisión». | `evidencias/03-productos-offline-real.png` |
| Sincronización online y login | No verificados: backend no disponible y no se introdujeron credenciales. | Sin captura. |
| Pendiente nuevo | No reproducido: restauración offline vigente es de solo lectura. No se alteraron pendientes históricos. | Sin captura. |
| Logout | No ejecutado: habría retirado la sesión previa sin una vía de login disponible para restaurarla. | Sin captura. |

## Bitácora de permisos - POST_NOTIFICATIONS

| Estado | Acción y observación | Evidencia |
| --- | --- | --- |
| Denegado inicial | «Acerca de» mostró Denegado antes de solicitar. | `evidencias/04-permiso-inicial-real.png` |
| Solicitud del sistema | Se abrió el diálogo nativo de Android con Allow / Don't allow. | `evidencias/05-permiso-dialogo-real.png` |
| Denegado | Primera pulsación en Don't allow; la app continuó y ofreció solicitar de nuevo. | `evidencias/06-permiso-denegado-real.png` |
| Denegado permanentemente | En la segunda solicitud, Android ofreció `permission_deny_and_dont_ask_again_button`; al denegar, la app mostró «Denegado permanentemente» y «Abrir Ajustes». | `evidencias/07-permiso-permanente-real.png` |
| Concedido | Desde el botón de la app se abrió Ajustes, se activaron las notificaciones y, de vuelta en la app, «Comprobar estado» mostró Concedido. | `evidencias/08-ajustes-real.png` a `11-permiso-concedido-real.png` |

## Pruebas y Firebase

- Resultado anterior de `:shared:testAndroidHostTest`: 81 pruebas, 0 fallos, 0 errores. `:androidApp:assembleDebug`: BUILD SUCCESSFUL. No se repitieron suites en esta validación visual.
- El 3 de noviembre se incorporó `androidApp/google-services.json` (ignorado por Git) para `com.edu.pe`. El APK con Google Services, Crashlytics y Analytics se instaló en Pixel_9a sin borrar datos. Abrir «Acerca de» registró el evento `s13_acerca_abierta`; Logcat FA-SVC mostró `Logging event`, `Gms url request successful` y `Successful upload` con HTTP 204. La captura auténtica de la app es `evidencias/12-firebase-acerca-real.png`. La recepción visual en Firebase Console DebugView continúa sin verificar y no existe captura de esa consola.
- Con la nueva integración se ejecutaron nuevamente `:shared:testAndroidHostTest` (81 pruebas, 0 fallos, 0 errores) y `:androidApp:assembleDebug` (BUILD SUCCESSFUL). El primer APK cerraba al iniciar por falta del ID de Crashlytics; se agregó el plugin oficial y el APK corregido abrió sin ese fallo.
- iOS no se compiló ni ejecutó en Windows.
