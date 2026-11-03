"""Genera el informe S13 desde capturas originales, sin alterarlas en disco."""

from io import BytesIO
from pathlib import Path

from PIL import Image as PillowImage
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Image, KeepTogether, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
)

BASE = Path(__file__).resolve().parent
EVIDENCIAS = BASE / "evidencias"
SALIDA = BASE / "PharmaMobile_S13_Evidencias_Logacho.pdf"

pdfmetrics.registerFont(TTFont("Arial", r"C:\Windows\Fonts\arial.ttf"))
pdfmetrics.registerFont(TTFont("ArialBold", r"C:\Windows\Fonts\arialbd.ttf"))
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="TituloS13", fontName="ArialBold", fontSize=24, leading=29,
                          textColor=colors.HexColor("#006D55"), spaceAfter=12))
styles.add(ParagraphStyle(name="SubS13", fontName="ArialBold", fontSize=15, leading=19,
                          textColor=colors.HexColor("#006D55"), spaceAfter=10))
styles.add(ParagraphStyle(name="CuerpoS13", fontName="Arial", fontSize=10.2, leading=15,
                          spaceAfter=8))
styles.add(ParagraphStyle(name="NotaS13", fontName="Arial", fontSize=8.7, leading=12,
                          textColor=colors.HexColor("#4F5B58"), spaceAfter=8))


def p(text, style="CuerpoS13"):
    return Paragraph(text, styles[style])


def captura(nombre, limite_superior=None, inicio=0, alto_max=610):
    origen = EVIDENCIAS / nombre
    with PillowImage.open(origen) as original:
        imagen = original.convert("RGB")
        if limite_superior is not None or inicio:
            imagen = imagen.crop((0, inicio, imagen.width, limite_superior or imagen.height))
        bufer = BytesIO()
        imagen.save(bufer, format="PNG")
        bufer.seek(0)
        factor = min(470 / imagen.width, alto_max / imagen.height)
        return Image(bufer, width=imagen.width * factor, height=imagen.height * factor)


def pagina_evidencia(titulo, descripcion, nombre, limite_superior=None, inicio=0):
    elementos.extend([PageBreak(), p(titulo, "SubS13"), p(descripcion),
                      captura(nombre, limite_superior, inicio), Spacer(1, 7 * mm),
                      p(f"Fuente: captura ADB real, archivo original {nombre}.", "NotaS13")])


def pie(canvas, doc):
    canvas.setStrokeColor(colors.HexColor("#C9D9D2"))
    canvas.line(18 * mm, 16 * mm, 192 * mm, 16 * mm)
    canvas.setFont("Arial", 8)
    canvas.setFillColor(colors.HexColor("#52635D"))
    canvas.drawString(18 * mm, 11 * mm, "PharmaMobil - Guía Práctica N.º 13")
    canvas.drawRightString(192 * mm, 11 * mm, f"Página {doc.page}")


elementos = [
    Spacer(1, 34 * mm),
    p("UNIDAD 3 · PRÁCTICA 13", "SubS13"),
    p("Proyecto integrador PharmaMobil", "TituloS13"),
    p("Evidencias reales de validación Android", "SubS13"),
    Spacer(1, 10 * mm),
    p("<b>Estudiante:</b> Richard Ismael Logacho Rivera"),
    p("<b>Fecha:</b> 3 de noviembre de 2026"),
    p("<b>Proyecto:</b> PharmaMobil (Kotlin Multiplatform) con PharmaSoft"),
    p("<b>Rama:</b> feature/integrador-13 · commit inicial de validación a3f6dea"),
    p("<b>Repositorio:</b> https://github.com/07Ismael02/PharmaMobile"),
    Spacer(1, 15 * mm),
    p("Alcance", "SubS13"),
    p("Se instaló el APK S13 sobre una instalación existente en el emulador Android 17, "
      "sin borrar datos. Las imágenes proceden de capturas ADB y se conservan originales. "
      "Los recortes de presentación solo mejoran legibilidad en este documento."),
    p("El backend no respondió en localhost:8080 durante la validación funcional anterior; "
      "no se presentan login nuevo ni sincronización por red como verificados. La prueba "
      "posterior de Analytics se documenta por separado.", "NotaS13"),
    PageBreak(),
    p("Matriz del recorrido real", "SubS13"),
]

filas = [
    ["Comprobación", "Resultado observado"],
    ["Instalación", "adb install -r: Success; datos conservados"],
    ["Inicio sin red", "Sesión previa restaurada en modo local de solo lectura"],
    ["Catálogo local", "Visible; última actualización y 3 filas en revisión"],
    ["Permiso Android", "Denegado, permanente y concedido observados"],
    ["Login y sync online", "No verificados: backend inaccesible"],
    ["Pendiente nuevo / logout", "No ejecutados para conservar la sesión y datos existentes"],
    ["Firebase", "Evento registrado y subida HTTP 204; DebugView pendiente"],
]
tabla = Table([[p(c, "NotaS13") for c in fila] for fila in filas], colWidths=[47 * mm, 126 * mm], repeatRows=1)
tabla.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#D9EFE7")),
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD7D0")),
    ("LEFTPADDING", (0, 0), (-1, -1), 7),
    ("RIGHTPADDING", (0, 0), (-1, -1), 7),
    ("TOPPADDING", (0, 0), (-1, -1), 7),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
]))
elementos.extend([
    tabla, Spacer(1, 9 * mm), p("Pruebas automatizadas previas", "SubS13"),
    p("<b>81 pruebas Android host, 0 fallos, 0 errores.</b> La compilación "
      "<b>:androidApp:assembleDebug</b> concluyó con BUILD SUCCESSFUL. "
      "La suite no se volvió a ejecutar durante esta validación visual."),
    p("El reporte HTML de Gradle se encuentra en shared/build/reports/tests/testAndroidHostTest/index.html.", "NotaS13"),
    p("Limitaciones", "SubS13"),
    p("No se compiló ni ejecutó iOS en Windows. No se modificó PharmaMobile-Backend "
      "ni se alteraron los tres pendientes históricos en revisión."),
])

pagina_evidencia("Catálogo local sin conexión", "La app muestra antigüedad de los datos, "
                 "0 cambios por enviar y 3 registros que requieren revisión.",
                 "03-productos-offline-real.png", 1160)
pagina_evidencia("Filas históricas en revisión", "Los productos locales continúan visibles "
                 "y no se reenvían automáticamente por nombre.",
                 "03-productos-offline-real.png", 2390, 1090)
pagina_evidencia("Permiso: solicitud del sistema", "El diálogo nativo Android apareció "
                 "al pulsar Solicitar permiso en Acerca de.", "05-permiso-dialogo-real.png", 1690)
pagina_evidencia("Permiso: denegado", "Tras la primera denegación, la app continuó "
                 "operativa y ofreció volver a solicitar el permiso.", "06-permiso-denegado-real.png", 1190)
pagina_evidencia("Permiso: denegado permanentemente", "La segunda denegación produjo "
                 "el estado permanente y el botón Abrir Ajustes.", "07-permiso-permanente-real.png", 1270)
pagina_evidencia("Permiso: concedido", "Se activaron las notificaciones en Ajustes "
                 "y la app mostró Concedido al comprobar nuevamente el estado.",
                 "11-permiso-concedido-real.png", 1100)
pagina_evidencia("Firebase Analytics: acción real en Android", "Al abrir Acerca de en Pixel_9a "
                 "se ejecutó Telemetria.registrarEvento con s13_acerca_abierta. Logcat FA-SVC "
                 "registró el evento y confirmó subida HTTP 204. La recepción visual en "
                 "Firebase Console DebugView aún no fue observada.",
                 "12-firebase-acerca-real.png", 1150)

elementos.extend([
    PageBreak(), p("Bitácora y ficha Firebase", "SubS13"),
    p("<b>Permiso seleccionado:</b> POST_NOTIFICATIONS. La primera consulta devolvió Denegado; "
      "la primera negativa mantuvo Denegado; la segunda solicitud mostró en el sistema "
      "la opción deny_and_dont_ask_again y la app pasó a Denegado permanentemente. "
      "Abrir Ajustes llevó a la ficha de la app; al habilitar notificaciones y comprobar, "
      "la app mostró Concedido."),
    p("<b>Firebase:</b> contrato Telemetria en dominio y adaptador Analytics/Crashlytics en datos. "
      "El archivo local google-services.json corresponde a com.edu.pe y está excluido de Git. "
      "Google Services y Crashlytics se aplicaron al APK. El evento s13_acerca_abierta "
      "se registró en Pixel_9a; el servicio de Google Analytics respondió HTTP 204. "
      "No se pudo confirmar su presencia visual en DebugView; falta la captura auténtica de consola."),
    p("<b>Pendiente para el cierre funcional:</b> levantar PharmaSoft de forma controlada, "
      "verificar login y datos REST, crear un pendiente nuevo con una sesión online validada, "
      "reconectar, confirmar su ID positivo y registrar capturas. Ejecutar logout solo cuando "
      "se pueda volver a iniciar sesión."),
    p("<b>Integridad de la evidencia:</b> no se simularon imágenes, respuestas del backend, "
      "recepción en DebugView ni resultados iOS. Las capturas originales permanecen en docs/s13/evidencias/."),
])

doc = SimpleDocTemplate(str(SALIDA), pagesize=A4, rightMargin=18 * mm,
                        leftMargin=18 * mm, topMargin=20 * mm, bottomMargin=22 * mm,
                        title="PharmaMobil S13 - Evidencias Android",
                        author="Richard Ismael Logacho Rivera")
doc.build(elementos, onFirstPage=pie, onLaterPages=pie)
print(SALIDA)
