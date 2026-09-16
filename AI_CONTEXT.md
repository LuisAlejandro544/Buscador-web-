# Contexto Técnico para Agentes de Inteligencia Artificial (AI Context)

Este archivo proporciona el contexto fundamental del proyecto para cualquier modelo de lenguaje o agente de IA que interactúe con el código fuente.

---

## 🎯 Perfil del Proyecto y del Usuario

1. **Entorno del Usuario:**
   - El usuario desarrolla e interactúa directamente desde un dispositivo móvil (teléfono), sin ordenador de escritorio.
   - Las respuestas, resúmenes y explicaciones deben ser claras, directas y legibles en pantallas móviles.

2. **Canal de Distribución:**
   - La aplicación está orientada a distribución en plataformas de terceros (como **Uptodown**, repositorios independientes o descarga directa de APK), **no** para Google Play Store.
   - No aplican restricciones artificiales de Google Play sobre empaquetado si ello limita la funcionalidad del navegador o de GeckoView.

3. **Política sobre Dependencias y Tamaño:**
   - El peso final del APK no es una limitación crítica para el usuario.
   - **Prioridad absoluta:** Dependencias oficiales, robustas y 100% funcionales (como `org.mozilla.geckoview:geckoview-omni`). No recurrir a soluciones simplificadas o "hacks" sin dependencias cuando una librería profesional soluciona el problema.

4. **Estilo Visual e Interfaces:**
   - **No al minimalismo extremo:** La interfaz debe ser visualmente rica, moderna, atractiva e interactiva.
   - **Separación de Pantallas:** No colocar todas las funciones en una sola pantalla saturada. Cada flujo principal (Navegador, Pestañas, Marcadores, Historial, Ajustes) debe residir en su propia pantalla dedicada con botones y navegación clara entre ellas.

5. **Compatibilidad de Arquitecturas y Capa Nativa:**
   - Debe soportar procesadores tanto de 32 bits como de 64 bits (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
   - El empaquetado de librerías nativas C++ (`.so`) de GeckoView requiere `useLegacyPackaging = true` en `app/build.gradle.kts`.
   - Infraestructura nativa preparada para **C++26 / C23** (NDK r28, CMake 3.31+) y **Rust 2024 / 1.89+** para tareas pesadas y de seguridad.
   - El archivo `.gitignore` debe mantener estrictamente excluidos los directorios y archivos de compilación temporal de CMake (`CMakeFiles`, `*.ninja`, `compile_commands.json`), Clang (`*.o`, `*.so`), NDK (`.cxx`, `.externalNativeBuild`) y Cargo/Rust (`target/`, `*.rlib`, etc.).

6. **Versión Mínima de Android:**

   - El proyecto utiliza `minSdk = 29` (Android 10) y `compileSdk = 36` (Android 16).
   - Cualquier cambio en el SDK mínimo debe justificarse estrictamente según las APIs requeridas por GeckoView y Jetpack Compose.

---

## 🏗️ Directrices de Código y Desarrollo

- **Explicación del Código:** Cada archivo nuevo o modificado debe contener comentarios explicativos en español que detallen el propósito de la clase, funciones y lógica principal para facilitar la comprensión.
- **Desarrollo Modular:** Evitar crear archivos de más de 500 líneas. Proactivamente dividir la lógica en componentes y casos de uso reutilizables.
- **Persistencia Reactiva:** Toda la información local (pestañas, historial, favoritos, descargas) se gestiona mediante Room Database con `StateFlow` y corrutinas de Kotlin.
- **Manejo de Idioma:** Las cadenas visibles al usuario se declaran en `res/values/strings.xml`. Documentación e información de commits se redactan en español.

---

## ⚡ Módulos y Capacidades del Motor GeckoView

1. **Sincronización Dinámica de Ajustes:**
   - La configuración de `isJavaScriptEnabled`, `isDoNotTrackEnabled` y modo escritorio se sincroniza de forma reactiva desde `BrowserPreferences` / `BrowserViewModel` hacia `GeckoSessionManager` y los objetos `GeckoSessionSettings` de cada pestaña activa y futura en tiempo real.

2. **Gestor de Descargas Avanzado:**
   - GeckoView intercepta URLs con cabeceras `Content-Disposition: attachment` o tipos MIME no renderizables mediante `GeckoSession.ContentDelegate.onExternalResponse(session, response)`.
   - `DownloadManagerHelper` normaliza el nombre de archivo, asigna carpetas seguras (`Environment.DIRECTORY_DOWNLOADS`) y encola en `android.app.DownloadManager`.
   - Los registros de descargas se persisten en Room (`DownloadEntity`, `DownloadDao`) y se gestionan desde `DownloadsScreen.kt`.

3. **Gestión de Diálogos Web Nativos:**
   - GeckoView no dibuja diálogos por defecto: requiere un `PromptDelegate`.
   - `GeckoPromptHandler` captura solicitudes web asíncronas (`ALERT`, `CONFIRM`, `PROMPT`, `AUTH`, `FILE_CHOOSER`) y emite objetos `WebPromptRequest` al `BrowserViewModel`.
   - `WebPromptDialog.kt` renderiza componentes nativos de Jetpack Compose acordes a Material 3 y devuelve la resolución `PromptResult` a GeckoView.

4. **Subsistema Nativo (C++26 CMake + Rust 2024 Cargo):**
   - **C++ (CMake):** Configurado en `app/src/main/cpp/CMakeLists.txt` con NDK r28 y estándares C++26 / C23. Compila la librería compartida JNI `libbrowser_native.so` para `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`.
   - **Rust (Cargo):** Módulo `core-native/` con `Cargo.toml` (Rust Edition 2024, staticlib/cdylib) y `src/lib.rs` para validaciones a bajo nivel y hashing de URLs.
   - **Puente Kotlin:** `NativeBridge.kt` gestiona la carga dinámica mediante `System.loadLibrary("browser_native")` con control de excepciones y fallback seguro para pruebas JVM.

5. **Pestañas Protegidas (Aislamiento de Sesión por Contenedor Contextual):**
   - **Aislamiento Multi-Account Container:** Utiliza `GeckoSessionSettings.Builder.contextId(String)` asignando un UUID único por pestaña protegida. Las cookies, caché, `localStorage` e inicios de sesión quedan confinados exclusivamente a esa pestaña.
   - **Sin Afectación a Cuentas:** Aceptar cookies o autenticarse en una pestaña protegida no interfiere con las pestañas normales ni con el perfil del usuario.
   - **Destrucción y Purga Automática:** Al cerrarse la pestaña, `GeckoSessionManager` invoca `runtime.storageController.clearDataForSessionContext(contextId)` purgando todos los datos del contenedor de forma irreversible.
   - **Persistencia en Room v3:** Almacenado con `isProtected = true` y `contextId` persistido para reconexión de sesión durante la navegación.
   - **Diseño UI:** Distintivo en color esmeralda (`#00897B`), icono de escudo (`Icons.Default.Shield`), pestaña dedicada en `TabsScreen` y página de inicio `BrowserStartPage` adaptada.

6. **Integración Continua, Firma Automática y Artefactos Divididos en GitHub Actions:**
   - **Workflow (`.github/workflows/build-debug.yml`):** Activación exclusivamente manual (`workflow_dispatch`), compilación limpia sin caché (`cache-disabled: true`, `--no-build-cache`), instalación de NDK 28 (`28.2.13676358`), CMake 3.31.6, Rust 2024 y descarga de GeckoView Omni de Mozilla.
   - **Script de Firma (`generate_debug_keystore.sh`):** Fuerza la generación no interactiva de un archivo `debug.keystore` (PKCS12, RSA 2048 bits) desde cero en el runner de CI para firmar los APKs sin requerir interacción ni secretos preexistentes.
   - **División por Arquitecturas (ABI Splits):** Gradle genera APKs independientes (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`) con `isUniversalApk = false`. Esto reduce sustancialmente el peso de descarga al incluir únicamente las librerías nativas de cada plataforma.
   - **Artefactos Separados en GitHub:** Los binarios se suben a GitHub Actions en artefactos individuales (`app-debug-arm64-v8a`, `app-debug-armeabi-v7a`, `app-debug-x86_64`, `app-debug-x86`) para que el usuario descargue directamente desde el móvil solo la variante requerida.

7. **Auditoría y Gestión de Cookies de Navegación (`CookiesScreen.kt`):**
   - **Esquema Room v4:** Persistencia en `CookieEntity` y `CookieDao` de cookies con metadatos clave (nombre, valor, dominio, ruta, caducidad, `isSecure`, `isHttpOnly`, `isTracker`).
   - **Detección de Rastreadores:** Clasificación automatizada de dominios de telemetría y anuncios de terceros con advertencia visual y filtro dedicado.
   - **Sincronización con GeckoView StorageController:** Purgas selectivas o totales coordinadas entre Room y el motor web mediante `storageController.clearDataFromHost(host, CLEAR_COOKIES)` y `storageController.clearData(CLEAR_COOKIES)`.
   - **Punto de Entrada en UI:** Accesible desde el menú desplegable de 3 puntos del navegador y desde la sección de Privacidad en Ajustes.

8. **Fijación de Escala Tipográfica para Móviles (`fontScale = 1.0f`):**
   - **Inmunidad ante Fuentes Gigantes del Sistema:** Sobrescritura de `LocalDensity` en `Theme.kt` manteniendo la densidad de pantalla pero fijando estrictamente `fontScale = 1.0f`. Garantiza que las interfaces permanezcan perfectamente proporcionadas sin recortes ni desbordamientos en teléfonos donde el usuario tiene configurada una fuente de sistema extra grande.
   - **Padding Táctil:** Inclusión de `navigationBarsPadding()` en la barra inferior para evitar solapamientos con la botonera de navegación de Android.

9. **Miniaturas de Pestañas, Hibernación tras 5 Minutos y Modo Incógnito:**
   - **Captura Gráfica de Miniaturas:** `TabThumbnailManager` y `GeckoViewEngine.captureThumbnail` capturan la pantalla web mediante `GeckoDisplay.capturePixels()` para mostrar exactamente dónde dejó el usuario cada pestaña al navegar o alternar.
   - **Hibernación no Agresiva tras 5 Minutos:** Monitor de inactividad en `BrowserViewModel` que revisa periódicamente el tiempo de último uso (`lastActiveTimestamp`). Si una pestaña supera los 5 minutos sin foco, invoca `GeckoSessionManager.hibernateSession()` cerrando el proceso pesado de GeckoView para liberar RAM y batería en el móvil. Los metadatos y la miniatura permanecen visibles con el distintivo "En reposo (5 min)"; al tocar la pestaña, se despierta y restaura instantáneamente.
   - **Creación Manual en Incógnito y Protegidas:** Los modos "Incógnito" y "Protegidas" ya no abren pestañas automáticas vacías al seleccionarlos; el usuario decide y crea manualmente cada sesión cuando realmente lo necesita.

10. **Blindaje de Seguridad Avanzada para Modo Incógnito (No Genérico):**
    - **Protección contra Huella Digital (Resist Fingerprinting - RFP):** Inyección de preferencias avanzadas estilo Tor (`privacy.resistFingerprinting = true`) para unificar resolución de pantalla, canvas hash, WebGL y audio fingerprint, junto a un User-Agent genérico Tor/ESR para evitar rastreos biométricos o del modelo exacto de teléfono.
    - **Bloqueo de Fugas por WebRTC (Anti IP-Leak):** Desactivación completa de `media.peerconnection` y aislamiento STUN (`media.peerconnection.ice.no_host = true`, `media.peerconnection.ice.default_address_only = true`) sumado a denegación estricta de permisos de cámara/micrófono en `GeckoViewEngine`, impidiendo que scripts externos descubran la IP privada o pública del usuario.
    - **DNS sobre HTTPS Cifrado (DoH):** Forzado de resolución cifrada mediante protocolo TRR (Trusted Recursive Resolver) apuntando a Cloudflare/Mozilla (`TRR_MODE_FIRST`), imposibilitando que operadores de telefonía o proveedores de internet (ISP) intercepten las solicitudes de dominios.
    - **Protección Total de Cookies (Total Cookie Protection / dFPI):** Aislamiento dinámico estricto (`ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS`, `privacy.partition.network_state = true`) confinando cualquier cookie o estado de almacenamiento al dominio de primer nivel sin permitir cruces entre sitios.
    - **Purga de Memoria RAM Inmediata al Cerrar:** Al cerrar una pestaña de incógnito o limpiar el grupo privado, se purgan de inmediato las cachés volátiles (`ALL_CACHES` y `AUTH_SESSIONS`) con `storageController.clearData`, se destruyen las miniaturas en disco/RAM y se fuerza la recolección de basura con `System.gc()`.
    - **Protección de Pantalla FLAG_SECURE:** Bloqueo de capturas de pantalla, grabadores externos y enmascaramiento visual en la vista de aplicaciones recientes de Android al entrar a incógnito.
    - **Evolución Continua:** El modo incógnito continuará expandiéndose con capas defensivas adicionales (bloqueo heurístico de telemetría, ofuscación de red y sandboxing estricto) para que sea un entorno de navegación de máxima seguridad real, muy superior a los modos privados genéricos del mercado.

11. **Gestión de Cuentas e Identidad Web Integrada (`AccountCredentialManager` y `WebSignInBridge`):**
    - **Credenciales Nativas AndroidX:** Integración con `androidx.credentials` (`CredentialManager`), `GetCredentialRequest` y `GetGoogleIdOption` en `AccountCredentialManager.kt`. Permite enlazar cuentas nativas de Google u otros proveedores en el dispositivo sin necesidad de bibliotecas legacy pesadas.
    - **Persistencia en Room (`UserAccountEntity` y `UserAccountDao`):** Almacena perfiles vinculados con `email`, `displayName`, `photoUrl`, `idToken`, `provider` y el estado activo `isActive`. Soporta múltiples cuentas y conmutación ágil del perfil activo.
    - **Detección Automática de Autenticación (`WebSignInBridge`):** Inspecciona las URLs cargadas en el motor web mediante patrones de detección de dominios de login (Google Accounts, OAuth2, OpenID Connect, FedCM y formularios de acceso comunes).
    - **Banner Interactivo en Compose (`WebSignInPromptBanner`):** Muestra un banner estilo Google One-Tap en la parte superior del navegador con la cuenta activa ("Continuar como [Nombre]").
    - **Inyección y Autocompletado en GeckoView:** Al confirmar el banner, `BrowserViewModel` invoca `engineController?.evaluateJavascript` con el script generado por `WebSignInBridge.generateSignInScript()`, autocompletando los campos de correo o activando los botones de acceso de Google.
    - **Pantalla Dedicada (`AccountsScreen.kt`):** Pantalla completa para gestionar cuentas, cambiar la identidad activa, vincular nuevas cuentas o eliminarlas, accesible desde el menú desplegable y desde Ajustes.

12. **Gestor de Permisos por Sitio Web y Políticas de "No Preguntar" (`SitePermissionsScreen.kt`):**
    - **Control Granular en Base de Datos (Room v8):** `SitePermissionEntity` almacena `origin`, `permissionType` (`MICROPHONE`, `CAMERA`, `GEOLOCATION`, `NOTIFICATION`, `PERSISTENT_STORAGE`), `status` (`GRANTED`, `DENIED`) y marca temporal `updatedAt`.
    - **Integración con `GeckoSession.PermissionDelegate`:** En `GeckoViewEngine`, intercepta tanto `onContentPermissionRequest` como `onMediaPermissionRequest`. Primero verifica las directivas de silenciamiento, luego consulta el historial almacenado en Room y, si no hay decisión previa, abre un diálogo nativo interactivo (`WebPromptRequest.Permission`) con opción de recordar la respuesta.
    - **Políticas Silenciosas de "No Preguntar":** Preferencias booleanas en DataStore (`block_notification_prompts`, `block_location_prompts`, `block_media_prompts`) expuestas en ViewModel para denegar automáticamente estas solicitudes sin mostrar popups invasivos al usuario.
    - **Pantalla Dedicada:** Permite auditar qué permisos tiene cada dominio, alternar su estado en vivo, eliminar permisos individuales para que el sitio vuelva a preguntar o purgar todos los permisos registrados con confirmación. Accesible desde Ajustes > Privacidad y Seguridad.

13. **Sistema de Efectos de Sonido Nativos y Procesamiento de Audio (`SoundEffectManager.kt`):**
    - **Arquitectura de Cero Latencia con SoundPool:** Utiliza la API nativa `android.media.SoundPool` con atributos de sonificación para reproducir efectos cortos en formato PCM `.wav` precargados en memoria sin gastar ciclos de CPU descomprimiendo audio.
    - **Banco de Tonos de Logro para Descargas:** Extraídos de un paquete de *achievement chimes* (`635665__laurenponder__achievment-chimes.wav`) ubicado en `tools/audio/`, divididos en 6 archivos limpios (`download_success_1.wav` a `download_success_6.wav`) en `app/src/main/res/raw/`. Al finalizar una descarga con éxito en `DownloadEngine`, se selecciona y reproduce uno aleatoriamente acompañado de una vibración háptica rápida.
    - **Herramienta de Procesamiento `tools/audio_processor.sh`:** Script ejecutable en Bash y Python que permite convertir audio entre formatos (WAV, MP3, OGG), recortar intervalos con micro-fade (`trim`), dividir paquetes multi-audio por silencios (`split-silence`) y extraer paquetes de tonos (`split-chimes`).
    - **Preferencias en DataStore:** Conmutador `sound_effects_enabled` en `BrowserPreferences` y `BrowserViewModel` integrado con la pantalla de Ajustes, con botón para probar la reproducción en vivo.
    - **Expansión Futura:** Planificada la adición de nuevos efectos de sonido para marcadores ("pop"), cierre de pestañas ("whoosh") y alertas de seguridad.

14. **Gestión de Extensiones Web (WebExtensions) Bajo Demanda y Protección Legal:**
    - **Conexión Directa con Mozilla Add-ons (AMO):** El APK **no** incorpora binarios de extensiones en sus `assets`, evitando licencias víricas o restrictivas (GPLv3). La descarga se realiza bajo demanda directa y consentimiento del usuario desde los servidores oficiales de Mozilla (`addons.mozilla.org`).
    - **Gestor Desacoplado (`ExtensionManager.kt`):** Se apoya en `GeckoRuntime.webExtensionController` para la descarga asíncrona de ficheros `.xpi`, reporte de progreso porcentual, instalación en caliente y control de ciclo de vida (`enable`, `disable`, `uninstall`).
    - **Catálogo Curado (`RecommendedExtension.kt`):** Incluye uBlock Origin (bloqueo de publicidad/rastreo), Dark Reader (modo oscuro), TWP (traducción) y ClearURLs (desinfección de enlaces).
    - **Pantalla Dedicada (`ExtensionsScreen.kt`):** Gestión visual de complementos activos, catálogo de instalación rápida e instalación mediante URL directa.

15. **Onboarding y Configuración Inicial (`OnboardingScreen.kt`):**
    - **Flujo de Bienvenida:** Se ejecuta exclusivamente en la primera apertura de la app (controlado por `onboarding_completed` en DataStore).
    - **Selección de Buscador Predeterminado:** Permite al usuario elegir entre DuckDuckGo, Google, Bing, Brave o Ecosia antes de comenzar a navegar.
    - **Preselección de Extensiones:** Ofrece la lista recomendada para que el usuario elija cuáles descargar e instalar automáticamente en segundo plano.

16. **Endurecimiento de Seguridad (Security Hardening):**
    - **Cifrado Fail-Closed:** Uso de `EncryptedSharedPreferences` con fallback seguro en memoria ante fallos del hardware Keystore.
    - **Mitigación de Path Traversal:** Verificación estricta de rutas canónicas (`canonicalPath`) en descargas para evitar fugas de archivos.
    - **Configuración de Seguridad de Red:** Restricción estricta de tráfico en texto claro con `cleartextTrafficPermitted="false"`, habilitando excepciones únicamente para `localhost`.
    - **FileProvider Aislado:** Directorio dedicado y restringido `share/` para compartir archivos de forma segura.



