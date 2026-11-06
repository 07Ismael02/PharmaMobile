# Producto C - Bitácora de permisos

Ejecución 06/11/2026, perfil writer 10, Android 17/API 37; targetSdk 36.

| Estado runtime POST_NOTIFICATIONS | Acción | Comportamiento y evidencia |
|---|---|---|
| Denegado | Pulsar No permitir en el diálogo nativo. | La app continúa y ofrece Solicitar permiso: permisos/03-denegado.png. |
| Denegado permanentemente | Solicitar otra vez y volver a denegar; comprobar estado. | La app muestra Denegado permanentemente y Abrir Ajustes: permisos/04-estado-tras-segunda-denegacion.png. |
| Concedido | Abrir Ajustes → Información de la app → Notificaciones → Todas las notificaciones; activar y volver a Comprobar estado. | La app muestra Concedido: permisos/08-concedido.png. También se conserva el diálogo y la ruta de ajustes en 02 y 05–07. |

INTERNET es permiso normal, sin diálogo runtime. El manifiesto fusionado añade ACCESS_NETWORK_STATE, WAKE_LOCK, RECEIVE de C2DM, BIND_GET_INSTALL_REFERRER_SERVICE, AD_ID, ACCESS_ADSERVICES_ATTRIBUTION, ACCESS_ADSERVICES_AD_ID y el permiso de firma DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. Son declaraciones de bibliotecas/infraestructura, no solicitudes hechas por GestorPermisos; no se atribuye a ellas ejercicio funcional ni tres estados de UI.

El sistema Android 17 también registra ACCESS_LOCAL_NETWORK concedido por compatibilidad para esta instalación target 36; no se observó ni implementó una solicitud propia de ese permiso. No se inventa un diálogo. No hay solicitudes de cámara, micrófono, ubicación ni almacenamiento en el código de la app.

POST_NOTIFICATIONS es el único runtime solicitado por la aplicación. Antes de Android 13 el adaptador lo trata como concedido; esta sesión sí usó Android 17. Las capturas históricas docs/s13/evidencias/04–11 (03/11/2026) se conservan, pero estas tres evidencias son nuevas. Conceder permiso no acredita recepción de un push; no se ensayó entrega de notificaciones.
