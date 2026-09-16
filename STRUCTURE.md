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
app/src/main/java/com/example/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── BookmarkDao.kt       # Acceso a marcadores guardados
│   │   │   ├── HistoryDao.kt        # Acceso al historial cronológico de navegación
│   │   │   └── TabDao.kt            # Acceso y persistencia de pestañas abiertas
│   │   ├── entity/
│   │   │   ├── BookmarkEntity.kt    # Modelo relacional para marcadores
│   │   │   ├── HistoryEntity.kt     # Modelo relacional para historial
│   │   │   └── TabEntity.kt         # Modelo relacional para pestañas (incluye flag isIncognito)
│   │   └── BrowserDatabase.kt       # Base de datos Room con control de versiones y migraciones
│   └── model/
│       ├── BrowserTab.kt            # Modelo de dominio para pestañas
│       ├── SearchEngine.kt          # Definición de proveedores de búsqueda (DuckDuckGo, Google, etc.)
│       └── UserSettings.kt          # Preferencias de usuario (motor de búsqueda, JS, DNT, desktop mode)
├── engine/
│   └── contract/
│       └── BrowserEngineContract.kt # Interfaz abstracta que define las operaciones de navegación web
├── ui/
│   ├── bookmarks/
│   │   └── BookmarksScreen.kt       # Pantalla completa de marcadores con búsqueda y CRUD
│   ├── browser/
│   │   ├── BrowserScreen.kt         # Pantalla principal con barra omnibox, barra de herramientas y visor
│   │   ├── BrowserHomeView.kt       # Pantalla de inicio visual (accesos rápidos, atajos y bienvenida)
│   │   └── BrowserWebView.kt        # Componente visual que acopla el visor web con Compose
│   ├── history/
│   │   └── HistoryScreen.kt         # Pantalla completa de historial con búsqueda y eliminación
│   ├── navigation/
│   │   └── BrowserNavigation.kt     # Grafo y rutas de navegación con Jetpack Navigation Compose
│   ├── settings/
│   │   └── SettingsScreen.kt        # Pantalla completa de ajustes y configuración del navegador
│   ├── tabs/
│   │   └── TabsScreen.kt            # Pantalla en cuadrícula para gestionar pestañas normales e incógnito
│   └── theme/
│       ├── Color.kt                 # Paleta de colores M3
│       ├── Theme.kt                 # Configuración de MaterialTheme con soporte de modo oscuro/claro
│       └── Type.kt                  # Configuración tipográfica
├── viewmodel/
│   └── BrowserViewModel.kt          # Gestor de estado centralizado que coordina motor, Room y UI
└── MainActivity.kt                  # Activity principal con configuración Edge-to-Edge y contenedor Compose
```

---

## 🔄 Flujo de Interacción y Estados

```text
[Usuario / Interfaz Compose]
          │
          ▼ Dispara eventos (Intent: Abrir pestaña, Navegar URL, Añadir marcador)
[BrowserViewModel]
          │
     ┌────┴──────────────────────────┐
     ▼                               ▼
[BrowserDatabase (Room)]    [BrowserEngineContract]
 (Pestañas, Historial,           (Carga URL, Back/Forward,
   Marcadores en SQLite)          Desktop Mode, Progreso)
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
- **Soporte GeckoView:** La infraestructura de Gradle importa `geckoview-omni` e incluye soporte nativo legacy para empaquetado de librerías ELF `.so` (`libxul.so`, etc.), permitiendo instanciar `GeckoRuntime` y `GeckoSession` implementando el contrato `BrowserEngineContract`.
- **Capa Nativa Híbrida (C++26 / Rust 2024):** Preparada mediante NDK r28 y CMake 3.31+ para vincular librerías `.so` de alto rendimiento. Rust asume la lógica pesada de seguridad (bloqueo de anuncios, hashes criptográficos, protección de rastreo) y C++ proporciona aceleración por hardware y enlace con APIs nativas del sistema. Los artefactos temporales de compilación de CMake y Cargo quedan completamente aislados por `.gitignore`.

