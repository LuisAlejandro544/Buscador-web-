# Navegador Web Android

Navegador web moderno, modular y extensible para dispositivos Android (Android 10+), desarrollado con **Kotlin**, **Jetpack Compose**, persistencia local con **Room** y una arquitectura de motor web desacoplada preparada para **GeckoView**.

---

## 🌟 Características Principales

### 🧭 Navegación y Motor Web GeckoView
- **Motor Web de Alto Rendimiento con GeckoView Omni**: Navegación web potenciada por el motor de renderizado de Mozilla, ofreciendo cumplimiento de estándares web modernos, aislamiento de procesos y alta fidelidad visual.
- **Pestañas Protegidas (Burbuja Aislada de Cookies y Sesiones)**: Navegación en contenedores contextuales independientes (`contextId`). Si visitas una web, aceptas cookies o inicias sesión en una pestaña protegida, sus datos y almacenamiento web quedan herméticamente confinados a esa pestaña sin alterar tus cuentas o perfiles principales. Al cerrarla, su contexto se limpia por completo con `clearDataForSessionContext`.
- **Modo Incógnito Avanzado con Blindaje Activo (No Genérico)**:
  - 🛡️ **Protección contra Huella Digital (Resist Fingerprinting - RFP):** Inyección de parámetros estilo Tor que neutralizan intentos de identificación biométrica o de hardware unificando dimensiones de pantalla, canvas, APIs de audio y User-Agent genérico Tor/ESR.
  - 🌐 **Bloqueo de Fugas WebRTC (Anti IP-Leak):** `media.peerconnection` desactivado por completo y puertos STUN herméticos junto a denegación estricta de permisos de cámara/micrófono para que ningún script externo descubra tu IP real.
  - 🔒 **DNS sobre HTTPS Cifrado (DoH):** Resolución cifrada obligatoria con Cloudflare/Mozilla (`TRR_MODE_FIRST`), impidiendo que proveedores de internet (ISP) o redes Wi-Fi públicas espíen los sitios que visitas.
  - 🍪 **Total Cookie Protection (dFPI):** Aislamiento estricto de almacenamiento y cookies confinado exclusivamente al host de origen (`ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS`), impidiendo el rastreo cruzado entre plataformas.
  - ⚡ **Purga Inmediata de Memoria RAM al Cerrar:** Destrucción y limpieza forzada instantánea de cachés en memoria (`storageController.clearData(ALL_CACHES or AUTH_SESSIONS)`), borrado de miniaturas y recolección de basura con `System.gc()`.
  - 👁️ **Protección de Pantalla FLAG_SECURE:** Bloqueo de capturas de pantalla, grabadores externos y ocultación visual en la multitarea de Android.
  - 🚀 **Evolución Continua:** Diseñado con arquitectura modular para seguir incorporando activamente muchas más capas de seguridad (defensa heurística de scripts, sandboxing de sensores y ofuscación de red) para convertir el modo incógnito en un auténtico bastión de privacidad.
- **Sincronización Dinámica de Ajustes**: Modificaciones instantáneas de preferencias (JavaScript activado/desactivado, cabecera *Do Not Track* `DNT: 1`, modo escritorio por pestaña o global) aplicadas directamente sobre las sesiones activas de GeckoView sin necesidad de reiniciar la app.
- **Gestión Nativa de Diálogos Web**: Integración de `PromptDelegate` mediante `GeckoPromptHandler` y diálogo nativo en Compose (`WebPromptDialog`), interceptando de manera segura alertas JavaScript (`alert`), diálogos de confirmación (`confirm`), campos de entrada (`prompt`), autenticación HTTP y selección de archivos locales (`<input type="file">`).
- **Modo Normal, Protegido e Incógnito**: Tres modos de navegación diferenciados: normal (persistencia estándar), protegida (aislamiento por contenedor contextual) e incógnito (sin rastro de historial ni cookies locales).
- **Omnibox inteligente**: Barra de direcciones y búsqueda integrada con detección de URLs y compatibilidad con DuckDuckGo, Google, Bing, Brave y Ecosia.

### 🍪 Gestor y Auditor de Cookies de Navegación
- **Auditoría Detallada:** Visualización en tiempo real de todas las cookies almacenadas, detallando nombre, dominio de procedencia, ruta, caducidad y atributos de seguridad (`Secure`, `HttpOnly`).
- **Detección de Rastreadores (Trackers):** Identificación automática de cookies de telemetría y publicidad de terceros con distintivo rojo y filtro rápido ("Solo rastreadores").
- **Control Selectivo y Masivo:** Posibilidad de inspeccionar o eliminar cookies individuales por dominio, purgar todas las cookies de rastreo con un solo toque o vaciar el almacén completo.
- **Sincronización con GeckoView StorageController:** La eliminación en la app coordina la purga en la base de datos Room y en el motor `GeckoRuntime.storageController` (`clearDataFromHost` y `clearData`).

### 🔤 Interfaz Optimizada y Escala de Fuente Fija
- **Diseño Móvil Estable:** Tamaño de fuente fijado a escala `1.0f` (`CompositionLocalProvider` sobre `LocalDensity`) para evitar que las configuraciones de accesibilidad o tamaño de texto gigante del sistema operativo Android desborden o rompan los componentes visuales de la app.
- **Navegación Táctil Cómoda:** Barras de herramientas con `navigationBarsPadding()` adaptadas para no interferir con los botones virtuales o gestos de navegación de Android.

### 📥 Gestor de Descargas Avanzado
- **Intercepción Automática:** Detección de descargas mediante cabeceras `Content-Disposition` o tipos de archivos descargables desde GeckoView (`WebResponse`).
- **Integración con Android DownloadManager:** Descargas gestionadas en segundo plano con notificaciones nativas del sistema.
- **Persistencia en Room Database:** Registro local de archivos descargados con nombre, peso formateado, fecha y estado.
- **Pantalla Dedicada:** Búsqueda rápida de descargas, apertura con visores del sistema Android y opciones para limpiar el historial.

### 📑 Gestión Avanzada de Pestañas
- **Pestañas Múltiples con Miniaturas en Vivo**: Pantalla dedicada en cuadrícula donde cada pestaña muestra una miniatura gráfica real capturada con `GeckoDisplay.capturePixels()` de la parte exacta donde dejaste la página web.
- **Sistema de Reposo / Hibernación (5 Minutos)**: Monitor no agresivo de inactividad. Si una pestaña permanece 5 minutos sin ser visitada, el sistema suspende la sesión de GeckoView para liberar memoria RAM y consumo de batería en el teléfono. Mantiene intactos el título, la URL y la miniatura con el distintivo `💤 En reposo (5 min)`, y al tocarla se despierta y restaura al instante.
- **Creación Manual en Incógnito y Protegidas**: Al entrar a los apartados de Incógnito o Protegidas ya no se crean pestañas vacías automáticamente. Se gestionan de forma manual y explícita por decisión del usuario.
- **Segmentación Tripartita**: Pestañas **Normales**, **Protegidas** (aisladas con escudo esmeralda y contextId) e **Incógnito** (sin registro en historial).
- **Persistencia de sesión**: Las pestañas abiertas se guardan en la base de datos local SQLite mediante Room v4 para que no se pierdan al cerrar la app.
- **Cierre selectivo o total con purga**: Cierra pestañas individuales o todas en bloque; las pestañas protegidas purgan de inmediato su contexto de cookies de GeckoView al eliminarse.

### ⭐ Marcadores y Favoritos
- Almacena tus páginas web favoritas con título, URL y fecha.
- Búsqueda en tiempo real dentro de marcadores.
- Posibilidad de añadir marcadores manualmente o directamente con un toque en el menú de navegación.

### 📜 Historial de Navegación
- Registro cronológico detallado de visitas en modo normal.
- Búsqueda instantánea por dominio o palabras clave.
- Limpieza individual de elementos o borrado completo de historial y caché.

### 👤 Gestión de Cuentas e Inicio de Sesión Web con un Toque
- **Vinculación de Cuentas:** Vincula tu cuenta de Google o cuenta personalizada directamente en la aplicación mediante `androidx.credentials` (`CredentialManager` y Google ID) de forma segura y nativa.
- **Detección Automática de Páginas de Autenticación (`WebSignInBridge`):** Al navegar por sitios web que cuenten con formularios de inicio de sesión o botones de "Iniciar sesión con Google", el navegador reconoce el contexto de autenticación en tiempo real.
- **Banner Flotante Interactivo (`WebSignInPromptBanner`):** Muestra un banner superior en Compose estilo One-Tap para acceder con la cuenta activa ("Continuar como [Nombre]").
- **Inyección y Autocompletado en GeckoView:** Al aceptar el banner, se evalúa JavaScript en la sesión activa de GeckoView para rellenar campos de usuario/correo o pulsar selectores estándar de Google Sign-In sin tener que teclear.
- **Pantalla Dedicada de Cuentas (`AccountsScreen`):** Administra múltiples perfiles, conmuta la cuenta activa, añade cuentas nuevas con un toque o elimina cuentas obsoletas con facilidad.

### 🛡️ Gestor de Permisos por Sitio Web y Políticas de Bloqueo Silencioso ("No Preguntar")
- **Control Granular por Dominio:** Visualización completa de todos los orígenes web que cuentan con permisos asignados (Cámara, Micrófono, Ubicación GPS, Notificaciones Web y Almacenamiento Persistente).
- **Conmutación Inmediata y Revocación:** Cambia al instante el estado de un permiso entre "Permitido" y "Bloqueado", revoca permisos individuales con un toque para que la web vuelva a preguntar, o elimina todos los permisos de un dominio en bloque.
- **Políticas Silenciosas de "No Preguntar":** Bloqueo automático a nivel de motor para silenciar solicitudes de Notificaciones, solicitudes de Ubicación y accesos a Cámara/Micrófono sin interrumpir la navegación con diálogos emergentes molestos.
- **Diálogos Nativos con Opción de Recordar:** Cuadro de diálogo nativo en Compose con checkbox "Recordar mi elección" integrado directamente con `GeckoSession.PermissionDelegate` y la base de datos local Room.
- **Pantalla Dedicada (`SitePermissionsScreen`):** Incluye barra de búsqueda por dominio, tarjetas visuales agrupadas por sitio, botón para restablecer todos los permisos y conmutadores rápidos de políticas globales.

### 🔔 Sistema de Efectos de Sonido Nativos y Retroalimentación Háptica
- **Reproducción sin Latencia (`SoundPool`):** Motor de sonido dedicado (`SoundEffectManager`) optimizado para audio PCM sin compresión (`.wav`) con cero sobrecarga de CPU y respuesta instantánea.
- **Tonos de Logro Aleatorios en Descargas:** Al completarse exitosamente una descarga de archivo, el navegador selecciona y reproduce de forma aleatoria una de las 6 variantes de tonos de logro creados a partir de *achievement chimes*, acompañados de una pulsación háptica sutil.
- **Control en Ajustes:** Interruptor maestro para activar o silenciar los efectos de sonido y botón interactivo para probar los tonos aleatorios en tiempo real.
- **Utilidad de Procesamiento de Audio (`tools/audio_processor.sh`):** Herramienta en bash y Python para convertir formatos de audio (WAV, MP3, OGG), extraer segmentos sin chasquidos mediante micro-desvanecimiento (`trim`), dividir archivos con detección de silencio (`split-silence`) y extraer paquetes de sonidos.
- **🔊 Próximamente:** Se incorporarán más bancos de efectos de sonido personalizables para añadir marcadores, cerrar pestañas, purgar datos y alertas de seguridad.

### ⚙️ Ajustes y Personalización
- Selector de motores de búsqueda predeterminados: DuckDuckGo, Google, Bing, Brave y Ecosia.
- Configuración de página de inicio personalizada o pantalla de inicio nativa (`about:home`).
- Control reactivo de privacidad, cookies y borrado completo de datos de navegación.
- Acceso directo a la configuración de Cuentas e Identidad Web.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología / Librería | Propósito |
| :--- | :--- | :--- |
| **Lenguaje Principal** | Kotlin 2.2+ | Desarrollo seguro, moderno y conciso para la interfaz y ciclo de vida |
| **Interfaz de Usuario** | Jetpack Compose + Material Design 3 | UI declarativa, temas adaptativos y componentes dinámicos |
| **Capa Nativa (C++)** | C++26 / C23 (NDK r28 LTS, CMake 3.31+) | Puente JNI `browser_native`, hashing y aceleración nativa por hardware |
| **Capa Nativa (Rust)** | Rust Edition 2024 (`core-native`) | Infraestructura base para filtrado de red de alto rendimiento, hashes y seguridad |
| **Persistencia Local** | Android Room Database v4 (SQLite) + DataStore | Almacenamiento reactivo de pestañas, historial, marcadores, descargas, cookies y cuentas de usuario |
| **Gestión de Identidad** | AndroidX Credential Manager + Google ID | Vinculación nativa de cuentas e inyección de sesiones web (`WebSignInBridge`) |
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

El repositorio cuenta con un flujo automatizado en `.github/workflows/build-debug.yml` con activación **exclusivamente manual** (`workflow_dispatch`):
- **Compilación Limpia (Sin Caché):** Descarga el código, instala Java JDK 17, Android NDK 28, CMake 3.31+, Rust 2024 y descarga las dependencias de GeckoView Omni de Mozilla sin utilizar cachés previas.
- **Generación Forzada de Firma:** Ejecuta `./generate_debug_keystore.sh` para generar un almacén de claves `debug.keystore` (RSA 2048 bits / PKCS12) desde cero de forma 100% desatendida y obligatoria.
- **Sincronización Directa P2P con Syncthing:** Tras compilar el APK de depuración, levanta un nodo Syncthing con certificado fijo y se conecta punto a punto con tu app **Syncthing-fork** en el teléfono, depositando automáticamente `app-debug.apk` en `/storage/emulated/0/Navegador/app-debug.apk` mediante la red P2P global con relays cifrados.
- **Artefacto de Respaldo:** Si la app de Syncthing en el teléfono no estuviera activa, el APK se guarda de forma segura como artefacto descargable en la pestaña Actions de GitHub.

### 🔐 Secreto Requerido en GitHub (Settings ➔ Secrets and variables ➔ Actions)

| Secreto | Descripción | Dónde obtenerlo |
| :--- | :--- | :--- |
| `PHONE_SYNCTHING_ID` | Device ID de tu teléfono en la app Syncthing-fork | En tu app: Menú ➔ *Mostrar ID de este dispositivo* (copiar código) |

### 📱 Configuración en Syncthing-fork (Teléfono)
1. **Carpeta compartida:** Etiqueta: `Navegador`, ID: `navegador-apk`, Ruta: `/storage/emulated/0/Navegador`.
2. **Dispositivo GitHub Actions:** Añadir dispositivo con ID:
   `IAXLEGX-HNWFEWZ-P4OQVFW-VBKMPQ2-ZJI6MX6-OE6YPBD-S4BC346-266H4AO`
   Nombre: `GitHub Actions`, y marcar la casilla de la carpeta `Navegador`.
