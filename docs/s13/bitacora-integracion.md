# Sesión 13 — recorrido y huecos

Autor: Richard Ismael Logacho Rivera. Base: `feature/offline-first-logacho` en `05e7610`.

Esta tabla es una auditoría del código. Los resultados visuales y de red requieren comprobación manual; no se presentan como pruebas ejecutadas.

| Momento | Hallazgo comprobado en código | Gravedad | Decisión |
| --- | --- | --- | --- |
| Primera apertura | `App` dirige a login sin sesión validada. | Baja | Verificar visualmente. |
| Credenciales incorrectas | `AuthRepositorioRest` distingue rechazo de conexión. | Media | Verificar mensaje en emulador. |
| Catálogo local vacío | `ProductoScreen` muestra «No hay productos registrados». | Baja | Conservar. |
| Registro sin red | `ProductoLocal` guarda ID negativo; la categoría debe haberse descargado. | Media | Mostrar contador de pendientes; verificar manualmente. |
| Reconexión | Se sube antes de bajar, con conciliación por ID de respuesta. | Alta | Conservar serialización; añadir estados e intentos sin reenviar POST ambiguos. |
| Validación del servidor | `ProductoViewModel` asocia errores a campos. | Baja | Verificar con backend real. |
| JWT vencido en formulario | Revocación de ámbito existente; no se garantiza conservar el formulario. | Media | Documentar para sesión 14. |
| Logout | Se revoca el ámbito y se limpia el token; los pendientes de cuenta permanecen. | Alta | Verificar en emulador sin borrar datos. |

`pharma.db` histórica permanece en cuarentena. Las bases de cuenta existentes se migran sin borrado.
