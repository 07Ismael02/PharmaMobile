This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Conectividad REST

La pantalla Productos consulta `GET /api/v1/productos?pagina=0&tamanio=20` del backend [PharmaMobile-Backend](https://github.com/07Ismael02/PharmaMobile-Backend). En el emulador Android la URL base es `http://10.0.2.2:8080/api/v1/`; en iOS se configura `http://localhost:8080/api/v1/`. El backend debe estar iniciado y accesible desde el dispositivo correspondiente.

El cliente Ktor compartido usa `ContentNegotiation` con JSON (`ignoreUnknownKeys = true`), `HttpTimeout` (15 segundos por petición y 10 para conectar) y `Logging` en nivel `HEADERS`. Android usa OkHttp y envía esos mensajes a Logcat con la etiqueta `PharmaMobilKtor`; iOS usa Darwin. `ProductoApi` recibe el envoltorio `PaginaResponseDto`, extrae `contenido` y el mapper convierte cada `ProductoResponseDto` al modelo de dominio, incluido `estado` → `activo`. La UI y el ViewModel no consumen DTO directamente.

En Sesión 07, `listar()` consulta REST mientras `registrar()` permanece en memoria; el CRUD REST completo corresponde a Sesión 08. La lista con tres productos del backend se comprobó en Android. La captura de Logcat GET/200 continúa pendiente. iOS está configurado, pero no se ha compilado ni ejecutado desde este equipo Windows.
