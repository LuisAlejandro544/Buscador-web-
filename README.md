# Navegador Web Android

Navegador web moderno, modular y extensible para dispositivos Android (Android 10+), desarrollado con **Kotlin**, **Jetpack Compose**, persistencia local con **Room** y una arquitectura de motor web desacoplada preparada para **GeckoView**.

---

## 🌟 Características Principales

### 🧭 Navegación y Motor Web
- **Arquitectura de motor desacoplada (`BrowserEngineContract`)**: La interfaz y la lógica de navegación no dependen rígidamente de un motor específico; actualmente cuenta con un motor activo basado en Android WebView acelerado por hardware y la infraestructura completa de Gradle configurada para **GeckoView Omni** de Mozilla.
- **Modo Normal e Incógnito**: Navegación privada real que aísla las pestañas, inhabilita el guardado en base de datos local y purga rastros de sesión.
- **Modo Escritorio bajo demanda**: Posibilidad de alternar entre versión móvil y versión de escritorio para sitios web que requieren interfaz completa.
- **Protección de Privacidad**: Envío automático de la cabecera *Do Not Track* (`DNT: 1`), control de cookies y ejecución de JavaScript configurable.
- **Omnibox inteligente**: Barra de direcciones y búsqueda integrada con detección de URLs válidas y compatibilidad con múltiples motores de búsqueda.

### 📑 Gestión Avanzada de Pestañas
- **Pestañas múltiples independientes**: Pantalla dedicada en cuadrícula para gestionar pestañas normales y pestañas de incógnito.
- **Persistencia de sesión**: Las pestañas abiertas se guardan en una base de datos local SQLite mediante Room para que no se pierdan al cerrar la app.
- **Cierre selectivo o total**: Cierra pestañas individuales o todas en bloque mediante confirmación segura.

### ⭐ Marcadores y Favoritos
- Almacena tus páginas web favoritas con título, URL y fecha.
- Búsqueda en tiempo real dentro de marcadores.
- Posibilidad de añadir marcadores manualmente o directamente con un toque en el menú de navegación.

### 📜 Historial de Navegación
- Registro cronológico detallado de visitas en modo normal.
- Búsqueda instantánea por dominio o palabras clave.
- Limpieza individual de elementos o borrado completo de historial y caché.

### ⚙️ Ajustes y Personalización
- Selector de motores de búsqueda predeterminados: DuckDuckGo, Google, Bing, Brave y Ecosia.
- Configuración de página de inicio personalizada o pantalla de inicio nativa (`about:home`).
- Acceso directo al gestor de descargas seguro de Android.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología / Librería | Propósito |
| :--- | :--- | :--- |
| **Lenguaje Principal** | Kotlin 2.2+ | Desarrollo seguro, moderno y conciso para la interfaz y ciclo de vida |
| **Interfaz de Usuario** | Jetpack Compose + Material Design 3 | UI declarativa, temas adaptativos y componentes dinámicos |
| **Capa Nativa (C++)** | C++26 / C23 (NDK r28 LTS, CMake 3.31+) | Máximo rendimiento, aceleración gráfica por hardware y bindings nativos |
| **Capa Nativa (Rust)** | Rust Edition 2024 / Toolchain 1.89+ | Procesamiento seguro de red, filtrado de reglas, criptografía y privacidad |
| **Persistencia Local** | Android Room Database (SQLite) + DataStore | Almacenamiento reactivo de pestañas, historial, marcadores y ajustes |
| **Motor Web** | GeckoView Omni (Mozilla) / Android WebView | Motor web potente, extensible y personalizable |
| **Arquitectura** | MVVM + Clean Architecture + StateFlow | Desacoplamiento de capas y flujo de datos unidireccional (UDF) |
| **Navegación** | Jetpack Navigation Compose | Enrutamiento desacoplado entre pantallas dedicadas |


---

## 📱 Requisitos y Compatibilidad

- **Versión mínima de Android:** Android 10 (API 29).
- **Versión objetivo:** Android 16 (API 36).
- **Arquitecturas soportadas (32 y 64 bits):**
  - `arm64-v8a` (Dispositivos móviles modernos)
  - `armeabi-v7a` (Dispositivos móviles anteriores de 32 bits)
  - `x86_64` (Emuladores y tablets modernas basadas en Intel/AMD)
  - `x86` (Entornos emulados o hardware x86 de 32 bits)
- **Distribución:** Diseñado para exportación e instalación directa mediante APK universal o dividido por ABI, ideal para tiendas y repositorios de terceros como **Uptodown**, **APKMirror** o distribución directa.

---

## 📂 Compilación y Construcción

Para compilar el proyecto en modo depuración (*Debug*) o generar el paquete APK:

```bash
# Compilar y ensamblar APK de depuración
gradle :app:assembleDebug

# Ejecutar pruebas unitarias locales
gradle :app:testDebugUnitTest
```

El APK resultante se genera en el directorio:
`app/build/outputs/apk/debug/app-debug.apk`
