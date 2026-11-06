# Producto B - Evidencias por plataforma

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
