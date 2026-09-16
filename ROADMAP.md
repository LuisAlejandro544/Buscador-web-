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

### 🟡 Fase 2: Cableado del Motor GeckoView y Subsistema Nativo (En Progreso)
- [x] Incorporación del repositorio oficial Maven de Mozilla (`https://maven.mozilla.org/maven2/`).
- [x] Integración de la dependencia `org.mozilla.geckoview:geckoview-omni` en `libs.versions.toml` y `app/build.gradle.kts`.
- [x] Configuración de empaquetado nativo JNI (`useLegacyPackaging = true`) para librerías binarias nativas C++ de Gecko en arquitecturas `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`.
- [x] Establecimiento de base de compilación nativa en `libs.versions.toml` y Gradle: NDK 28 LTS (`28.2.13676358`), CMake 3.31.6, flags C++26 (`-std=c++26 -O3`), C23 (`-std=c23 -O3`) y Rust Edition 2024 / 1.89.0.
- [ ] Implementación de `GeckoViewEngine` implementando `BrowserEngineContract`.
- [ ] Configuración del singleton `GeckoRuntime` con banderas de optimización de memoria para dispositivos móviles.
- [ ] Implementación de `GeckoSession` por pestaña con delegados de navegación, progreso y seguridad.
- [ ] Módulo nativo Rust/C++ (`core-native`): Procesamiento de filtros de red, hashing de URLs y seguridad anti-rastreo a bajo nivel.
- [ ] Alternador en ajustes para seleccionar motor de renderizado activo (GeckoView / WebView).

---

### 🔵 Fase 3: Capacidades Avanzadas de Navegación y Privacidad (Siguiente)
- [ ] **Soporte de WebExtensions:** Integración de extensiones de Mozilla (bloqueadores de publicidad como uBlock Origin, gestores de scripts).
- [ ] **Gestor de Descargas Nativo:** Panel de control para descargas en segundo plano con soporte para pausar, reanudar y visualización de progreso.
- [ ] **Lector de Modo Lectura:** Extracción del contenido principal de artículos para lectura limpia sin anuncios ni estilos intrusivos.
- [ ] **Protección contra Rastreo Mejorada (ETP):** Bloqueo nativo de rastreadores de terceros y cookies de seguimiento mediante GeckoView.

---

### 🟣 Fase 4: Rendimiento, Exportación y Distribución
- [ ] **Optimizaciones de Memoria en Segundo Plano:** Hibernación de pestañas no visibles para evitar consumo excesivo de RAM.
- [ ] **Generación de APKs optimizados por arquitectura:** Configuración de splits ABI (`arm64-v8a`, `armeabi-v7a`) para usuarios que descarguen desde Uptodown y deseen menor peso por instalación.
- [ ] **Copia de Seguridad y Restauración Local:** Exportación e importación de marcadores e historial en formatos abiertos (HTML/JSON).
