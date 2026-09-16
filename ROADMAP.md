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
- [x] **Flujo de Integración Continua (CI) y Entrega Directa P2P:** Workflow en GitHub Actions (`.github/workflows/build-debug.yml`) con activación exclusivamente manual (`workflow_dispatch`), compilación limpia sin caché (NDK 28, CMake 3.31, Rust 2024, GeckoView Omni), generación forzada de firma con `generate_debug_keystore.sh` y sincronización directa P2P desatendida mediante Syncthing a la carpeta `/storage/emulated/0/Navegador/app-debug.apk` del móvil.
- [x] **Gestión de Cuentas e Identidad Web Integrada (Google One-Tap):** Vinculación nativa de cuentas de Google y credenciales federadas con AndroidX `CredentialManager`, persistencia local en Room (`UserAccountEntity`, `UserAccountDao`), detección contextual de páginas de autenticación en vivo con `WebSignInBridge`, banner superior interactivo en Compose (`WebSignInPromptBanner`), autocompletado e inyección en GeckoView y pantalla dedicada multicuenta (`AccountsScreen`).

---

### 🔵 Fase 3: Capacidades Avanzadas de Navegación, Privacidad y Lógica Rust (Siguiente)
- [ ] **Expansión del Sistema de Cuentas:** Sincronización local/cifrada opcional de marcadores y preferencias vinculadas al perfil activo, y gestor integrado de contraseñas web.
- [ ] **Expansión Continua de Seguridad en Modo Incógnito:**
  - Incorporación de muchas más funciones de seguridad para que el modo incógnito no sea genérico: bloqueo heurístico de telemetría oculta en scripts, sandbox estricto de APIs de sensores (giroscopio, acelerómetro, batería), y rotación dinámica de identidades virtuales.
  - Generación de informe de rastreo en tiempo real para verificar qué elementos intentaron perfilar al usuario y fueron neutralizados.
- [ ] **Lógica de Seguridad en Rust (`core-native`):** Integración de filtros de bloqueo de publicidad y listas de rastreo procesadas en el crate Rust.
- [ ] **Soporte de WebExtensions:** Integración de extensiones de Mozilla (bloqueadores de publicidad como uBlock Origin, gestores de scripts).
- [ ] **Lector de Modo Lectura:** Extracción del contenido principal de artículos para lectura limpia sin anuncios ni estilos intrusivos.
- [ ] **Protección contra Rastreo Mejorada (ETP):** Bloqueo nativo de rastreadores de terceros y cookies de seguimiento mediante GeckoView.

---

### 🟣 Fase 4: Rendimiento, Exportación y Distribución
- [ ] **Optimizaciones de Memoria en Segundo Plano:** Hibernación de pestañas no visibles para evitar consumo excesivo de RAM.
- [ ] **Generación de APKs optimizados por arquitectura:** Configuración de splits ABI (`arm64-v8a`, `armeabi-v7a`) para usuarios que descarguen desde Uptodown y deseen menor peso por instalación.
- [ ] **Copia de Seguridad y Restauración Local:** Exportación e importación de marcadores e historial en formatos abiertos (HTML/JSON).
