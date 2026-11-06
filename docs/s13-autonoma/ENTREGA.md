# Entrega académica S13

Archivo para subir al aula virtual: [Logacho_Ismael_Sesion13_Evidencias.pdf](Logacho_Ismael_Sesion13_Evidencias.pdf).

- Carta, 13 páginas, 1 286 041 bytes; comprobación de texto, renders PNG, enlaces y secretos en verificacion-pdf.json.
- Frontend evaluado: feature/integrador-13, f0d1e13f0a444d98cd0acb81875d39f6340d02ad.
- Backend evaluado: feature/s13-autonoma-integracion, 6f6f619fb2c81be10f02491768fc01357592c860.
- Este commit de entrega añade PDF, generador y verificación después de fijar el contenido evaluado. No hay referencia circular ni merge a main.

A: diez escenarios Android ejecutados; PNG originales, recortes, hashes y complemento técnico. B: comparación Android/iOS con limitaciones explícitas y procedimiento macOS. C: tres estados POST_NOTIFICATIONS y recuperación por Ajustes. D: problemas cerrados y abiertos, con severidad y decisiones justificadas.

81 pruebas frontend y 39 backend fueron aprobadas previamente y no repetidas durante el recorrido visual. La repetición del alta online conservó el HTTP 201 explícito y añadió ID 61; los IDs 41 y 42 anteriores no se borraron. JWT de 60 s realmente expirado produjo 401; configuración restaurada a 3600 s. Logout retira token/identidad y conserva cinco productos/vínculo.

Pendientes declarados: iOS no compilado ni ejecutado en Windows; permisos nativos iOS no implementados. JSON auténtico Firebase ausente; se reutiliza DebugView histórico del 03/11/2026. No se probó entrega de notificaciones push. Mejora futura del mensaje de alta online y del formulario al expirar: ficha de integración.

Para reproducir el documento se usaron ReportLab y Pillow; para verificar, pypdf y Poppler. Estaban disponibles en el runtime, sin instalaciones adicionales. Desde Windows con fuentes Arial:

```powershell
python tools/generar_pdf.py f0d1e13f0a444d98cd0acb81875d39f6340d02ad 6f6f619fb2c81be10f02491768fc01357592c860
```

Los anexos evaluados permanecen fijados en el commit de contenido; el PDF entrega su interpretación y referencias. No se publicaron secretos, bases, preferencias Android ni AGENTS.md personales.
