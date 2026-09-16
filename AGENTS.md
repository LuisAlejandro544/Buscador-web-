# Reglas e Instrucciones para Agentes de IA (AGENTS.md)

Este documento contiene las directrices obligatorias y de comportamiento para cualquier agente de IA que opere en este repositorio.

---

## 🤖 Directivas Generales de Comportamiento

1. **Razonamiento Previo Obligatorio:**
   - Antes de realizar cualquier cambio, debes razonar a profundidad: qué herramientas se usarán, qué archivos serán modificados, qué dependencias intervienen y cómo impacta a la arquitectura global. No respondas ni actúes a ciegas.

2. **Perfil del Usuario y Dispositivo:**
   - El usuario trabaja exclusivamente desde un dispositivo móvil (teléfono). Redacta explicaciones directas, claras y bien estructuradas para lectura en pantallas pequeñas.

3. **Canal de Distribución:**
   - La aplicación está destinada a subirse a tiendas y repositorios de terceros (como **Uptodown** o APK directo), no a Google Play. No limites funcionalidades del navegador basándote en políticas restrictivas de Google Play si no aplican a distribución de terceros.

4. **Política de Dependencias y Tamaño del APK:**
   - Al usuario no le importa el peso del APK si las dependencias son 100% funcionales y de grado de producción.
   - Evita soluciones sin dependencias cuando exista una librería sólida, estándar y funcional.
   - No sugieras librerías que obliguen al proyecto a tener licencias víricas (como GPL estricto o que fuercen liberación de código privado/créditos forzados).

5. **Diseño de Interfaz y Usabilidad:**
   - **Prohibido el minimalismo extremo:** La aplicación debe lucir cuidada, con buen uso del color, tarjetas, iconos claros y retroalimentación táctil.
   - **Múltiples Pantallas Dedicadas:** No coloques todas las funciones agrupadas o amontonadas en una sola pantalla. Diseña pantallas separadas (Navegador, Pestañas, Marcadores, Historial, Ajustes) con botones y barras de navegación claras para moverse entre ellas.

6. **Calidad y Documentación del Código:**
   - Todo archivo de código fuente Kotlin debe incluir comentarios y explicaciones en español sobre la lógica que contiene y su propósito.
   - Mantén modularidad para que los archivos no sobrepasen las 500 líneas y la arquitectura sea limpia (MVVM + Room + Engine).

7. **Compatibilidad, Arquitecturas y Compilación Nativa:**
   - Garantiza soporte para arquitecturas de 32 y 64 bits (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
   - Mantén `useLegacyPackaging = true` en `app/build.gradle.kts` para permitir la correcta carga de librerías nativas C++ de GeckoView.
   - Respeta la versión mínima de Android (`minSdk = 29`) y analiza con cuidado antes de cualquier propuesta de modificación del SDK.
   - En compilaciones nativas de C++ (CMake/NDK) y Rust (Cargo), asegúrate de que `.gitignore` excluya rigurosamente todos los archivos generados, cachés de compilador, carpetas `target/`, `.cxx/`, `.ninja` y binarios intermedios.

8. **Archivos Protegidos y Convenciones:**

   - Nunca modifiques ni toques `debug.keystore` o `debug.keystore.base64`.
   - Si existe un archivo `commit_message.txt`, mantén siempre su información en español y modifícalo solo si el usuario lo solicita explícitamente.
   - Evita incluir nombres de marcas registradas que puedan comprometer legalmente al usuario.
