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
- **Persistencia Reactiva:** Toda la información local (pestañas, historial, favoritos) se gestiona mediante Room Database con `StateFlow` y corrutinas de Kotlin.
- **Manejo de Idioma:** Las cadenas visibles al usuario se declaran en `res/values/strings.xml`. Documentación e información de commits se redactan en español.
