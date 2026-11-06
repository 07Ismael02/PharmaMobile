# Actividad Autónoma S13 - Bitácora real

Richard Ismael Logacho Rivera. Ejecución: 6 de noviembre de 2026, Android 17/API 37, Pixel 9a, APK existente aprobado. Perfiles nuevos 10 (writer) y 11 (reader); propietario 0 conservado. No se borraron bases, volúmenes ni evidencias históricas.

Los originales PNG proceden de adb screencap; los recortes solo eliminan márgenes y partes no pertinentes. recorrido.json identifica límites, fecha de archivo y SHA-256 del original. Ningún texto se agregó a las capturas. La hora de la barra Android usa formato de 12 horas; los JSON registran UTC y las descripciones normalizan a Lima.

## 1. Primera apertura sin sesión

- Acción: Abrir el APK en el perfil nuevo 11, antes de autenticar.
- Estado: Login visible, sin identidad activa.
- Observación: Almacén sin token y cero bases locales en la instalación reader. Perfil 10 también se creó nuevo para writer.
- Captura: [evidencias/flujo/01b-perfil-aislado-sin-sesion.png](evidencias/flujo/01b-perfil-aislado-sin-sesion.png)
- Complemento: [evidencias/tecnica/01-instalacion-reader-vacia.json](evidencias/tecnica/01-instalacion-reader-vacia.json)

## 2. Login y almacenamiento seguro

- Acción: Autenticar s13_writer en el perfil 10.
- Estado: Sesión online ALMACENERO.
- Observación: Login HTTP 200; identidad S13 Escritura Sintetica. Token e identidad cifrados con IV; clave gestionada por Android Keystore según implementación. No se extrae ni muestra el JWT.
- Captura: [evidencias/flujo/02-login-writer.png](evidencias/flujo/02-login-writer.png)
- Complemento: [evidencias/tecnica/03-almacen-catalogo.json](evidencias/tecnica/03-almacen-catalogo.json)

## 3. Catálogo tras primera sincronización

- Acción: Abrir Productos con red y categorías descargadas.
- Estado: Dos productos sincronizados; cero pendientes.
- Observación: SQLite contiene IDs 1 y 21; estado_sync=1793995198. GET real 200 en Logcat. Hora de sincronización 06/11/2026 14:59:58 Lima.
- Captura: [evidencias/flujo/03-catalogo-sincronizado.png](evidencias/flujo/03-catalogo-sincronizado.png)
- Complemento: [evidencias/tecnica/03-almacen-catalogo.json](evidencias/tecnica/03-almacen-catalogo.json)

## 4. Registro online confirmado

- Acción: Registrar desde Android; repetir el alta con nombre Trazable para conservar el 201 antes de rotar Logcat.
- Estado: POST 201 observado; IDs definitivos 41 y 61.
- Observación: Primera alta: ID 41 (precio 6.75, stock 12). Repetición de trazabilidad: S13_Android_Online_Trazable, ID 61 (precio 7.25, stock 6). La captura corresponde a la repetición; 04-http201-android-repeticion.txt conserva el 201 real.
- Captura: [evidencias/flujo/04c-alta-online-http201.png](evidencias/flujo/04c-alta-online-http201.png)
- Complemento: [evidencias/tecnica/04c-online-trazable.json](evidencias/tecnica/04c-online-trazable.json)

## 5. Registro sin conexión

- Acción: Activar modo avión y deshabilitar Wi-Fi/datos; registrar con sesión ya validada.
- Estado: Un cambio pendiente, sin respuesta HTTP.
- Observación: S13_Android_Offline_20261106 queda con ID -1, pendiente=1, intentoEnvio=0, intentos=1. Categoría previamente descargada. Ningún pendiente histórico se reenvía.
- Captura: [evidencias/flujo/05b-fila-pendiente.png](evidencias/flujo/05b-fila-pendiente.png)
- Complemento: [evidencias/tecnica/05-pendiente-confirmado.json](evidencias/tecnica/05-pendiente-confirmado.json)

## 6. Reconexión y conciliación

- Acción: Desactivar modo avión, habilitar red y pulsar Sincronizar.
- Estado: Pendientes: 1 → 0; ID 42 confirmado.
- Observación: El ID -1 se sustituye por 42 y pendiente=0; catálogo de cuatro elementos. Oracle confirma IDs 41/42 y stocks 12/9. estado_sync=1793995397.
- Captura: [evidencias/flujo/06-sincronizacion-recuperada.png](evidencias/flujo/06-sincronizacion-recuperada.png)
- Complemento: [evidencias/tecnica/06-conciliacion-sqlite.json](evidencias/tecnica/06-conciliacion-sqlite.json)

## 7. Validación local y remota

- Acción: Pulsar Registrar sin seleccionar categoría; realizar aparte una petición HTTP controlada con precio cero.
- Estado: Campo Categoría requerido; petición externa HTTP 400.
- Observación: Android muestra «Seleccione una categoría.» sin crear fila. El 400 remoto devuelve validationErrors.precio. Son dos recorridos distintos: no se atribuye el HTTP 400 al formulario local.
- Captura: [evidencias/flujo/07-error-validacion-local.png](evidencias/flujo/07-error-validacion-local.png)
- Complemento: [evidencias/tecnica/07-http400-real.json](evidencias/tecnica/07-http400-real.json)

## 8. Operación sin privilegios

- Acción: Autenticar s13_reader en perfil 11 y consultar Productos; intentar POST controlado con su JWT.
- Estado: Lectura permitida; escritura HTTP 403.
- Observación: VENDEDOR tiene CATALOGO_LEER y no CATALOGO_ESCRIBIR. Android no muestra Nuevo/Editar/Baja. El rechazo 403 se obtiene realmente por HTTP externo; ocultar botones no se presenta como respuesta de red.
- Captura: [evidencias/flujo/08-reader-estado-actual.png](evidencias/flujo/08-reader-estado-actual.png)
- Complemento: [evidencias/tecnica/08-http403-real.json](evidencias/tecnica/08-http403-real.json)

## 9. Vencimiento auténtico de JWT

- Acción: Emitir JWT con JWT_EXPIRACION_MINUTOS=1; esperar expiración más tolerancia y sincronizar.
- Estado: HTTP 401 por token expirado; Android vuelve al login.
- Observación: Control: expiraEnSegundos=60; /auth/me devuelve «El token ha expirado». Android recibe 401, elimina token/identidad y revoca ámbito. Base y vinculación permanecen. Se restauró emisión normal a 3600 s.
- Captura: [evidencias/flujo/09-jwt-vencido-revocado.png](evidencias/flujo/09-jwt-vencido-revocado.png)
- Complemento: [evidencias/tecnica/09-jwt-expirado-http401.json](evidencias/tecnica/09-jwt-expirado-http401.json)

## 10. Logout y estado local

- Acción: Con duración normal, iniciar sesión y pulsar Cerrar sesión desde el menú.
- Estado: Login visible; token e identidad ausentes.
- Observación: Antes: token/identidad presentes. Después: solo cuenta_vinculada; una base y cinco productos conservados. Logout no borra datos ni habilita otra cuenta en esa instalación.
- Captura: [evidencias/flujo/10c-logout-final.png](evidencias/flujo/10c-logout-final.png)
- Complemento: [evidencias/tecnica/10d-logout-final.json](evidencias/tecnica/10d-logout-final.json)

## Evidencia complementaria y límites

- recorrido-http-android.txt contiene únicamente líneas REQUEST/RESPONSE/METHOD/FROM de Logcat, sin cabeceras de autorización ni cuerpos de login. Son llamadas reales de las instalaciones de ensayo, no un servidor simulado.
- Los snapshots de SQLite y preferencias se leen con run-as de un APK debug. Solo se publican metadatos de presencia/longitud del cifrado, filas sintéticas e instantes; no se publican preferencias, claves, tokens ni archivos .db.
- El HTTP 400 y el 403 se ejecutaron en peticiones externas autenticadas. La UI local muestra validación y restricción visual respectivamente; se distinguen expresamente.
- El token expirado de control y el token Android fueron emitidos por el backend temporal de 60 s. El 401 de Android consta en Logcat y la revocación en 09-token-revocado.json. No se alteró el reloj ni se fabricó un JWT.
- Pruebas: se reutilizan 81 Android host y 39 backend ya aprobadas, sin repetirlas durante esta sesión visual. No hubo cambios de código funcional móvil. El script backend de parada se corrigió por @argfile y se validó al detener los procesos propios.
- Fecha Firebase histórica: evidencia incorporada en e5b0b2c el 03/11/2026 16:00:13 -05:00. El PNG muestra DebugView y s13_acerca_abierta; los textos anteriores que lo declaran pendiente están desactualizados. No se verificó Firebase de nuevo el 06/11.
- No se compilaron ni ejecutaron targets iOS. La matriz y el procedimiento macOS están en plataformas.md.
- Las capturas de intentos intermedios se conservan localmente fuera de la selección evaluada; solo se publican las capturas finales referenciadas.
