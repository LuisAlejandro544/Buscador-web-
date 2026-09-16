# Estructura del Proyecto y Arquitectura Técnica

Este documento describe la organización de módulos, paquetes, flujo de datos y patrones arquitectónicos adoptados en el desarrollo de la aplicación.

---

## 🏛️ Principios de Diseño y Arquitectura

1. **Separación de Responsabilidades (SoC):** Cada paquete y clase cumple un rol único y delimitado. La lógica de presentación está separada del acceso a datos y de la implementación concreta del motor web.
2. **Modularidad y Pantallas Independientes:** Siguiendo las directrices del proyecto, la aplicación evita concentrar todas las funciones en una sola vista monolítica. Cada funcionalidad relevante cuenta con su propia pantalla dedicada (Navegador, Gestor de Pestañas, Marcadores, Historial, Ajustes).
3. **Flujo de Datos Unidireccional (UDF):** El estado de la interfaz se modela mediante `StateFlow` inmutables emitidos por los ViewModels y consumidos reactivamente por los componentes de Jetpack Compose.
4. **Desacoplamiento del Motor Web:** A través de la interfaz `BrowserEngineContract`, la capa de interfaz no conoce detalles internos específicos de bajo nivel del motor de renderizado (p. ej. si se renderiza con GeckoView o con un fallback), lo cual permite evolucionar o cambiar de motor sin alterar la lógica de UI ni la gestión de pestañas.

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
│   │   ├── download/
│   │   │   ├── DownloadEngine.kt           # Motor autónomo de descargas concurrentes en streaming con pausas y reanudaciones
│   │   │   ├── DownloadNotificationHelper.kt # Gestor de notificaciones nativas de progreso y finalización
│   │   │   └── DownloadManagerHelper.kt    # Integración legacy con el servicio del sistema
│   │   ├── extension/
│   │   │   ├── ExtensionManager.kt         # Gestor integral de WebExtensions sobre GeckoView (descarga AMO, instalación, ciclo de vida)
│   │   │   ├── RecommendedExtension.kt     # Catálogo oficial curado (uBlock Origin, Dark Reader, TWP, ClearURLs)
│   │   │   └── WebExtensionModel.kt        # Modelo de estado para la interfaz en Compose
│   │   ├── sound/
│   │   │   └── SoundEffectManager.kt       # Gestor de efectos sonoros y háptica de baja latencia con SoundPool
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
│   │   │   ├── SitePermissionDao.kt # Acceso y control de permisos otorgados o bloqueados por dominio web
│   │   │   ├── TabDao.kt            # Acceso y persistencia de pestañas abiertas (normales y protegidas)
│   │   │   └── UserAccountDao.kt    # Acceso, conmutación y persistencia de cuentas de usuario
│   │   ├── entity/
│   │   │   ├── BookmarkEntity.kt    # Modelo relacional para marcadores
│   │   │   ├── CookieEntity.kt      # Modelo relacional para cookies (dominio, valor, expiración, isTracker)
│   │   │   ├── DownloadEntity.kt    # Modelo relacional para descargas (estado, bytes, URI)
│   │   │   ├── HistoryEntity.kt     # Modelo relacional para historial
│   │   │   ├── SitePermissionEntity.kt # Modelo relacional para permisos web (origen, tipo, estado y actualización)
│   │   │   ├── TabEntity.kt         # Modelo relacional para pestañas (incluye flags isIncognito, isProtected y contextId)
│   │   │   └── UserAccountEntity.kt # Modelo relacional para cuentas vinculadas (email, nombre, activo)
│   │   └── BrowserDatabase.kt       # Base de datos Room con control de versiones (v8) y migraciones
│   ├── model/
│   │   ├── BrowserTab.kt            # Modelo de dominio para pestañas
│   │   └── SearchEngine.kt          # Proveedores de búsqueda (DuckDuckGo, Google, Bing, etc.)
│   ├── preferences/
│   │   └── BrowserPreferences.kt    # Persistencia de preferencias del usuario mediante DataStore (incluye bloqueo de prompts)
│   └── repository/
│       └── BrowserRepository.kt     # Repositorio unificado que conecta DAOs, DataStore y ViewModel
├── ui/
│   ├── account/
│   │   └── AccountsScreen.kt        # Pantalla completa de gestión de cuentas vinculadas y conmutación
│   ├── bookmarks/
│   │   └── BookmarksScreen.kt       # Pantalla completa de marcadores con búsqueda y CRUD
│   ├── browser/
│   │   └── BrowserScreen.kt         # Pantalla principal con contenedor GeckoView, omnibox y menú
│   ├── components/
│   │   ├── WebPromptDialog.kt       # Diálogos nativos Material 3 para alerts, confirms, prompts, permisos y ficheros
│   │   └── WebSignInPromptBanner.kt # Banner flotante interactivo de acceso web con un solo toque
│   ├── cookies/
│   │   └── CookiesScreen.kt         # Pantalla de auditoría de cookies, detección de rastreadores y borrado
│   ├── downloads/
│   │   └── DownloadsScreen.kt       # Pantalla avanzada de descargas con búsqueda, apertura y vaciado
│   ├── extension/
│   │   └── ExtensionsScreen.kt      # Pantalla dedicada de gestión y exploración de WebExtensions
│   ├── history/
│   │   └── HistoryScreen.kt         # Pantalla completa de historial con búsqueda y eliminación
│   ├── navigation/
│   │   ├── BrowserNavGraph.kt       # Grafo y rutas de navegación con Jetpack Navigation Compose
│   │   └── Screen.kt                # Definición de pantallas y rutas fuertemente tipadas
│   ├── onboarding/
│   │   └── OnboardingScreen.kt      # Pantalla inicial de bienvenida (selección de buscador y extensiones)
│   ├── permissions/
│   │   └── SitePermissionsScreen.kt # Pantalla dedicada para gestión de permisos por sitio y políticas "No Preguntar"
│   ├── settings/
│   │   └── SettingsScreen.kt        # Pantalla completa de ajustes y configuración del navegador
│   ├── tabs/
│   │   ├── TabCardItem.kt           # Tarjeta individual con renderizado de miniatura, distintivo de reposo y cierre
│   │   └── TabsScreen.kt            # Pantalla en cuadrícula para gestionar pestañas normales, protegidas e incógnito
│   └── theme/
│       ├── Color.kt                 # Paleta de colores M3
│       ├── Theme.kt                 # Configuración de MaterialTheme con soporte de modo oscuro/claro
│       └── Type.kt                  # Configuración tipográfica
├── viewmodel/
│   └── BrowserViewModel.kt          # Gestor de estado centralizado que coordina motor Gecko, Room y UI
├── MainActivity.kt                  # Activity principal con configuración Edge-to-Edge y contenedor Compose
├── .github/
│   └── workflows/
│       └── build-debug.yml          # Workflow CI de GitHub Actions: compilación manual sin caché y artefactos divididos por ABI
└── generate_debug_keystore.sh       # Script de generación obligatoria y desatendida de debug.keystore (PKCS12)
```

---

## 🔄 Flujo de Interacción y Estados

```text
[Usuario / Interfaz Compose]
          │
          ▼ Dispara eventos (Intent: Abrir pestaña protegida, Navegar URL, Añadir marcador)
[BrowserViewModel]
          │
     ┌────┴──────────────────────────┐
     ▼                               ▼
[BrowserDatabase (Room v3)]  [BrowserEngineContract]
 (Pestañas, Historial,        (Carga URL, Back/Forward,
  Marcadores en SQLite)        Desktop Mode, Progreso)
     │                               │
     └─────────────┬─────────────────┘
                   ▼
       Emisión de StateFlow (BrowserUiState)
                   │
                   ▼ Recomposición reactiva
       [Pantallas de Jetpack Compose]
```

---

## 🛡️ Modularidad y Extensibilidad

- **Inyección y Ciclo de Vida:** La base de datos `BrowserDatabase` se inicializa como Singleton mediante lazy evaluation para evitar sobrecargas de memoria o accesos concurrentes destructivos.
- **Seguridad en Modo Incógnito:** Las pestañas marcadas como `isIncognito = true` no se persisten en la tabla `tabs` de Room y su navegación no genera registros en `history`.
- **Aislamiento por Pestañas Protegidas (Context Containers):** Cada pestaña protegida opera con su propio `contextId` inyectado en `GeckoSessionSettings.Builder`. Esto crea una partición estricta de cookies, caché web y `localStorage`. Al eliminarse la pestaña, `GeckoSessionManager` destruye la sesión y limpia el contexto en el motor invocando `runtime.storageController.clearDataForSessionContext(contextId)`.
- **Soporte GeckoView:** La infraestructura de Gradle importa `geckoview-omni` e incluye soporte nativo legacy para empaquetado de librerías ELF `.so` (`libxul.so`, etc.), permitiendo instanciar `GeckoRuntime` y `GeckoSession` implementando el contrato `BrowserEngineContract`.
- **Auditoría de Cookies y Eliminación Sincronizada:** El módulo `ui/cookies/CookiesScreen.kt` audita cookies locales indexadas en Room v4 e interactúa con `GeckoRuntime.storageController` para purgas por host o globales. Incorpora heurísticas para etiquetar cookies rastreadoras de terceros (`isTracker = true`) y advertir al usuario en la interfaz.
- **Inmunidad de Tipografía Móvil (`fontScale = 1.0f`):** En `Theme.kt`, se inyecta una instancia personalizada de `Density` mediante `CompositionLocalProvider(LocalDensity provides ...)`. Esto desvincula la interfaz del multiplicador de fuente global del sistema operativo del dispositivo, asegurando proporciones estables en cualquier pantalla de teléfono sin importar el tamaño de letra configurado en Android.
- **Miniaturas Gráficas y Suspensión Inteligente (5 min):** `TabThumbnailManager` almacena capturas escaladas de las páginas visitadas mediante `GeckoDisplay.capturePixels()`. Paralelamente, `BrowserViewModel` evalúa el tiempo de inactividad de las pestañas cada 15 segundos; si una pestaña supera los 5 minutos sin foco, invoca `GeckoSessionManager.hibernateSession()` cerrando el proceso de GeckoView y liberando memoria RAM del teléfono. La miniatura y el estado visual permanecen intactos en la cuadrícula indicando "En reposo (5 min)"; al tocar la pestaña, la sesión se reactiva automáticamente.
- **Blindaje Avanzado del Modo Incógnito (No Genérico):**
  - **RFP (Resist Fingerprinting):** Configuración activa de preferencias estilo Tor (`privacy.resistFingerprinting = true`) enmascarando métricas de pantalla, canvas, APIs de audio y User-Agent genérico Tor/ESR para frustrar el fingerprinting biométrico y del dispositivo móvil.
  - **Anti IP-Leak (WebRTC Bloqueado):** Desactivación estricta de `media.peerconnection` y aislamiento STUN, junto a un `permissionDelegate` en `GeckoViewEngine` que rechaza solicitudes de cámara y micrófono en incógnito para impedir fugas de direcciones IP privadas y públicas.
  - **DNS sobre HTTPS Cifrado (DoH):** Forzado de resolución cifrada con Cloudflare/Mozilla (`TRR_MODE_FIRST`), imposibilitando el espionaje o filtrado de tráfico por parte de proveedores de internet y operadoras móviles.
  - **Total Cookie Protection (dFPI):** Confinamiento de cookies y almacenamiento al dominio de primer nivel (`ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS`, `privacy.partition.network_state = true`), previniendo el rastreo cruzado entre sitios.
  - **Purga Inmediata de RAM:** Al cerrar cualquier pestaña de incógnito o salir del modo, `GeckoSessionManager` y `GeckoRuntimeProvider` ejecutan `storageController.clearData(ALL_CACHES or AUTH_SESSIONS)`, destruyen miniaturas y disparan `System.gc()`.
  - **Protección Visual FLAG_SECURE:** Bloqueo de capturas de pantalla y ocultación visual en la multitarea de Android al navegar en incógnito.
  - **Arquitectura Abierta para Expansión Continua:** El motor de incógnito está concebido para incorporar progresivamente más capas de defensa activa (bloqueo heurístico de telemetría oculta en scripts, virtualización de red y sandboxing riguroso).
- **Capa Nativa Híbrida (C++26 / Rust 2024):** Preparada mediante NDK r28 y CMake 3.31+ para vincular librerías `.so` de alto rendimiento. Rust asume la lógica pesada de seguridad (bloqueo de anuncios, hashes criptográficos, protección de rastreo) y C++ proporciona aceleración por hardware y enlace con APIs nativas del sistema. Los artefactos temporales de compilación de CMake y Cargo quedan completamente aislados por `.gitignore`.
- **Ecosistema de Extensiones Web (WebExtensions) Bajo Demanda y Prevención de Licencias Víricas:**
  - **Descarga Oficial Directa desde Mozilla Add-ons (AMO):** En estricto cumplimiento de la política de código cerrado y protección legal de propiedad intelectual, el proyecto **NO** empaqueta binarios `.xpi` de extensiones bajo licencias copyleft o víricas (como GPLv3 en uBlock Origin) dentro del APK o carpeta `assets`. En su lugar, el navegador actúa como un agente de usuario neutral que se conecta directamente a los servidores oficiales de Mozilla (`https://addons.mozilla.org/firefox/downloads/latest/...`) bajo petición explícita y consentimiento del usuario.
  - **Ciclo de Vida y Gestor Desacoplado (`ExtensionManager`):**
    - Se apoya en `GeckoRuntime.webExtensionController` para la descarga, verificación de permisos e instalación dinámica en tiempo de ejecución.
    - Soporta la activación (`enable()`), desactivación (`disable()`) y desinstalación (`uninstall()`) en caliente sin requerir reinicio del proceso de GeckoView.
    - Emite flujos reactivos `StateFlow` con el progreso porcentual de descarga por extensión y la lista actualizada de complementos para consumo en Jetpack Compose.
  - **Flujo de Configuración Inicial (Onboarding) y Pantalla Dedicada:**
    - `OnboardingScreen`: En el primer arranque, el usuario selecciona qué motor de búsqueda desea fijar y qué extensiones del catálogo desea instalar automáticamente antes de navegar.
    - `ExtensionsScreen`: Pantalla dedicada para auditar complementos activos, alternar su estado, eliminarlos o instalar cualquier extensión externa mediante URL directa.
  - **Sinergia Futura con el Motor Nativo Rust (`core-native`):**
    En smartphones, ejecutar cientos de miles de reglas exclusivamente en el motor JavaScript de una extensión satura el hilo de eventos y consume batería. La arquitectura propone una división de responsabilidades simbiótica:
    1. *uBlock Origin (Capa Web / Interfaz / Inyección DOM):* Proporciona la interfaz de usuario familiar, defusers de scriptlets para evadir anti-adblockers y filtrado cosmético de nodos en el DOM.
    2. *Rust `core-native` (Capa de Rendimiento Extremo / JNI):*
       - **Evaluación de Filtros en Memoria Nativa:** Indexación de listas masivas (EasyList, Peter Lowe, etc.) en estructuras ultra compactas (Tries, filtros de Bloom y autómatas Aho-Corasick) ejecutadas a velocidad nativa sin sobrecargar el recolector de basura de Java ni la máquina virtual JS.
       - **Desinfección Instantánea de URLs (Query Parameter Stripping):** Limpieza inmediata de tokens de seguimiento y telemetría (`fbclid`, `gclid`, `utm_*`, `yclid`, `mc_eid`) en Rust antes de despachar la petición de red.
       - **Filtrado Previo a Nivel de Red y DNS:** Bloqueo de peticiones maliciosas antes de la negociación TLS en GeckoView, reduciendo drásticamente el consumo de datos móviles y energía del procesador.
       - **Auditoría y Métricas Zero-Copy:** Contadores de elementos bloqueados y telemetría de amenazas expuestos a Compose con latencia mínima.
- **Canal de Despliegue y Artefactos por Arquitectura (GitHub Actions CI):** El flujo en `.github/workflows/build-debug.yml` implementa compilación continua sin intermediarios: compila los APKs de depuración de forma limpia (sin cachés), genera una firma `debug.keystore` fresca, genera binarios divididos por arquitectura (`splits.abi`) y los publica como artefactos independientes en GitHub (`app-debug-arm64-v8a`, `app-debug-armeabi-v7a`, `app-debug-x86_64`, `app-debug-x86`) para permitir descargas optimizadas directamente al móvil sin pesos universales redundantes.
- **Gestión de Cuentas e Identidad Web Integrada (`AccountCredentialManager` y `WebSignInBridge`):**
  - **Vinculación Nativa con `androidx.credentials`:** Utiliza `CredentialManager` y `GetGoogleIdOption` para enlazar cuentas de Google o credenciales personalizadas en el dispositivo móvil sin depender de servicios propietarios inflexibles.
  - **Almacenamiento Reactivo con Room v4 (`UserAccountEntity` / `UserAccountDao`):** Persistencia local de perfiles con soporte multicuenta, avatar, correo y bandera de cuenta activa (`isActive`), permitiendo conmutar la identidad principal en cualquier momento.
  - **Detección Dinámica de Autenticación (`WebSignInBridge`):** Analiza en tiempo real las URLs cargadas en el motor web para identificar portales de acceso, protocolos OAuth2, OpenID Connect y botones de inicio de sesión de Google.
  - **Banner Interactivo One-Tap en Jetpack Compose (`WebSignInPromptBanner`):** Despliega un componente animado en la parte superior del navegador con la identidad activa del usuario cuando se visita un sitio web compatible.
  - **Inyección y Autocompletado en GeckoView:** Al presionar "Continuar", el navegador evalúa JavaScript en el contexto de la página para rellenar campos de correo/usuario o activar selectores de inicio de sesión de Google automáticamente.
  - **Pantalla Dedicada (`AccountsScreen`):** Vista completa accesible desde el menú principal del navegador y desde Ajustes para vincular cuentas, alternar perfiles y gestionar credenciales guardadas.

