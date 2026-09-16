# Navegador Web Android

Navegador web moderno, modular y extensible para dispositivos Android (Android 10+), desarrollado con **Kotlin**, **Jetpack Compose**, persistencia local con **Room** y una arquitectura de motor web desacoplada preparada para **GeckoView**.

---

## 🌟 Características Principales

### 🧭 Navegación y Motor Web GeckoView
- **Motor Web de Alto Rendimiento con GeckoView Omni**: Navegación web potenciada por el motor de renderizado de Mozilla, ofreciendo cumplimiento de estándares web modernos, aislamiento de procesos y alta fidelidad visual.
- **Pestañas Protegidas (Burbuja Aislada de Cookies y Sesiones)**: Navegación en contenedores contextuales independientes (`contextId`). Si visitas una web, aceptas cookies o inicias sesión en una pestaña protegida, sus datos y almacenamiento web quedan herméticamente confinados a esa pestaña sin alterar tus cuentas o perfiles principales. Al cerrarla, su contexto se limpia por completo con `clearDataForSessionContext`.
- **Sincronización Dinámica de Ajustes**: Modificaciones instantáneas de preferencias (JavaScript activado/desactivado, cabecera *Do Not Track* `DNT: 1`, modo escritorio por pestaña o global) aplicadas directamente sobre las sesiones activas de GeckoView sin necesidad de reiniciar la app.
- **Gestión Nativa de Diálogos Web**: Integración de `PromptDelegate` mediante `GeckoPromptHandler` y diálogo nativo en Compose (`WebPromptDialog`), interceptando de manera segura alertas JavaScript (`alert`), diálogos de confirmación (`confirm`), campos de entrada (`prompt`), autenticación HTTP y selección de archivos locales (`<input type="file">`).
- **Modo Normal, Protegido e Incógnito**: Tres modos de navegación diferenciados: normal (persistencia estándar), protegida (aislamiento por contenedor contextual) e incógnito (sin rastro de historial ni cookies locales).
- **Omnibox inteligente**: Barra de direcciones y búsqueda integrada con detección de URLs y compatibilidad con DuckDuckGo, Google, Bing, Brave y Ecosia.

### 📥 Gestor de Descargas Avanzado
- **Intercepción Automática:** Detección de descargas mediante cabeceras `Content-Disposition` o tipos de archivos descargables desde GeckoView (`WebResponse`).
- **Integración con Android DownloadManager:** Descargas gestionadas en segundo plano con notificaciones nativas del sistema.
- **Persistencia en Room Database:** Registro local de archivos descargados con nombre, peso formateado, fecha y estado.
- **Pantalla Dedicada:** Búsqueda rápida de descargas, apertura con visores del sistema Android y opciones para limpiar el historial.

### 📑 Gestión Avanzada de Pestañas
- **Pestañas múltiples independientes**: Pantalla dedicada en cuadrícula con segmentación tripartita para gestionar pestañas **Normales**, **Protegidas** (aisladas con escudo esmeralda) e **Incógnito**.
- **Persistencia de sesión**: Las pestañas abiertas se guardan en una base de datos local SQLite mediante Room v3 para que no se pierdan al cerrar la app.
- **Cierre selectivo o total con purga**: Cierra pestañas individuales o todas en bloque; las pestañas protegidas purgan de inmediato su contexto de cookies de GeckoView al eliminarse.

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
- Control reactivo de privacidad, cookies y borrado completo de datos de navegación.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología / Librería | Propósito |
| :--- | :--- | :--- |
| **Lenguaje Principal** | Kotlin 2.2+ | Desarrollo seguro, moderno y conciso para la interfaz y ciclo de vida |
| **Interfaz de Usuario** | Jetpack Compose + Material Design 3 | UI declarativa, temas adaptativos y componentes dinámicos |
| **Capa Nativa (C++)** | C++26 / C23 (NDK r28 LTS, CMake 3.31+) | Puente JNI `browser_native`, hashing y aceleración nativa por hardware |
| **Capa Nativa (Rust)** | Rust Edition 2024 (`core-native`) | Infraestructura base para filtrado de red de alto rendimiento, hashes y seguridad |
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
# Compilar y ensamblar APK de depuración (incluyendo CMake / NDK / JNI)
gradle :app:assembleDebug

# Verificar módulo nativo Rust (core-native)
cargo check --manifest-path core-native/Cargo.toml

# Ejecutar pruebas unitarias locales
gradle :app:testDebugUnitTest
```

El APK resultante se genera en el directorio:
`app/build/outputs/apk/debug/app-debug.apk` y contiene las librerías binarias nativas `libbrowser_native.so` y `libxul.so` para las arquitecturas `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`.

---

## 🤖 Integración Continua (GitHub Actions CI)

El repositorio cuenta con un flujo automatizado en `.github/workflows/build-debug.yml`:
- **Compilación Limpia (Sin Caché):** Descarga el código, instala Java JDK 17, Android NDK 28, CMake 3.31+, Rust 2024 y descarga las dependencias de GeckoView Omni de Mozilla.
- **Generación Forzada de Firma:** Ejecuta `./generate_debug_keystore.sh` para generar un almacén de claves `debug.keystore` desde cero de forma 100% desatendida.
- **Artefacto Descargable:** Tras compilar `app-debug.apk`, lo sube automáticamente a los artefactos de la ejecución para descargarlo e instalarlo directamente en el teléfono.
- **Ejecución Manual:** Disparable en cualquier momento desde la pestaña **Actions** de GitHub con el botón *Run workflow*.
