import os,pathlib,json,hashlib,datetime,shutil
from PIL import Image,ImageOps,ImageDraw
F=pathlib.Path(os.environ['USERPROFILE'])/'AndroidStudioProjects/PharmaMobile'
O=F/'docs/s13-autonoma'; E=O/'evidencias'; (E/'recortes').mkdir(exist_ok=True)
steps=[
('1','Primera apertura sin sesión','01b-perfil-aislado-sin-sesion.png',(0,750,1080,1750),'Abrir el APK en el perfil nuevo 11, antes de autenticar.','Login visible, sin identidad activa.','Almacén sin token y cero bases locales en la instalación reader. Perfil 10 también se creó nuevo para writer.','01-instalacion-reader-vacia.json'),
('2','Login y almacenamiento seguro','02-login-writer.png',(0,900,1080,1780),'Autenticar s13_writer en el perfil 10.','Sesión online ALMACENERO.','Login HTTP 200; identidad S13 Escritura Sintetica. Token e identidad cifrados con IV; clave gestionada por Android Keystore según implementación. No se extrae ni muestra el JWT.','03-almacen-catalogo.json'),
('3','Catálogo tras primera sincronización','03-catalogo-sincronizado.png',(0,300,1080,2320),'Abrir Productos con red y categorías descargadas.','Dos productos sincronizados; cero pendientes.','SQLite contiene IDs 1 y 21; estado_sync=1793995198. GET real 200 en Logcat. Hora de sincronización 06/11/2026 14:59:58 Lima.','03-almacen-catalogo.json'),
('4','Registro online confirmado','04c-alta-online-http201.png',(0,300,1080,2320),'Registrar desde Android; repetir el alta con nombre Trazable para conservar el 201 antes de rotar Logcat.','POST 201 observado; IDs definitivos 41 y 61.','Primera alta: ID 41 (precio 6.75, stock 12). Repetición de trazabilidad: S13_Android_Online_Trazable, ID 61 (precio 7.25, stock 6). La captura corresponde a la repetición; 04-http201-android-repeticion.txt conserva el 201 real.','04c-online-trazable.json'),
('5','Registro sin conexión','05b-fila-pendiente.png',(0,300,1080,2320),'Activar modo avión y deshabilitar Wi-Fi/datos; registrar con sesión ya validada.','Un cambio pendiente, sin respuesta HTTP.','S13_Android_Offline_20261106 queda con ID -1, pendiente=1, intentoEnvio=0, intentos=1. Categoría previamente descargada. Ningún pendiente histórico se reenvía.','05-pendiente-confirmado.json'),
('6','Reconexión y conciliación','06-sincronizacion-recuperada.png',(0,300,1080,2320),'Desactivar modo avión, habilitar red y pulsar Sincronizar.','Pendientes: 1 → 0; ID 42 confirmado.','El ID -1 se sustituye por 42 y pendiente=0; catálogo de cuatro elementos. Oracle confirma IDs 41/42 y stocks 12/9. estado_sync=1793995397.','06-conciliacion-sqlite.json'),
('7','Validación local y remota','07-error-validacion-local.png',(0,1230,1080,1820),'Pulsar Registrar sin seleccionar categoría; realizar aparte una petición HTTP controlada con precio cero.','Campo Categoría requerido; petición externa HTTP 400.','Android muestra «Seleccione una categoría.» sin crear fila. El 400 remoto devuelve validationErrors.precio. Son dos recorridos distintos: no se atribuye el HTTP 400 al formulario local.','07-http400-real.json'),
('8','Operación sin privilegios','08-reader-estado-actual.png',(0,300,1080,2320),'Autenticar s13_reader en perfil 11 y consultar Productos; intentar POST controlado con su JWT.','Lectura permitida; escritura HTTP 403.','VENDEDOR tiene CATALOGO_LEER y no CATALOGO_ESCRIBIR. Android no muestra Nuevo/Editar/Baja. El rechazo 403 se obtiene realmente por HTTP externo; ocultar botones no se presenta como respuesta de red.','08-http403-real.json'),
('9','Vencimiento auténtico de JWT','09-jwt-vencido-revocado.png',(0,700,1080,1770),'Emitir JWT con JWT_EXPIRACION_MINUTOS=1; esperar expiración más tolerancia y sincronizar.','HTTP 401 por token expirado; Android vuelve al login.','Control: expiraEnSegundos=60; /auth/me devuelve «El token ha expirado». Android recibe 401, elimina token/identidad y revoca ámbito. Base y vinculación permanecen. Se restauró emisión normal a 3600 s.','09-jwt-expirado-http401.json'),
('10','Logout y estado local','10c-logout-final.png',(0,750,1080,1750),'Con duración normal, iniciar sesión y pulsar Cerrar sesión desde el menú.','Login visible; token e identidad ausentes.','Antes: token/identidad presentes. Después: solo cuenta_vinculada; una base y cinco productos conservados. Logout no borra datos ni habilita otra cuenta en esa instalación.','10d-logout-final.json')]
manifest=[]
for num,title,name,box,action,state,result,tech in steps:
    src=E/'flujo'/name; dst=E/'recortes'/('paso-'+num.zfill(2)+'.png')
    with Image.open(src) as im: im.crop(box).save(dst,optimize=True)
    manifest.append(dict(paso=int(num),titulo=title,original='evidencias/flujo/'+name,recorte='evidencias/recortes/'+dst.name,limites=list(box),sha256_original=hashlib.sha256(src.read_bytes()).hexdigest(),fecha_captura_host_utc=datetime.datetime.fromtimestamp(src.stat().st_mtime,datetime.timezone.utc).isoformat(),accion=action,estado=state,resultado=result,tecnica='evidencias/tecnica/'+tech))
(O/'recorrido.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
perms=[('Denegado','03-denegado.png'),('Denegado permanentemente','04-estado-tras-segunda-denegacion.png'),('Concedido','08-concedido.png')]
for i,(label,name) in enumerate(perms):
    with Image.open(E/'permisos'/name) as im: im.crop((0,300,1080,1270)).save(E/'recortes'/('permiso-'+str(i)+'.png'),optimize=True)
rows=['# Actividad Autónoma S13 - Bitácora real','', 'Richard Ismael Logacho Rivera. Ejecución: 6 de noviembre de 2026, Android 17/API 37, Pixel 9a, APK existente aprobado. Perfiles nuevos 10 (writer) y 11 (reader); propietario 0 conservado. No se borraron bases, volúmenes ni evidencias históricas.','', 'Los originales PNG proceden de adb screencap; los recortes solo eliminan márgenes y partes no pertinentes. recorrido.json identifica límites, fecha de archivo y SHA-256 del original. Ningún texto se agregó a las capturas. La hora de la barra Android usa formato de 12 horas; los JSON registran UTC y las descripciones normalizan a Lima.','']
for s in manifest:
    rows += [f"## {s['paso']}. {s['titulo']}",'',f"- Acción: {s['accion']}",f"- Estado: {s['estado']}",f"- Observación: {s['resultado']}",f"- Captura: [{s['original']}]({s['original']})",f"- Complemento: [{s['tecnica']}]({s['tecnica']})",'']
rows += ['## Evidencia complementaria y límites','', '- recorrido-http-android.txt contiene únicamente líneas REQUEST/RESPONSE/METHOD/FROM de Logcat, sin cabeceras de autorización ni cuerpos de login. Son llamadas reales de las instalaciones de ensayo, no un servidor simulado.', '- Los snapshots de SQLite y preferencias se leen con run-as de un APK debug. Solo se publican metadatos de presencia/longitud del cifrado, filas sintéticas e instantes; no se publican preferencias, claves, tokens ni archivos .db.', '- El HTTP 400 y el 403 se ejecutaron en peticiones externas autenticadas. La UI local muestra validación y restricción visual respectivamente; se distinguen expresamente.', '- El token expirado de control y el token Android fueron emitidos por el backend temporal de 60 s. El 401 de Android consta en Logcat y la revocación en 09-token-revocado.json. No se alteró el reloj ni se fabricó un JWT.', '- Pruebas: se reutilizan 81 Android host y 39 backend ya aprobadas, sin repetirlas durante esta sesión visual. No hubo cambios de código funcional móvil. El script backend de parada se corrigió por @argfile y se validó al detener los procesos propios.', '- Fecha Firebase histórica: evidencia incorporada en e5b0b2c el 03/11/2026 16:00:13 -05:00. El PNG muestra DebugView y s13_acerca_abierta; los textos anteriores que lo declaran pendiente están desactualizados. No se verificó Firebase de nuevo el 06/11.', '- No se compilaron ni ejecutaron targets iOS. La matriz y el procedimiento macOS están en plataformas.md.', '- Las capturas de intentos intermedios se conservan localmente fuera de la selección evaluada; solo se publican las capturas finales referenciadas.','']
(O/'bitacora.md').write_text('\n'.join(rows),encoding='utf-8')
platform='''# Producto B - Evidencias por plataforma

| Aspecto | Android: ejecutado hoy | iOS: inspección estática, sin compilación/ejecución |
|---|---|---|
| Token | Token/identidad cifrados AES-GCM; clave en Android Keystore según implementación. Metadatos presentes tras login y ausentes tras 401/logout. | AlmacenSeguroIos usa Keychain/SecItem, sin prueba real de lectura/escritura. |
| Persistencia | SQLDelight conserva cinco filas tras el logout final. Vinculación origen/usuario permanece; otra cuenta usa perfil separado. Instalación del APK con -r; no se ensayó desinstalación ni recuperación de backup. | Driver nativo y vinculación declarados; persistencia y reinstalación no comprobadas. Keychain tiene semántica propia, no asumir equivalencia con Android. |
| Permisos/ajustes | POST_NOTIFICATIONS observado en los tres estados y recuperado desde Ajustes. INTERNET no muestra diálogo. | GestorPermisosIosNoDisponible devuelve Denegado y abrirAjustes no realiza acción; requiere integración nativa. |
| Offline | Sesión previamente validada: alta -1 y conciliación 42; 1→0 pendientes. Reapertura offline es solo lectura por diseño. | Repositorio común disponible; transporte Darwin y driver nativo configurados; comportamiento no ejecutado. |
| Notificaciones/Firebase | Se verificó permiso, no entrega de notificaciones push. JSON Firebase auténtico ausente; DebugView histórico del 03/11 reutilizado. | firebaseConfigurado=false; GoogleService-Info.plist y permisos/notificaciones pendientes. |
| Interfaz/barras | Capturas reales de Compose, barra de estado y navegación gestual. No se modificó la imagen ni se simuló un iPhone. | ContentView.swift y MainViewController existentes; safe areas/barras y adaptaciones visuales no verificadas. |

## Estado iOS y procedimiento necesario

shared/build.gradle.kts declara iosArm64 e iosSimulatorArm64 y framework Shared. Existe iosApp/iosApp.xcodeproj, ContentView.swift, iOSApp.swift y Config.xcconfig. Windows no ofrece macOS/Xcode, SDK de Apple ni simulador iOS; no se instala ni se simula ese entorno.

En un Mac: recuperar la misma rama y commit, instalar/configurar Xcode y JDK compatible, abrir iosApp.xcodeproj, verificar framework Shared y configuración de firma, compilar para simulador y ejecutar iosSimulatorArm64Test. Resolver errores de interoperabilidad reales antes de declarar éxito. Configurar acceso desde el Mac al backend: localhost del simulador apunta al Mac, no al Windows anfitrión; comprobar red y política de transporte. Si se requiere Firebase, obtener plist auténtico e integrar notificaciones nativas. Repetir los diez escenarios y conservar capturas originales de esa plataforma.

No hay evidencia iOS inventada. Producto B se entrega con comparación y limitación explícita, conforme al alcance permitido en la misión.
'''
(O/'plataformas.md').write_text(platform,encoding='utf-8')
permissions='''# Producto C - Bitácora de permisos

Ejecución 06/11/2026, perfil writer 10, Android 17/API 37; targetSdk 36.

| Estado runtime POST_NOTIFICATIONS | Acción | Comportamiento y evidencia |
|---|---|---|
| Denegado | Pulsar No permitir en el diálogo nativo. | La app continúa y ofrece Solicitar permiso: permisos/03-denegado.png. |
| Denegado permanentemente | Solicitar otra vez y volver a denegar; comprobar estado. | La app muestra Denegado permanentemente y Abrir Ajustes: permisos/04-estado-tras-segunda-denegacion.png. |
| Concedido | Abrir Ajustes → Información de la app → Notificaciones → Todas las notificaciones; activar y volver a Comprobar estado. | La app muestra Concedido: permisos/08-concedido.png. También se conserva el diálogo y la ruta de ajustes en 02 y 05–07. |

INTERNET es permiso normal, sin diálogo runtime. El manifiesto fusionado añade ACCESS_NETWORK_STATE, WAKE_LOCK, RECEIVE de C2DM, BIND_GET_INSTALL_REFERRER_SERVICE, AD_ID, ACCESS_ADSERVICES_ATTRIBUTION, ACCESS_ADSERVICES_AD_ID y el permiso de firma DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. Son declaraciones de bibliotecas/infraestructura, no solicitudes hechas por GestorPermisos; no se atribuye a ellas ejercicio funcional ni tres estados de UI.

El sistema Android 17 también registra ACCESS_LOCAL_NETWORK concedido por compatibilidad para esta instalación target 36; no se observó ni implementó una solicitud propia de ese permiso. No se inventa un diálogo. No hay solicitudes de cámara, micrófono, ubicación ni almacenamiento en el código de la app.

POST_NOTIFICATIONS es el único runtime solicitado por la aplicación. Antes de Android 13 el adaptador lo trata como concedido; esta sesión sí usó Android 17. Las capturas históricas docs/s13/evidencias/04–11 (03/11/2026) se conservan, pero estas tres evidencias son nuevas. Conceder permiso no acredita recepción de un push; no se ensayó entrega de notificaciones.
'''
(O/'permisos.md').write_text(permissions,encoding='utf-8')
issues=[
('I01','Backend/JWT','main carecía de autenticación requerida.','docs/s13-integracion.md (backend)','Alta','Cerrado hoy','06/11/2026','Rama JWT aislada, login/me/403/401 reales; main intacto.'),
('I02','Oracle Lite','USERS no existe en la imagen Lite.','crear-esquema.ps1 y registro M4','Alta','Cerrado hoy','06/11/2026','Tablespace PHARMAS13, cuota limitada y esquema vacío; V1–V3 exitosas.'),
('I03','Windows/JDK','Tomcat/Gradle fallaban al conectar sockets Unix bajo MSIX.','iniciar-backend.ps1 y arranque real','Alta','Cerrado hoy','06/11/2026','Fallback TCP limitado al proceso; ningún cambio global ni funcional.'),
('I04','Operación backend','Script de parada no reconocía classpath @argfile.','detener-backend.ps1 y reinicios del ensayo','Media','Cerrado hoy','06/11/2026','Lee argfile y verifica pertenencia antes de detener Java. Dos paradas verificadas.'),
('I05','Android/UX','Mensaje de alta online dice que se sincronizará aunque ya hay ID definitivo.','04-online-sqlite.json + captura 04','Baja','Sesión 14','—','No afecta conciliación: ID 41 confirmado. Mejorar precisión del mensaje sin ocultar el resultado.'),
('I06','Android/seguridad','Otra cuenta no puede usar una instalación vinculada.','Perfiles 10/11 y snapshots logout','Baja','Descartado','—','Es protección de aislamiento, no defecto que deba eliminarse; usar perfiles separados conserva datos.'),
('I07','iOS','No hay compilación/ejecución; permisos nativos no implementados.','plataformas.md y iosMain','Alta','Sesión 14','—','Requiere macOS/Xcode y evaluación real; no se presenta como cerrado.'),
('I08','Firebase','JSON auténtico ausente y bitácora histórica contradice PNG DebugView.','PNG histórico + git e5b0b2c','Media','Sesión 14','—','Se aclara recepción del 03/11; integración actual depende del archivo del propietario.'),
('I09','Offline/UX','Tras reinicio offline, acceso es solo lectura; formulario no garantizado al expirar.','AuthRepositorioRest/SesionViewModel','Media','Sesión 14','—','El alta offline demostrada parte de sesión validada y app abierta; ampliar alcance requiere diseño posterior.')]
(O/'integracion.json').write_text(json.dumps(issues,ensure_ascii=False,indent=2),encoding='utf-8')
lines=['# Producto D - Ficha de integración','', '| ID | Componente | Problema observado | Evidencia | Severidad | Decisión | Cierre | Justificación |','|---|---|---|---|---|---|---|---|']
lines += ['|'+'|'.join(row)+'|' for row in issues]
(O/'ficha-integracion.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
protocol='''# Reproducción y control de evidencia

1. Reutilizar Oracle y backend de docs/s13-integracion.md, rama feature/s13-autonoma-integracion. No repetir migraciones, seeds o pruebas ya aprobadas sin motivo. Comprobar health y 10.0.2.2 desde Android.
2. Conservar el usuario propietario y crear perfiles Android nuevos. Instalar el APK existente con adb install -r y pm install-existing --user ID com.edu.pe. Esperar am switch-user -w ID, desbloquear el perfil y abrir MainActivity. No usar pm clear, uninstall, reset ni borrar bases.
3. Cargar credenciales exclusivamente desde %LOCALAPPDATA%/PharmaMobile/S13/secrets.json, nunca imprimirlas. Autenticar writer/reader en perfiles diferentes. No capturar campos de contraseña ni publicar tokens/archivos .db.
4. Seguir acciones de bitacora.md. Tomar PNG original con adb shell screencap -p /data/local/tmp/capture.png y adb pull. Capturar estado estable, no pantallas transitorias. La validación local y los HTTP externos 400/403 se documentan por separado.
5. Activar modo avión y deshabilitar Wi-Fi/datos solo para el alta offline; restaurarlos y confirmar -1→ID remoto. No tocar pendientes de otras instalaciones. Contrastar SQLite de debug con run-as y Oracle mediante consultas de solo lectura.
6. Expiración: detener solo el Java propio usando detener-backend.ps1, iniciar con JWT_EXPIRACION_MINUTOS=1, login nuevo y registrar metadatos iat/exp sin token. Esperar exp más tolerancia efectiva del decoder (en este ensayo, más de 60 s adicionales). Obtener 401 de control y provocar GET Android. Comprobar revocación. Reiniciar sin override y verificar expiraEnSegundos=3600. No editar reloj ni fabricar firmas.
7. Tras nuevo login normal, ejecutar logout desde menú. Comparar metadatos antes/después; conservar vinculación y filas. Las capturas originales no se retocan. Los recortes eliminan solo márgenes, con límites y hashes en recorrido.json.
8. Firebase: falta androidApp/google-services.json. Propietario: Firebase Console → proyecto PharmaMobile-UPeU Copiar → Configuración del proyecto → General → Tus apps → com.edu.pe → Descargar google-services.json; colocarlo en androidApp sin versionar. No bloquea REST. La captura DebugView del 03/11/2026 se referencia como histórica, no repetida hoy.
9. Publicar primero contenido/evidencias; fijar commits frontend/backend; generar PDF carta 8–14 páginas <20 MB con esos commits. Publicar PDF en un commit de entrega separado, sin referencia circular.

No se ejecutaron nuevas suites durante este recorrido: 81 pruebas frontend y 39 backend son resultados previos identificados. Se realizaron verificaciones reales de UI, HTTP, preferencias cifradas y SQLite/Oracle. No se compiló ni ejecutó iOS. No se alteró código funcional móvil. Resultado: A completo en Android; B comparación y limitación iOS honesta; C tres estados POST_NOTIFICATIONS; D incluye problemas abiertos sin cierre ficticio.
'''
(O/'reproduccion.md').write_text(protocol,encoding='utf-8')
# Contact sheet only for review, never submitted as evidence.
tiles=[]
for item in manifest:
    im=Image.open(O/item['recorte']).convert('RGB');im.thumbnail((200,310))
    tile=Image.new('RGB',(220,350),'white');tile.paste(im,((220-im.width)//2,28));ImageDraw.Draw(tile).text((8,6),str(item['paso'])+' '+item['titulo'][:22],fill='black');tiles.append(tile)
sheet=Image.new('RGB',(1100,700),'#dddddd')
for i,t in enumerate(tiles):sheet.paste(t,((i%5)*220,(i//5)*350))
sheet.save(pathlib.Path(os.environ['LOCALAPPDATA'])/'PharmaMobile/S13/contacto.png')
print('Materiales A/B/C/D y recortes generados con fuentes reales; no PDF todavía.')
