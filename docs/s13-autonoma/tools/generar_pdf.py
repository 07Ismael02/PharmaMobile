import os,pathlib,json,sys,datetime,hashlib
from xml.sax.saxutils import escape
from reportlab.pdfgen import canvas
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph,Table,TableStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from PIL import Image
F=pathlib.Path(os.environ['USERPROFILE'])/'AndroidStudioProjects/PharmaMobile'
O=F/'docs/s13-autonoma'; E=O/'evidencias'
front_commit=sys.argv[1]; back_commit=sys.argv[2]
output=O/'Logacho_Ismael_Sesion13_Evidencias.pdf'
for name,file in [('Arial','arial.ttf'),('Arial-Bold','arialbd.ttf')]:pdfmetrics.registerFont(TTFont(name,'C:/Windows/Fonts/'+file))
W,H=letter; teal=colors.HexColor('#006D62'); ink=colors.HexColor('#17302D'); gray=colors.HexColor('#526560'); light=colors.HexColor('#EAF4F1')
c=canvas.Canvas(str(output),pagesize=letter,pageCompression=1)
c.setTitle('Sesion 13 - Evidencias del flujo completo por plataforma')
c.setAuthor('Richard Ismael Logacho Rivera');c.setSubject('UPeU - Desarrollo de Aplicaciones Moviles - Actividad Autonoma 13')
page=0
def p(text,x,y,width,size=10.3,color=ink,bold=False,leading=None):
    text=text.replace('\u2013','-').replace('\u2014','-').replace('\u2011','-')
    style=ParagraphStyle('body',fontName='Arial-Bold' if bold else 'Arial',fontSize=size,leading=leading or size*1.32,textColor=color,spaceAfter=0)
    ob=Paragraph(text,style); _,h=ob.wrap(width,1000)
    if y-h<43: raise RuntimeError(f'Overflow page {page}: {text[:60]} bottom={y-h}')
    ob.drawOn(c,x,y-h);return y-h-9
def base(title,kicker='S13 / ACTIVIDAD AUTONOMA'):
    global page
    page+=1;c.setFillColor(teal);c.rect(0,H-12,W,12,fill=1,stroke=0)
    p(kicker,40,H-33,530,8.5,teal,True)
    p(title,40,H-52,532,20,ink,True)
    c.setStrokeColor(colors.HexColor('#C8DBD5'));c.line(40,40,W-40,40)
    c.setFont('Arial',8);c.setFillColor(gray);c.drawString(40,25,'Richard Ismael Logacho Rivera | UPeU | 06/11/2026')
    c.drawRightString(W-40,25,f'{page} / 13')
    return H-100
def finish():c.showPage()
def figure(path,x,y,maxw,maxh):
    with Image.open(path) as im:iw,ih=im.size
    scale=min(maxw/iw,maxh/ih);w=iw*scale;h=ih*scale
    c.drawImage(str(path),x+(maxw-w)/2,y-h,width=w,height=h,mask='auto')
    return y-h-8
def table(headers,rows,widths,x,y,size=8.5):
    style=ParagraphStyle('cell',fontName='Arial',fontSize=size,leading=size*1.25,textColor=ink)
    data=[[Paragraph(escape(str(v).replace('\u2013','-').replace('\u2014','-').replace('\u2011','-')),style) for v in row] for row in [headers]+rows]
    t=Table(data,colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),light),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),1,teal),('LINEBELOW',(0,1),(-1,-1),.25,colors.HexColor('#D9E4DF')),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6)]))
    _,h=t.wrap(sum(widths),1000)
    if y-h<44:raise RuntimeError(f'Table overflow page {page}: {h}')
    t.drawOn(c,x,y-h);return y-h-12
steps=json.loads((O/'recorrido.json').read_text(encoding='utf-8'))
issues=json.loads((O/'integracion.json').read_text(encoding='utf-8'))

# 1: formal cover
y=base('Documentar el flujo completo','UNIVERSIDAD PERUANA UNION / INGENIERIA DE SISTEMAS')
y=p('y las evidencias por plataforma',40,y+8,532,23,teal,True)
y=p('Desarrollo de Aplicaciones Moviles<br/>Actividad Autonoma N.° 13',40,y-18,500,15)
c.setFillColor(light);c.roundRect(40,365,532,145,12,fill=1,stroke=0)
p('PharmaMobile + PharmaSoft',60,488,492,24,teal,True)
p('Integración real Android - REST - JWT - Oracle<br/>Persistencia local SQLDelight y sincronización offline-first',60,443,492,12)
y=p('Richard Ismael Logacho Rivera',40,320,532,17,ink,True)
y=p('6 de noviembre de 2026<br/>Android 17 / API 37 / Pixel 9a<br/>Windows 11 - Oracle Free 26ai - Spring Boot',40,y-10,532,11)
p('Código y grupo: no consignados por no estar verificados.<br/>No se incluyen credenciales ni JWT completos.',40,132,532,9,gray)
finish()
# 2: index and methods
y=base('Índice, alcance y trazabilidad')
y=table(['Producto / sección','Páginas','Rúbrica'],[['A. Diez pasos del flujo Android','3-7','6 puntos'],['B. Comparación Android / iOS','8','5 puntos'],['C. Permisos runtime y ajustes','9','5 puntos'],['D. Ficha de integración y decisiones','10','4 puntos'],['Firebase y evidencias históricas','11','Complemento'],['Resultados, límites y control de calidad','12','Verificación'],['Repositorios, ramas y commits evaluados','13','Trazabilidad']],[330,100,102],40,y,10)
y=p('Método de ejecución',40,y,532,14,teal,True)
y=p('Se reutilizó el APK Debug previamente aprobado y el backend Oracle/JWT operativo. Los perfiles Android nuevos 10 (writer) y 11 (reader) separan cuentas sin borrar el propietario 0. Las capturas se obtuvieron con screencap; los recortes solo eliminan áreas no pertinentes. Los originales y sus SHA-256 están en recorrido.json.',40,y,532)
y=p('Cada paso muestra acción, estado y resultado observado. Los JSON técnicos contienen datos sintéticos, IDs y metadatos del almacén cifrado; nunca el token ni las preferencias originales. Logcat conserva solo rutas, métodos y códigos. Los HTTP 400/403 externos se distinguen de la UI.',40,y,532)
y=p('La barra Android usa hora de 12 horas; los registros técnicos usan UTC y los textos normalizan a Lima. Primera sincronización: 14:59:58; reconexión: 15:03:17 del 06/11/2026. iOS solo fue inspeccionado estáticamente. Firebase nuevo no se configura con datos ficticios.',40,y,532)
finish()
# 3-7: paired steps
for start in range(0,10,2):
    y=base(f'Producto A · Pasos {start+1} y {start+2}')
    for j,s in enumerate(steps[start:start+2]):
        x=40+j*274;cw=258;yy=y
        yy=p(f"{s['paso']:02d} / {escape(s['titulo'])}",x,yy,cw,13,teal,True)
        yy=figure(O/s['recorte'],x,yy,cw,360 if start in [2,4,6] else 280)
        yy=p('Figura A'+str(s['paso'])+'. Captura real Android; recorte del original.',x,yy,cw,8.2,gray)
        yy=p('<b>Acción.</b> '+escape(s['accion']),x,yy,cw,9.6)
        yy=p('<b>Estado.</b> '+escape(s['estado']),x,yy,cw,9.6)
        yy=p('<b>Observado.</b> '+escape(s['resultado']),x,yy,cw,9.6)
        yy=p('<b>Complemento:</b> '+escape(pathlib.Path(s['tecnica']).name),x,yy,cw,8,gray)
    finish()
# 8: platform comparison
y=base('Producto B · Evidencias por plataforma')
y=table(['Aspecto','Android: ejecución real','iOS: solo implementación estática'],[
['Token','AES-GCM/Keystore; metadatos presentes tras login, ausentes tras 401/logout.','Adaptador Keychain/SecItem existente; sin ejecución acreditada.'],
['Persistencia / reinstalación','Cinco filas y vínculo conservados al cierre final. APK instalado con -r. No se desinstaló ni probó backup.','Driver nativo y vínculo declarados. Reinstalación/Keychain no ensayados.'],
['Permisos / ajustes','POST_NOTIFICATIONS en tres estados; recuperación desde Ajustes. INTERNET sin diálogo.','GestorPermisosIosNoDisponible: Denegado y ajustes sin acción.'],
['Offline','Sesión validada: ID -1 → 42; 1 → 0 pendientes. Reapertura offline: solo lectura por diseño.','Lógica común y transporte Darwin configurados; comportamiento no probado.'],
['Notificaciones / Firebase','Permiso observado; no se ensayó entrega push. JSON ausente; DebugView histórico reutilizado.','firebaseConfigurado=false; plist y soporte nativo pendientes.'],
['Interfaz / barras','Compose, barra de estado y navegación gestual reales en las capturas.','SwiftUI/MainViewController existen; safe areas y barras no evaluadas.']],[100,216,216],40,y,9)
y=p('Procedimiento pendiente en macOS',40,y,532,13,teal,True)
y=p('Los targets iosArm64 e iosSimulatorArm64 y el framework Shared están declarados; existe iosApp.xcodeproj. Windows no proporciona Xcode, SDK de Apple ni simulador iOS. En un Mac: recuperar el commit evaluado, configurar Xcode/JDK, compilar Shared y iosApp, resolver interoperabilidad real, configurar firma y ejecutar pruebas iOS. Verificar conectividad al backend: localhost del simulador corresponde al Mac, no a Windows.',40,y,532,9.7)
y=p('Completar permisos nativos y Firebase auténtico si la guía los exige; repetir los escenarios y conservar capturas iOS reales. No hay compilación ni ejecución iOS acreditadas en esta entrega.',40,y,532,9.7)
finish()
# 9: runtime permissions
y=base('Producto C · Bitácora de permisos')
y=p('POST_NOTIFICATIONS · Android 17 / perfil 10',40,y,532,13,teal,True)
top=y
for i,label in enumerate(['Denegado','Denegado permanente','Concedido']):
    x=40+i*180;yy=p(label,x,top,172,10,teal,True)
    figure(E/'recortes'/f'permiso-{i}.png',x,yy,172,155)
y=top-195
y=table(['Estado','Acción y comportamiento observado'],[
['Denegado','No permitir en el diálogo nativo; la app continúa y permite solicitar otra vez.'],
['Permanente','Segunda negativa; muestra Denegado permanentemente y Abrir Ajustes.'],
['Concedido','App → Abrir Ajustes → Notificaciones → Todas las notificaciones; activar y volver a Comprobar estado.']],[105,427],40,y,9.3)
y=p('Permisos normales, de bibliotecas y compatibilidad',40,y,532,12,teal,True)
y=p('INTERNET no requiere solicitud runtime. El manifiesto fusionado añade ACCESS_NETWORK_STATE, WAKE_LOCK, C2DM RECEIVE, BIND_GET_INSTALL_REFERRER_SERVICE, AD_ID, ACCESS_ADSERVICES_ATTRIBUTION/AD_ID y un permiso de firma del receptor. Su declaración no prueba su ejercicio ni implica diálogos gestionados por la app.',40,y,532,9.4)
y=p('Android 17 registra además ACCESS_LOCAL_NETWORK concedido por compatibilidad en esta instalación target 36; no se observó una solicitud propia. POST_NOTIFICATIONS es el único runtime solicitado por GestorPermisos. No hay solicitudes de cámara, ubicación, micrófono ni almacenamiento. Conceder notificaciones no demuestra recepción de un push.',40,y,532,9.4)
y=p('Fuentes: evidencias/permisos/02-dialogo-nativo.png, 03-denegado.png, 04-estado-tras-segunda-denegacion.png, 05-07-ajustes y 08-concedido.png. Capturas nuevas: 06/11/2026; las anteriores del 03/11 se conservan intactas.',40,y,532,8.5,gray)
finish()
# 10: issues, compact professional table
y=base('Producto D · Ficha de integración')
rows=[]
for r in issues:
    rows.append([r[0]+' / '+r[1],r[2]+' Evidencia: '+r[3],r[4],r[5]+(' '+r[6] if r[6]!='—' else ''),r[7]])
y=table(['ID / componente','Problema / evidencia','Sev.','Decisión / cierre','Justificación'],rows,[73,167,36,91,165],40,y,7.8)
y=p('Cerrado hoy exige observación real; Sesión 14 permanece abierto. Descartado se justifica como comportamiento de protección que se conserva, no como eliminación de evidencia.',40,y,532,9,gray)
finish()
# 11: Firebase historical proof
y=base('Firebase · Integración y evidencia histórica')
y=p('Recepción real conservada, no repetida hoy',40,y,532,13,teal,True)
y=figure(F/'docs/s13/evidencias/firebase_debugview_evento_real.png',40,y,532,315)
y=p('Figura F1. Firebase Console / DebugView muestra s13_acerca_abierta en el proyecto PharmaMobile-UPeU Copiar. Imagen histórica incorporada al repositorio el 03/11/2026 16:00:13 -05:00, commit e5b0b2c. No se presenta como captura del 06/11.',40,y,532,9,gray)
y=p('El archivo androidApp/google-services.json está ausente. La app mantiene TelemetriaPendiente; REST/JWT funciona de forma independiente. No se creó configuración falsa ni se publicaron identificadores privados nuevos. Las bitácoras antiguas que dicen DebugView pendiente no reflejan el PNG incorporado después.',40,y,532,10)
y=p('Intervención del propietario: Firebase Console → proyecto existente → Configuración del proyecto → General → Tus apps → Android com.edu.pe → Descargar google-services.json. Colocarlo en androidApp; mantenerlo ignorado por Git. Una nueva validación Firebase requiere el archivo auténtico y una compilación afectada por los plugins.',40,y,532,10)
finish()
# 12: quality and evidence references
y=base('Resultados y control de calidad')
y=table(['Comprobación','Resultado y alcance'],[
['Flujo Android','10 escenarios con PNG, acción/estado y complemento técnico. Se distinguen UI, SQLite/Oracle y HTTP externo.'],
['Online / offline','IDs online 41 y 61 (repetición trazable); pendiente -1 conciliado a 42. Pendientes 1→0; cinco productos tras logout.'],
['HTTP','201 alta; 400 validación remota externa; 403 lector externo; 401 JWT realmente expirado y revocación Android.'],
['Pruebas','81 frontend y 39 backend previamente aprobadas, no reejecutadas en este recorrido visual. Se identifican como antecedentes.'],
['Permisos','Denegado, permanente y concedido: observados hoy. No se afirma entrega push.'],
['Plataformas / Firebase','Android real. iOS estático, no compilado/ejecutado. Firebase: prueba histórica, configuración actual pendiente.'],
['Integridad','Originales preservados; recortes trazables; sin token/contraseñas/DB publicados. Propietario 0 y prácticas anteriores conservados.']],[128,404],40,y,9.5)
y=p('Conclusión basada en resultados',40,y,532,13,teal,True)
y=p('El recorrido REST autenticado funciona en Android con Oracle real, aislamiento por cuenta y sincronización controlada. La revocación y logout retiran secretos de sesión sin borrar el catálogo. Los límites iOS y Firebase actual siguen abiertos; no se usan mocks ni imágenes inventadas para acreditarlos.',40,y,532,10)
y=p('Anexos en GitHub: bitacora.md, plataformas.md, permisos.md, ficha-integracion.md, reproduccion.md, recorrido.json y evidencias/tecnica. Los registros de pruebas previos y la configuración Oracle/JWT están referenciados. El profesor puede cotejar resultados con los commits de la página siguiente.',40,y,532,9.5)
finish()
# 13: non-circular fixed evaluated commits
y=base('Repositorios y commits evaluados')
front_url='https://github.com/07Ismael02/PharmaMobile'
back_url='https://github.com/07Ismael02/PharmaMobile-Backend'
for title,url,branch,sha in [('Frontend',front_url,'feature/integrador-13',front_commit),('Backend',back_url,'feature/s13-autonoma-integracion',back_commit)]:
    y=p(title,40,y,532,16,teal,True)
    y=p(f'<link href="{url}" color="#006D62">{url}</link>',40,y,532,10)
    y=p('Rama: '+branch+'<br/>Commit evaluado exacto:<br/><b>'+sha+'</b>',40,y,532,10.5)
    y=p(f'<link href="{url}/commit/{sha}" color="#006D62">Abrir commit evaluado en GitHub</link>',40,y,532,10)
    y-=18
y=p('Contenido evaluado y entrega final',40,y,532,14,teal,True)
y=p('Los commits anteriores fijan código, documentación y evidencias antes de generar este PDF. Un commit posterior incorpora únicamente el documento y su generador/verificación, y se identifica en el informe de entrega. El PDF no se atribuye a sí mismo un hash futuro: se evita la referencia circular.',40,y,532,10)
y=p(f'<link href="{front_url}/tree/{front_commit}/docs/s13-autonoma" color="#006D62">Abrir anexos de evidencia fijados por commit</link>',40,y,532,10)
y=p('Archivo para el aula virtual:<br/><b>Logacho_Ismael_Sesion13_Evidencias.pdf</b><br/>Formato carta; 13 páginas. Código y grupo no verificados: se omiten.',40,y,532,11)
finish();c.save()
print(json.dumps({'path':str(output),'pages':page,'bytes':output.stat().st_size,'front_evaluado':front_commit,'backend_evaluado':back_commit},ensure_ascii=False))
