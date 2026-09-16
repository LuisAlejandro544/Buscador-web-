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
│   │   ├── download/
│   │   │   └── DownloadManagerHelper.kt # Gestor de descargas con integración al DownloadManager de Android
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
│   │   │   └── TabDao.kt            # Acceso y persistencia de pestañas abiertas (normales y protegidas)
│   │   ├── entity/
│   │   │   ├── BookmarkEntity.kt    # Modelo relacional para marcadores
│   │   │   ├── CookieEntity.kt      # Modelo relacional para cookies (dominio, valor, expiración, isTracker)
│   │   │   ├── DownloadEntity.kt    # Modelo relacional para descargas (estado, bytes, URI)
│   │   │   ├── HistoryEntity.kt     # Modelo relacional para historial
│   │   │   └── TabEntity.kt         # Modelo relacional para pestañas (incluye flags isIncognito, isProtected y contextId)
│   │   └── BrowserDatabase.kt       # Base de datos Room con control de versiones (v4) y migraciones
│   ├── model/
│   │   ├── BrowserTab.kt            # Modelo de dominio para pestañas
│   │   └── SearchEngine.kt          # Proveedores de búsqueda (DuckDuckGo, Google, Bing, etc.)
│   ├── preferences/
│   │   └── BrowserPreferences.kt    # Persistencia de preferencias del usuario mediante DataStore
│   └── repository/
│       └── BrowserRepository.kt     # Repositorio unificado que conecta DAOs, DataStore y ViewModel
├── ui/
│   ├── bookmarks/
│   │   └── BookmarksScreen.kt       # Pantalla completa de marcadores con búsqueda y CRUD
│   ├── browser/
│   │   └── BrowserScreen.kt         # Pantalla principal con contenedor GeckoView, omnibox y menú
│   ├── components/
│   │   └── WebPromptDialog.kt       # Diálogos nativos Material 3 para alerts, confirms, prompts y ficheros
│   ├── cookies/
│   │   └── CookiesScreen.kt         # Pantalla de auditoría de cookies, detección de rastreadores y borrado
│   ├── downloads/
│   │   └── DownloadsScreen.kt       # Pantalla avanzada de descargas con búsqueda, apertura y vaciado
│   ├── history/
│   │   └── HistoryScreen.kt         # Pantalla completa de historial con búsqueda y eliminación
│   ├── navigation/
│   │   ├── BrowserNavGraph.kt       # Grafo y rutas de navegación con Jetpack Navigation Compose
│   │   └── Screen.kt                # Definición de pantallas y rutas fuertemente tipadas
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
│   ├── scripts/
│   │   └── syncthing_transfer.py    # Script de sincronización P2P desatendida hacia Syncthing-fork
│   ├── syncthing/
│   │   ├── cert.pem                 # Certificado TLS fijo de Syncthing para GitHub Actions
│   │   └── key.pem                  # Clave privada TLS fija de Syncthing para GitHub Actions
│   └── workflows/
│       └── build-debug.yml          # Workflow CI de GitHub Actions: compilación manual sin caché y entrega P2P
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
- **Canal de Despliegue P2P (GitHub Actions + Syncthing):** El flujo en `.github/workflows/build-debug.yml` implementa entrega continua sin intermediarios: compila el APK Debug de forma limpia (sin cachés), genera una firma `debug.keystore` fresca y transfiere el binario directamente al almacenamiento del móvil (`/storage/emulated/0/Navegador/app-debug.apk`) a través del protocolo P2P de Syncthing con relays globales cifrados de extremo a extremo.

