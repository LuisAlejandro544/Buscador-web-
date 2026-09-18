# Estructura del Proyecto y Arquitectura Técnica

Este documento describe la organización de módulos, paquetes, flujo de datos y patrones arquitectónicos adoptados en el desarrollo de la aplicación.

---

## 🏛️ Principios de Diseño y Arquitectura

1. **Separación de Responsabilidades (SoC):** Cada paquete y clase cumple un rol único y delimitado. La lógica de presentación está separada del acceso a datos y de la implementación concreta del motor web.
2. **Modularidad Estricta y Archivos < 500 Líneas:** Para prevenir fallos por complejidad monolítica, el proyecto aplica modularización integral:
   - **Patrón Delegado en ViewModel:** `BrowserViewModel` orquesta delegados especializados de dominio (`CookieDelegate`, `ExtensionDelegate`, `DownloadDelegate`, `AccountDelegate`, `SitePermissionDelegate`).
   - **Subcomponentes de UI Atómicos:** Las pantallas principales se descomponen en componentes especializados reutilizables, independientes y probables.
3. **Pantallas Dedicadas e Interconexión Intuitiva:** Siguiendo las directrices del proyecto, la aplicación evita concentrar todas las funciones en una sola vista monolítica o recurrir a minimalismo extremo. Cada funcionalidad relevante cuenta con su propia pantalla dedicada (Navegador, Gestor de Pestañas, Marcadores, Historial, Descargas, Extensiones, Cookies, Cuentas, Permisos de Sitio y Ajustes) con botones y barras de navegación claras para moverse entre ellas.
4. **Flujo de Datos Unidireccional (UDF):** El estado de la interfaz se modela mediante `StateFlow` inmutables emitidos por los ViewModels y consumidos reactivamente por los componentes de Jetpack Compose.
5. **Desacoplamiento del Motor Web:** A través de la interfaz `BrowserEngineContract`, la capa de interfaz no conoce detalles internos específicos de bajo nivel del motor de renderizado (Mozilla GeckoView en C++/Rust con fallbacks nativos).

---

## 📁 Árbol de Paquetes y Archivos Clave

```text
app/src/main/
├── cpp/
│   ├── CMakeLists.txt               # Configuración de compilación CMake 3.31 para C++26 / C23 y enlace con NDK
│   └── native-bridge.cpp            # Puente JNI y funciones nativas exportadas (libbrowser_native.so)
├── java/com/example/
│   ├── browser/
│   │   ├── account/
│   │   │   ├── AccountCredentialManager.kt # Gestor de credenciales nativas AndroidX y Google ID
│   │   │   └── WebSignInBridge.kt          # Detección de páginas de autenticación y generación de JS de acceso
│   │   ├── debug/
│   │   │   ├── CrashLogManager.kt          # Gestor de captura y persistencia de excepciones críticas no controladas
│   │   │   ├── CrashReportModel.kt         # Modelo forense con especificaciones de hardware, memoria y traza de error
│   │   │   └── PrivacyAuditEngine.kt       # Motor de pruebas forenses de privacidad, fugas de BD, red, memoria e ISP
│   │   ├── download/
│   │   │   ├── DownloadEngine.kt           # Motor autónomo de descargas concurrentes en streaming con pausas y reanudaciones
│   │   │   ├── DownloadForegroundService.kt # Servicio en primer plano para transferencias en segundo plano
│   │   │   ├── DownloadNotificationHelper.kt # Gestor de notificaciones nativas de progreso y finalización
│   │   │   ├── DownloadProgressState.kt    # Modelo de estado reactivo de transferencias
│   │   │   └── DownloadManagerHelper.kt    # Integración legacy con el servicio del sistema
│   │   ├── extension/
│   │   │   ├── ExtensionManager.kt         # Gestor integral de WebExtensions sobre GeckoView (descarga AMO, instalación, ciclo de vida)
│   │   │   ├── RecommendedExtension.kt     # Catálogo oficial curado (uBlock Origin, Dark Reader, TWP, ClearURLs)
│   │   │   └── WebExtensionModel.kt        # Modelo de estado para la interfaz en Compose
│   │   ├── sound/
│   │   │   └── SoundEffectManager.kt       # Gestor de efectos sonoros y háptica de baja latencia con SoundPool
│   │   ├── security/
│   │   │   ├── ThreatShieldNotificationHelper.kt # Gestor de notificaciones transparentes y no abrumadoras (IMPORTANCE_LOW)
│   │   │   ├── ThreatShieldUpdateWorker.kt       # Tarea en segundo plano CoroutineWorker para descarga e inyección en Rust
│   │   │   └── ThreatShieldUpdateScheduler.kt    # Planificador WorkManager periódico (24h) con restricciones de red y batería
│   │   ├── engine/
│   │   │   ├── BrowserEngineContract.kt # Interfaz abstracta que define las operaciones de navegación web
│   │   │   ├── GeckoPromptHandler.kt    # Delegado GeckoView PromptDelegate para alertas, confirmaciones y ficheros
│   │   │   ├── GeckoRuntimeProvider.kt  # Singleton de GeckoRuntime con optimización de memoria y ETP
│   │   │   ├── GeckoSessionManager.kt   # Gestor concurrente de sesiones GeckoSession, aislamiento contextId, hibernación y purga de datos
│   │   │   ├── GeckoViewEngine.kt       # Implementación completa de BrowserEngineContract sobre GeckoView con captura de miniaturas
│   │   │   ├── NativeBridge.kt          # Puente Kotlin-JNI seguro para el subsistema C++26 y Rust
│   │   │   └── WebPrompt.kt             # Modelos de eventos para diálogos nativos en Compose
│   │   └── thumbnail/
│   │       └── TabThumbnailManager.kt   # Gestor en memoria y caché de miniaturas capturadas de cada pestaña
core-native/                         # Módulo de alto rendimiento en Rust (Edition 2024)
├── Cargo.toml                       # Manifiesto de dependencias y configuración staticlib/cdylib
└── src/
    └── lib.rs                       # Funciones nativas de alto rendimiento (hashing, validación de URLs)
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── BookmarkDao.kt       # Acceso a marcadores guardados
│   │   │   ├── CookieDao.kt         # Acceso, filtrado por sitio y purga de cookies y rastreadores
│   │   │   ├── DownloadDao.kt       # Acceso y control del registro de descargas
│   │   │   ├── HistoryDao.kt        # Acceso al historial cronológico de navegación
│   │   │   ├── ShortcutDao.kt       # Acceso y CRUD de accesos directos configurables por el usuario
│   │   │   ├── SitePermissionDao.kt # Acceso y control de permisos otorgados o bloqueados por dominio web
│   │   │   ├── TabDao.kt            # Acceso y persistencia de pestañas abiertas (normales y protegidas)
│   │   │   └── UserAccountDao.kt    # Acceso, conmutación y persistencia de cuentas de usuario
│   │   ├── entity/
│   │   │   ├── BookmarkEntity.kt    # Modelo relacional para marcadores
│   │   │   ├── CookieEntity.kt      # Modelo relacional para cookies (dominio, valor, expiración, isTracker)
│   │   │   ├── DownloadEntity.kt    # Modelo relacional para descargas (estado, bytes, URI)
│   │   │   ├── HistoryEntity.kt     # Modelo relacional para historial
│   │   │   ├── ShortcutEntity.kt    # Modelo relacional para accesos directos configurables (título, URL, icono, color)
│   │   │   ├── SitePermissionEntity.kt # Modelo relacional para permisos web (origen, tipo, estado y actualización)
│   │   │   ├── TabEntity.kt         # Modelo relacional para pestañas (incluye flags isIncognito, isProtected y contextId)
│   │   │   └── UserAccountEntity.kt # Modelo relacional para cuentas vinculadas (email, nombre, activo)
│   │   └── BrowserDatabase.kt       # Base de datos Room con control de versiones y migraciones
│   ├── model/
│   │   ├── BrowserTab.kt            # Modelo de dominio para pestañas
│   │   └── SearchEngine.kt          # Proveedores de búsqueda (DuckDuckGo, Google, Bing, etc.)
│   ├── preferences/
│   │   └── BrowserPreferences.kt    # Persistencia de preferencias del usuario mediante DataStore
│   └── repository/
│       └── BrowserRepository.kt     # Repositorio unificado que conecta DAOs, DataStore y ViewModel
├── ui/
│   ├── account/
│   │   └── AccountsScreen.kt        # Pantalla completa de gestión de cuentas vinculadas y conmutación
│   ├── bookmarks/
│   │   └── BookmarksScreen.kt       # Pantalla completa de marcadores con búsqueda y CRUD
│   ├── browser/
│   │   ├── BrowserScreen.kt         # Orquestador principal de navegación (GeckoView + Diálogos + StartPage)
│   │   ├── OmniboxField.kt          # Barra de direcciones inteligente (cifrado HTTPS, atajos, búsqueda)
│   │   ├── BrowserActionMenu.kt     # Menú desplegable contextual (pestañas, modo escritorio, descargas, etc.)
│   │   ├── BrowserBottomBar.kt      # Barra de herramientas inferior con historial, inicio y gestor de pestañas
│   │   ├── BrowserStartPage.kt      # Página de inicio rápida con marcadores, historial y accesos directos
│   │   └── EditShortcutDialog.kt    # Diálogo de creación, edición y personalización de accesos directos (Speed Dial)
│   ├── components/
│   │   ├── WebPromptDialog.kt       # Diálogos nativos Material 3 para alerts, confirms, prompts, permisos y ficheros
│   │   └── WebSignInPromptBanner.kt # Banner flotante interactivo de acceso web con un solo toque
│   ├── cookies/
│   │   ├── CookiesScreen.kt         # Orquestador de auditoría de cookies y rastreadores
│   │   ├── CookieMetricsHeader.kt   # Indicadores visuales de métricas (total, rastreadores, protegidas)
│   │   ├── CookieDetailCard.kt      # Ficha detallada individual de cookie con metadatos técnicos y borrado
│   │   └── DomainCookieGroupCard.kt # Tarjeta colapsable agrupada por dominio con purga en lote
│   ├── debug/
│   │   ├── CrashInspectorActivity.kt # Activity independiente para inspección forense de fallos y stack traces
│   │   └── PrivacyAuditActivity.kt  # Activity independiente para auditoría forense de fugas en modo incógnito
│   ├── downloads/
│   │   ├── DownloadsScreen.kt       # Pantalla de descargas (activas en tiempo real e historial archivado)
│   │   ├── ActiveDownloadCard.kt    # Tarjeta de descarga activa con velocidad, progreso y controles
│   │   ├── DownloadItemCard.kt      # Tarjeta del historial de archivos completados
│   │   └── DownloadFileIconHelper.kt # Categorización visual por extensión y formato de tamaño/velocidad
│   ├── extension/
│   │   ├── ExtensionsScreen.kt      # Orquestador de gestión de complementos WebExtensions
│   │   ├── ExtensionLogo.kt         # Renderizado de isotipos oficiales con fallback temático dinámico
│   │   ├── InstalledExtensionsTab.kt # Pestaña de extensiones instaladas con switches y desinstalación
│   │   ├── RecommendedCatalogTab.kt # Catálogo oficial AMO (uBlock, Dark Reader, TWP, ClearURLs)
│   │   └── CustomUrlInstallerTab.kt # Instalador universal de paquetes .xpi por URL directa
│   ├── history/
│   │   └── HistoryScreen.kt         # Pantalla completa de historial con búsqueda y eliminación
│   ├── navigation/
│   │   ├── BrowserNavGraph.kt       # Grafo y rutas de navegación con Jetpack Navigation Compose
│   │   └── Screen.kt                # Definición de pantallas y rutas fuertemente tipadas
│   ├── onboarding/
│   │   └── OnboardingScreen.kt      # Pantalla inicial de bienvenida (selección de buscador y extensiones)
│   ├── permissions/
│   │   └── SitePermissionsScreen.kt # Pantalla dedicada para gestión de permisos por sitio y políticas "No Preguntar"
│   ├── reader/
│   │   └── ReaderScreen.kt          # Pantalla de modo lectura con extracción heurística y síntesis de voz (TTS)
│   ├── security/
│   │   ├── ThreatBlockedScreen.kt   # Pantalla de advertencia crítica por detección de phishing o malware
│   │   └── SecurityThreatScreen.kt  # Pantalla de control, estadísticas y sincronización de motores de ciberseguridad
│   ├── settings/
│   │   ├── SettingsScreen.kt        # Orquestador de ajustes y configuración del navegador
│   │   ├── SettingsGeneralSection.kt # Motor de búsqueda, página de inicio y modo escritorio
│   │   ├── SettingsShortcutsSection.kt # Accesos directos a Cookies, Cuentas, Permisos y Extensiones
│   │   ├── SettingsPrivacySection.kt # Políticas JavaScript, cookies, Do Not Track y bloqueo silencioso
│   │   ├── SettingsSoundSection.kt  # Efectos sonoros y prueba interactiva
│   │   └── SettingsArchitectureSection.kt # Diagnóstico nativo del motor GeckoView y ABIs (32/64 bits)
│   ├── tabs/
│   │   ├── TabCardItem.kt           # Tarjeta individual con renderizado de miniatura, distintivo de reposo y cierre
│   │   └── TabsScreen.kt            # Pantalla en cuadrícula para gestionar pestañas normales, protegidas e incógnito
│   └── theme/
│       ├── Color.kt                 # Paleta de colores M3
│       ├── Theme.kt                 # Configuración de MaterialTheme con soporte de modo oscuro/claro
│       └── Type.kt                  # Configuración tipográfica
├── viewmodel/
│   ├── BrowserViewModel.kt          # Orquestador del ViewModel (coordinación reactiva < 420 líneas)
│   └── delegates/
│       ├── CookieDelegate.kt        # Delegado de auditoría, categorización y purga de cookies
│       ├── ExtensionDelegate.kt     # Delegado de descarga, instalación y ciclo de vida de WebExtensions
│       ├── DownloadDelegate.kt      # Delegado del motor de descargas en streaming y persistencia
│       ├── AccountDelegate.kt       # Delegado de gestión de credenciales y perfiles de usuario
│       ├── SitePermissionDelegate.kt # Delegado de permisos por dominio web y políticas de bloqueo
│       ├── ReaderDelegate.kt        # Delegado de extracción de artículos y motor TTS
│       ├── FilterDelegate.kt        # Delegado del motor adblock nativo en Rust (core-native)
│       └── ThreatProtectionDelegate.kt # Delegado del escudo de seguridad, anti-phishing y malware multimotor
├── MainActivity.kt                  # Activity principal con configuración Edge-to-Edge y contenedor Compose
├── .github/
│   └── workflows/
│       └── build-debug.yml          # Workflow CI de GitHub Actions: compilación manual sin caché y artefactos divididos por ABI
└── generate_debug_keystore.sh       # Script de generación obligatoria y desatendida de debug.keystore (PKCS12)
```

---

## 🔄 Flujo de Interacción y Delegados

```text
[Usuario / Interfaz Compose]
          │
          ▼ Dispara eventos (Navegar URL, Instalar Extensión, Descargar archivo, Gestionar Cookies)
[BrowserViewModel] (Orquestador Central)
          │
     ┌────┼──────────────────────────┬──────────────────────────┐
     ▼    ▼                          ▼                          ▼
[CookieDelegate]             [ExtensionDelegate]        [DownloadDelegate]
(Purga, Rastreadores,         (Instalación AMO,          (Streaming, Pausa,
 Sincronización Gecko)        Gestión en vivo)           Reanudación, Notif)
     │    │                          │                          │
     ▼    ▼                          ▼                          ▼
[AccountDelegate]            [SitePermissionDelegate]   [BrowserEngineContract]
(Credenciales, One-Tap)      (Permisos por Dominio)     (GeckoView C++/Rust)
          │
          └──────────────────────────┬──────────────────────────┘
                                     ▼
                        Emisión de StateFlow reactivos
                                     │
                                     ▼ Recomposición limpia
                        [Pantallas de Jetpack Compose]
```

---

## 🛡️ Modularidad y Extensibilidad

- **Arquitectura de Delegados en ViewModel:**
  - Originalmente, `BrowserViewModel` concentraba más de 1,380 líneas asumiendo responsabilidades dispares.
  - Se desacopló en 5 delegados específicos (`CookieDelegate`, `ExtensionDelegate`, `DownloadDelegate`, `AccountDelegate`, `SitePermissionDelegate`) reduciendo el ViewModel principal a ~400 líneas, facilitando el mantenimiento, pruebas unitarias locales con Robolectric y previniendo colapsos de memoria.
- **Modularización de Pantallas de UI:**
  - `BrowserScreen.kt`: Reducido de 679 a ~280 líneas delegando en `OmniboxField`, `BrowserActionMenu` y `BrowserBottomBar`.
  - `CookiesScreen.kt`: Reducido de 894 a ~270 líneas delegando en `CookieMetricsHeader`, `CookieDetailCard` y `DomainCookieGroupCard`.
  - `ExtensionsScreen.kt`: Reducido de 687 a ~180 líneas delegando en `ExtensionLogo`, `InstalledExtensionsTab`, `RecommendedCatalogTab` y `CustomUrlInstallerTab`.
  - `DownloadsScreen.kt`: Reducido de 663 a ~200 líneas delegando en `ActiveDownloadCard`, `DownloadItemCard` y `DownloadFileIconHelper`.
  - `SettingsScreen.kt`: Reducido de 632 a ~180 líneas delegando en `SettingsGeneralSection`, `SettingsShortcutsSection`, `SettingsPrivacySection`, `SettingsSoundSection` y `SettingsArchitectureSection`.
- **Aislamiento por Pestañas Protegidas (Context Containers):** Cada pestaña protegida opera con su propio `contextId` inyectado en `GeckoSessionSettings.Builder`. Esto crea una partición estricta de cookies, caché web y `localStorage`. Al eliminarse la pestaña, `GeckoSessionManager` destruye la sesión y limpia el contexto en el motor invocando `runtime.storageController.clearDataForSessionContext(contextId)`.
- **Soporte GeckoView:** La infraestructura de Gradle importa `geckoview-omni` e incluye soporte nativo legacy para empaquetado de librerías ELF `.so` (`libxul.so`, etc.), permitiendo instanciar `GeckoRuntime` y `GeckoSession` implementando el contrato `BrowserEngineContract`.
- **Inmunidad de Tipografía Móvil (`fontScale = 1.0f`):** En `Theme.kt`, se inyecta una instancia personalizada de `Density` mediante `CompositionLocalProvider(LocalDensity provides ...)`. Esto desvincula la interfaz del multiplicador de fuente global del sistema operativo del dispositivo, asegurando proporciones estables en cualquier pantalla de teléfono sin importar el tamaño de letra configurado en Android.
- **Miniaturas Gráficas y Suspensión Inteligente (5 min):** `TabThumbnailManager` almacena capturas escaladas de las páginas visitadas mediante `GeckoDisplay.capturePixels()`. Paralelamente, `BrowserViewModel` evalúa el tiempo de inactividad de las pestañas cada 15 segundos; si una pestaña supera los 5 minutos sin foco, invoca `GeckoSessionManager.hibernateSession()` cerrando el proceso de GeckoView y liberando memoria RAM del teléfono. La miniatura y el estado visual permanecen intactos en la cuadrícula indicando "En reposo (5 min)"; al tocar la pestaña, la sesión se reactiva automáticamente.
- **Blindaje Avanzado del Modo Incógnito (No Genérico):**
  - **RFP (Resist Fingerprinting):** Configuración activa de preferencias estilo Tor (`privacy.resistFingerprinting = true`) enmascarando métricas de pantalla, canvas, APIs de audio y User-Agent genérico Tor/ESR para frustrar el fingerprinting biométrico y del dispositivo móvil.
  - **Anti IP-Leak (WebRTC Bloqueado):** Desactivación estricta de `media.peerconnection` y aislamiento STUN, junto a un `permissionDelegate` en `GeckoViewEngine` que rechaza solicitudes de cámara y micrófono en incógnito para impedir fugas de direcciones IP privadas y públicas.
  - **DNS sobre HTTPS Cifrado (DoH):** Forzado de resolución cifrada con Cloudflare/Mozilla (`TRR_MODE_FIRST`), imposibilitando el espionaje o filtrado de tráfico por parte de proveedores de internet y operadoras móviles.
  - **Total Cookie Protection (dFPI):** Confinamiento de cookies y almacenamiento al dominio de primer nivel (`ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS`, `privacy.partition.network_state = true`), previniendo el rastreo cruzado entre sitios.
  - **Purga Inmediata de RAM:** Al cerrar cualquier pestaña de incógnito o salir del modo, `GeckoSessionManager` y `GeckoRuntimeProvider` ejecutan `storageController.clearData(ALL_CACHES or AUTH_SESSIONS)`, destruyen miniaturas y disparan `System.gc()`.
  - **Protección Visual FLAG_SECURE:** Bloqueo de capturas de pantalla y ocultación visual en la multitarea de Android al navegar en incógnito.
- **Capa Nativa Híbrida (C++26 / Rust 2024):** Preparada mediante NDK r28 y CMake 3.31+ para vincular librerías `.so` de alto rendimiento. Rust asume la lógica pesada de seguridad (bloqueo de anuncios, hashes criptográficos, protección de rastreo) y C++ proporciona aceleración por hardware y enlace con APIs nativas del sistema. Los artefactos temporales de compilación de CMake y Cargo quedan completamente aislados por `.gitignore`.
- **Ecosistema de Extensiones Web (WebExtensions) Bajo Demanda y Prevención de Licencias Víricas:**
  - **Descarga Oficial Directa desde Mozilla Add-ons (AMO):** En estricto cumplimiento de la política de código cerrado y protección legal de propiedad intelectual, el proyecto **NO** empaqueta binarios `.xpi` de extensiones bajo licencias copyleft o víricas (como GPLv3 en uBlock Origin) dentro del APK o carpeta `assets`. En su lugar, el navegador actúa como un agente de usuario neutral que se conecta directamente a los servidores oficiales de Mozilla (`https://addons.mozilla.org/firefox/downloads/latest/...`) bajo petición explícita y consentimiento del usuario.
  - **Ciclo de Vida y Gestor Desacoplado (`ExtensionManager`):**
    - Se apoya en `GeckoRuntime.webExtensionController` para la descarga, verificación de permisos e instalación dinámica en tiempo de ejecución.
    - Soporta la activación (`enable()`), desactivación (`disable()`) y desinstalación (`uninstall()`) en caliente sin requerir reinicio del proceso de GeckoView.
    - Emite flujos reactivos `StateFlow` con el progreso porcentual de descarga por extensión y la lista actualizada de complementos para consumo en Jetpack Compose.
- **Canal de Despliegue y Artefactos por Arquitectura (GitHub Actions CI):** El flujo en `.github/workflows/build-debug.yml` implementa compilación continua sin intermediarios: compila los APKs de depuración de forma limpia (sin cachés), genera una firma `debug.keystore` fresca, genera binarios divididos por arquitectura (`splits.abi`) y los publica como artefactos independientes en GitHub (`app-debug-arm64-v8a`, `app-debug-armeabi-v7a`, `app-debug-x86_64`, `app-debug-x86`) para permitir descargas optimizadas directamente al móvil sin pesos universales redundantes.
- **Gestión de Cuentas e Identidad Web Integrada (`AccountCredentialManager` y `WebSignInBridge`):**
  - **Vinculación Nativa con `androidx.credentials`:** Utiliza `CredentialManager` y `GetGoogleIdOption` para enlazar cuentas de Google o credenciales personalizadas en el dispositivo móvil sin depender de servicios propietarios inflexibles.
  - **Almacenamiento Reactivo con Room v4 (`UserAccountEntity` / `UserAccountDao`):** Persistencia local de perfiles con soporte multicuenta, avatar, correo y bandera de cuenta activa (`isActive`), permitiendo conmutar la identidad principal en cualquier momento.
  - **Detección Dinámica de Autenticación (`WebSignInBridge`):** Analiza en tiempo real las URLs cargadas en el motor web para identificar portales de acceso, protocolos OAuth2, OpenID Connect y botones de inicio de sesión de Google.
  - **Banner Interactivo One-Tap en Jetpack Compose (`WebSignInPromptBanner`):** Despliega un componente animado en la parte superior del navegador con la identidad activa del usuario cuando se visita un sitio web compatible.
  - **Inyección y Autocompletado en GeckoView:** Al presionar "Continuar", el navegador evalúa JavaScript en el contexto de la página para rellenar campos de correo/usuario o activar selectores de inicio de sesión de Google automáticamente.
- **Sincronización en Segundo Plano y Notificaciones Transparentes (`ThreatShieldUpdateScheduler`, `ThreatShieldUpdateWorker` y `ThreatShieldNotificationHelper`):**
  - **Planificación Desatendida con Jetpack WorkManager:** `ThreatShieldUpdateScheduler` encola una tarea periódica de 24 horas (`PeriodicWorkRequestBuilder`) asociada a restricciones de hardware estrictas: dispositivo con batería no baja (`setRequiresBatteryNotLow(true)`) y tipo de red configurable (cualquiera o exclusivamente Wi-Fi vía `NetworkType.UNMETERED`).
  - **Descarga e Inyección en Tiempo de Ejecución:** `ThreatShieldUpdateWorker` consulta los repositorios mundiales de estafas y malware (PhishTank, OpenPhish, URLhaus, HaGeZi, StevenBlack) e inyecta las nuevas reglas en microsegundos dentro del motor de filtrado compilado en Rust invocando `NativeBridge.addFilterRules()`.
  - **Transparencia Total sin Fricción:** `ThreatShieldNotificationHelper` crea un canal de notificaciones dedicado de importancia baja (`IMPORTANCE_LOW`). La notificación informa al usuario exactamente cuántas firmas nuevas se añadieron y cuántos motores se sincronizaron, sin emitir sonidos molestos ni vibraciones que interrumpan la actividad del usuario en el teléfono.
- **Arnés de Auditoría Forense de Privacidad (`PrivacyAuditActivity` y `PrivacyAuditEngine`):**
  - **Aislamiento en Proceso Separado/Activity Independiente:** Similar al inspector de fallos (`CrashInspectorActivity`), la herramienta se implementa como una Activity independiente para no contaminar las sesiones normales ni compartir memoria volátil con la navegación cotidiana.
  - **Módulo de Fugas en Base de Datos (Room Leak Check):** Realiza consultas concurrentes en `BrowserDatabase` (tablas `history_entries`, `cookies`, `tabs`) para corroborar que ninguna sesión privada genere registros residuales.
  - **Módulo de Memoria y Ciclo de Vida:** Evalúa el ciclo de vida de `GeckoSession` bajo modo privado (`isPrivate = true`, `contextId`), verifica la recolección de basura con `System.gc()` y confirma la purga de cachés volátiles con `storageController.clearData(ALL_CACHES or AUTH_SESSIONS)`.
  - **Módulo de Red y DNS (Network & DoH Inspector):** Audita la efectividad del cifrado DoH en GeckoView (`TRR_MODE_FIRST`), valida el cierre hermético de puertos STUN WebRTC (`media.peerconnection = false`) y detecta paquetes salientes en texto claro.
  - **Módulo de Huella Digital (Fingerprint Discrepancy Test):** Realiza un escaneo de variables biométricas y de hardware reportadas por el motor web, validando que el blindaje RFP ofusque resoluciones de pantalla, fuentes, canvas y mantenga un User-Agent genérico Tor/ESR estandarizado.
  - **Módulo de Fugas hacia el ISP (ISP Query Leak Sniffer):** Monitorea la capa de resolución de consultas de búsqueda para certificar que ningún término de búsqueda ni dominio sea transmitido en texto plano hacia los servidores DNS del proveedor de telefonía móvil u operadora de red.
  - **Diagnóstico Forense Crudo y Portapapeles:** En caso de discrepancias o fallos, genera un volcado técnico completo (Raw Stack Trace, volcado de memoria y JSON estructurado) con un botón flotante de copiado rápido al portapapeles del teléfono para diagnóstico inmediato.

