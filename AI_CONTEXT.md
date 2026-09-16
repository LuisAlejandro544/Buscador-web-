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

6. **Integración Continua, Firma Automática y Entrega P2P en GitHub Actions:**
   - **Workflow (`.github/workflows/build-debug.yml`):** Activación exclusivamente manual (`workflow_dispatch`), compilación limpia sin caché (`cache-disabled: true`, `--no-build-cache`), instalación de NDK 28 (`28.2.13676358`), CMake 3.31.6, Rust 2024 y descarga de GeckoView Omni de Mozilla.
   - **Script de Firma (`generate_debug_keystore.sh`):** Fuerza la generación no interactiva de un archivo `debug.keystore` (PKCS12, RSA 2048 bits) desde cero en el runner de CI para firmar el APK sin requerir interacción ni secretos preexistentes.
   - **Sincronización Directa P2P al Móvil con Syncthing:** Se inicia un nodo local de Syncthing en el runner (usando identidad TLS fija en `.github/syncthing/` con ID `IAXLEGX-HNWFEWZ-P4OQVFW-VBKMPQ2-ZJI6MX6-OE6YPBD-S4BC346-266H4AO`) y el script `.github/scripts/syncthing_transfer.py` transfiere directamente `app-debug.apk` a la carpeta `/storage/emulated/0/Navegador` de la app **Syncthing-fork** en el teléfono mediante la red global P2P con relays cifrados.
   - **Secreto de Actions Requerido:** Únicamente `PHONE_SYNCTHING_ID` (el ID del teléfono en Syncthing). Si el teléfono no estuviera en línea o alcanzara el tiempo límite, el APK se preserva intacto en los artefactos descargables de GitHub como respaldo.

