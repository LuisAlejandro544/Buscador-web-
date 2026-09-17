# Hoja de Ruta del Proyecto (Roadmap)

Este documento traza las fases de desarrollo, hitos completados y objetivos futuros para la evolución del navegador web.

---

## 📍 Estado General del Proyecto

- **Fase Actual:** Fase 2 (Integración e infraestructura del motor GeckoView).
- **Objetivo Final:** Navegador independiente de alto rendimiento con motor GeckoView completo, soporte para extensiones web de Mozilla (WebExtensions) y distribución directa en tiendas de terceros (Uptodown, APKMirror).

---

## 🚀 Fases de Desarrollo

### 🟢 Fase 1: Arquitectura Base y Modularidad de Pantallas (Completada)
- [x] Configuración de Jetpack Compose y diseño Material 3 con tema dinámico claro/oscuro.
- [x] Arquitectura MVVM con `BrowserViewModel` y `StateFlow`.
- [x] Separación modular de pantallas:
  - `BrowserScreen`: Visor de navegación principal, omnibox inteligente y barra de herramientas inferior.
  - `TabsScreen`: Vista en cuadrícula para alternar entre pestañas normales y de incógnito, crear y cerrar pestañas.
  - `BookmarksScreen`: Pantalla dedicada de marcadores con búsqueda en vivo y borrado.
  - `HistoryScreen`: Pantalla dedicada de historial con búsqueda y limpieza por elemento o total.
  - `SettingsScreen`: Pantalla dedicada de ajustes (motores de búsqueda, DNT, JavaScript, limpieza de datos).
- [x] Persistencia local con **Room Database** para pestañas, marcadores e historial.
- [x] Implementación de `BrowserEngineContract` como contrato abstracto desacoplado.

---

### 🟡 Fase 2: Cableado del Motor GeckoView y Subsistema Nativo (Completada)
- [x] Incorporación del repositorio oficial Maven de Mozilla (`https://maven.mozilla.org/maven2/`).
- [x] Integración de la dependencia `org.mozilla.geckoview:geckoview-omni` en `libs.versions.toml` y `app/build.gradle.kts`.
- [x] Configuración de empaquetado nativo JNI (`useLegacyPackaging = true`) para librerías binarias nativas C++ de Gecko en arquitecturas `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`.
- [x] Establecimiento de base de compilación nativa en `libs.versions.toml` y Gradle: NDK 28 LTS (`28.2.13676358`), CMake 3.31.6, flags C++26 (`-std=c++26 -O3`), C23 (`-std=c23 -O3`) y Rust Edition 2024 / 1.89.0.
- [x] Implementación de `GeckoViewEngine` implementando `BrowserEngineContract`.
- [x] Configuración del singleton `GeckoRuntimeProvider` con ETP Estricto y optimizaciones de memoria para dispositivos móviles.
- [x] Implementación de `GeckoSession` por pestaña en `GeckoSessionManager` con delegados de navegación, progreso, seguridad y recuperación ante caídas.
- [x] **Sincronización Dinámica de Ajustes:** Conexión en tiempo real entre DataStore/ViewModel y GeckoView para alternar al instante JavaScript, Do Not Track (DNT) y Modo Escritorio por pestaña.
- [x] **Gestor de Descargas Avanzado:** Intercepción de respuestas no renderizables con `WebResponse`, encolamiento en el `DownloadManager` de Android, persistencia en Room (`DownloadEntity`/`DownloadDao`) y pantalla dedicada con acciones de apertura y búsqueda.
- [x] **Gestión de Diálogos Web:** Intercepción de llamadas nativas de scripts mediante `GeckoPromptHandler` (`PromptDelegate`), soportando alertas JS, confirmaciones, solicitud de texto, autenticación HTTP y selector de archivos `<input type="file">` presentados en Jetpack Compose con `WebPromptDialog`.
- [x] **Pestañas Protegidas (Contenedores Aislados de Sesión):** Aislamiento estricto de sesiones y cookies por pestaña mediante `GeckoSessionSettings.Builder.contextId()`, persistencia con esquema Room v3 (`isProtected`, `contextId`), selector tripartito en `TabsScreen` y purga automática de cookies al cerrarse con `runtime.storageController.clearDataForSessionContext(contextId)`.
- [x] **Infraestructura Nativa Rust/C++ (Scaffolding):** Configuración de CMake 3.31.6 (`app/src/main/cpp/CMakeLists.txt`), compilación de `libbrowser_native.so` con C++26, módulo base en Rust Edition 2024 (`core-native/Cargo.toml`, `src/lib.rs`), puente JNI en Kotlin (`NativeBridge.kt`) y soporte garantizado para 32 y 64 bits (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
- [x] **Auditor y Administrador de Cookies de Navegación:** Nueva pantalla dedicada (`CookiesScreen`) con filtrado por dominio, categorización y detección de rastreadores publicitarios/telemetría, visualización de caducidad y atributos de seguridad (`Secure`, `HttpOnly`), y purga sincronizada con Room v4 y `GeckoRuntime.storageController`.
- [x] **Estabilización Visual con Escala de Fuente Fija:** Inmunidad contra configuraciones de accesibilidad o tamaño de fuente global gigante de Android mediante `CompositionLocalProvider` (`fontScale = 1.0f`) y padding de navegación inferior para dispositivos móviles.
- [x] **Miniaturas Visuales de Pestañas en Vivo:** Captura de pantalla dinámica en alta fidelidad mediante `GeckoDisplay.capturePixels()` almacenada en memoria y mostrada en las tarjetas de la cuadrícula de pestañas.
- [x] **Suspensión / Hibernación Inteligente (5 minutos):** Monitor no invasivo que detecta pestañas inactivas durante 5 minutos para suspender la sesión GeckoView y liberar la memoria RAM del teléfono sin perder la URL, título ni miniatura.
- [x] **Modo Incógnito y Creación Manual de Pestañas:** Transición y estandarización del término "Incógnito" en toda la interfaz, eliminando la creación automática accidental de pestañas en los apartados de Incógnito y Protegidas.
- [x] **Blindaje Integral del Modo Incógnito (No Genérico):**
  - **Protección contra Huella Digital (RFP - Resist Fingerprinting):** Simulación unificada de Canvas, WebGL, Audio y User-Agent genérico Tor/ESR para frustrar perfilados biométricos y de hardware del teléfono.
  - **Bloqueo de Fugas WebRTC (Anti IP-Leak):** Desactivación completa de `media.peerconnection` y aislamiento STUN para salvaguardar las IPs reales.
  - **DNS sobre HTTPS Cifrado (DoH):** Protocolo TRR activado con Mozilla/Cloudflare para evitar monitoreo del proveedor de telefonía móvil (ISP).
  - **Total Cookie Protection (dFPI):** Aislamiento estricto de cookies por sitio web impidiendo el seguimiento entre dominios distintos.
  - **Purga Inmediata de Memoria RAM al Cerrar:** Destrucción y limpieza forzada de cachés volátiles (`ALL_CACHES`, `AUTH_SESSIONS`), eliminación de miniaturas y recolección de basura con `System.gc()`.
  - **Protección de Pantalla FLAG_SECURE:** Bloqueo de capturas de pantalla y ofuscación en la multitarea de Android durante la navegación anónima.
- [x] **Flujo de Integración Continua (CI) y Artefactos Divididos por ABI:** Workflow en GitHub Actions (`.github/workflows/build-debug.yml`) con activación exclusivamente manual (`workflow_dispatch`), compilación limpia sin caché (NDK 28, CMake 3.31, Rust 2024, GeckoView Omni), generación forzada de firma con `generate_debug_keystore.sh`, división de binarios por arquitectura (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`) y publicación como artefactos independientes descargables desde GitHub.
- [x] **Gestión de Cuentas e Identidad Web Integrada (Google One-Tap):** Vinculación nativa de cuentas de Google y credenciales federadas con AndroidX `CredentialManager`, persistencia local en Room (`UserAccountEntity`, `UserAccountDao`), detección contextual de páginas de autenticación en vivo con `WebSignInBridge`, banner superior interactivo en Compose (`WebSignInPromptBanner`), autocompletado e inyección en GeckoView y pantalla dedicada multicuenta (`AccountsScreen`).
- [x] **Gestor de Permisos por Sitio Web y Políticas de Bloqueo Silencioso ("No Preguntar"):** Control granular de accesos web (cámara, micrófono, geolocalización, notificaciones de escritorio y almacenamiento persistente) persistido en Room v8 (`SitePermissionEntity`, `SitePermissionDao`), interceptación y resolución en `GeckoSession.PermissionDelegate`, diálogos nativos interactivos con opción de recordar, pantalla dedicada (`SitePermissionsScreen`) con búsqueda y filtros, y directivas globales para silenciar solicitudes de permisos sin mostrar popups.
- [x] **Sistema de Efectos de Sonido Nativos y Procesamiento de Audio:** Integración de `SoundPool` con cero latencia (`SoundEffectManager`) con 6 variantes de tonos de logro reproducidos de forma aleatoria al finalizar descargas exitosamente, retroalimentación háptica coordinada, control on/off en Ajustes, y script multipropósito `tools/audio_processor.sh` para conversión de formatos (WAV, MP3, OGG), recorte sin chasquidos y división automática por detección de silencios.
- [x] **Soporte Completo de WebExtensions (Mozilla Add-ons) Bajo Demanda:**
  - Descarga e instalación directa desde servidores oficiales de Mozilla (`addons.mozilla.org`) sin empaquetar binarios de terceros dentro del APK para preservar la privacidad y la licencia cerrada del código base.
  - Catálogo de extensiones recomendadas: uBlock Origin (bloqueo de publicidad), Dark Reader (modo oscuro universal), TWP (traducción de páginas) y ClearURLs (desinfección de rastreadores en enlaces).
  - Ciclo de vida completo (`ExtensionManager`): instalación `.xpi`, activación/desactivación dinámica, desinstalación y monitor de progreso de descarga.
  - Pantalla dedicada de gestión (`ExtensionsScreen`) con tarjetas visuales e instalación desde URLs directas.
- [x] **Flujo de Bienvenida y Configuración Inicial (Onboarding):** Pantalla inicial dedicada (`OnboardingScreen`) previa al acceso al navegador para elegir el motor de búsqueda predeterminado (DuckDuckGo, Google, Bing, Brave, Ecosia) y seleccionar las extensiones recomendadas para descargar de forma desatendida.
- [x] **Blindaje y Endurecimiento de Seguridad (Security Hardening):**
  - Cifrado Fail-Closed para credenciales con fallback en memoria.
  - Protección estricta contra Path Traversal en descargas (`canonicalPath`).
  - Restricción estricta de tráfico en texto claro con `network_security_config.xml` (`cleartextTrafficPermitted="false"` en producción).
  - FileProvider privado y aislado con directorio `share/`.

---

### 🔵 Fase 3: Capacidades Avanzadas de Navegación, Privacidad y Lógica Rust (En Progreso)
- [x] **Motor de Filtrado Nativo en Rust (`core-native`):** Integración del crate de alto rendimiento `adblock` (Mozilla/Brave compatible) en Rust Edition 2024 conectado vía JNI (`NativeBridge.kt`). Soporta reglas de sintaxis Adblock Plus / EasyList y listas Hosts, métricas de bloqueo en tiempo real, actualización de listas remotas y reglas manuales con panel de control dedicado en Ajustes.
- [x] **Protección Anti-Phishing y Malware Web en Tiempo Real (Rust Multimotor):**
  - Intercepción preventiva a microsegundos de cada solicitud web antes del renderizado en GeckoView mediante el motor Rust.
  - Sincronización e integración de inteligencia de amenazas mundial: **URLhaus (Abuse.ch)** para malware y troyanos, **PhishTank & OpenPhish** para estafas bancarias y suplantación de identidad, **HaGeZi Threat Intelligence (TIF)** para botnets y C2, y **StevenBlack Security Hosts**.
  - Pantalla dedicada de advertencia crítica (`ThreatBlockedScreen`) con opciones seguras de retroceso o bypass consciente.
  - Pantalla dedicada de gestión y sincronización en caliente (`SecurityThreatScreen`) con métricas acumulativas de amenazas neutralizadas.
  - **Sincronización Esporádica en Segundo Plano con WorkManager y Notificaciones Transparentes:** Tarea periódica desatendida (`ThreatShieldUpdateWorker`) que descarga listas de estafas bancarias y malware respetando batería (`batteryNotLow`) y datos móviles (filtro Wi-Fi opcional), informando al usuario mediante una notificación nativa discreta (`IMPORTANCE_LOW`) sin secretos.
- [x] **Modo Lectura Nativo (Reader Mode):** Extracción heurística de artículos web con `jsoup`, limpieza de elementos ruidosos (anuncios, cookies, menús), personalización tipográfica (Serif, Sans, Monospace, Sepia, Noche, Papel, OLED, espaciado e interlineado) y sintetizador de voz (TTS - Text to Speech) con lectura continua por bloques y selector de velocidad.
- [ ] **Expansión de Efectos de Sonido y Personalización (Próximamente):**
  - Incorporación de nuevos bancos de efectos de sonido para añadir a marcadores ("pop"), cierre de pestañas ("whoosh"), vaciado de datos del modo incógnito y alertas de descargas bloqueadas.
  - Selector en Ajustes para permitir al usuario elegir sus sonidos favoritos o importar archivos de audio personalizados desde el almacenamiento del dispositivo.
- [ ] **Expansión del Sistema de Cuentas:** Sincronización local/cifrada opcional de marcadores y preferencias vinculadas al perfil activo, y gestor integrado de contraseñas web.
- [ ] **Expansión Continua de Seguridad en Modo Incógnito:**
  - Incorporación de muchas más funciones de seguridad para que el modo incógnito no sea genérico: bloqueo heurístico de telemetría oculta en scripts, sandbox estricto de APIs de sensores (giroscopio, acelerómetro, batería), y rotación dinámica de identidades virtuales.
  - Generación de informe de rastreo en tiempo real para verificar qué elementos intentaron perfilar al usuario y fueron neutralizados.
- [ ] **Protección contra Rastreo Mejorada (ETP):** Bloqueo nativo de rastreadores de terceros y cookies de seguimiento mediante GeckoView.

---

### 🟣 Fase 4: Rendimiento, Exportación y Distribución
- [ ] **Optimizaciones de Memoria en Segundo Plano:** Hibernación de pestañas no visibles para evitar consumo excesivo de RAM.
- [ ] **Generación de APKs optimizados por arquitectura:** Configuración de splits ABI (`arm64-v8a`, `armeabi-v7a`) para usuarios que descarguen desde Uptodown y deseen menor peso por instalación.
- [ ] **Copia de Seguridad y Restauración Local:** Exportación e importación de marcadores e historial en formatos abiertos (HTML/JSON).
