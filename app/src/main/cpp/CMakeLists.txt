# ==============================================================================
# CMakeLists.txt - Compilación Nativa C++ (C++26 / C23) y Enlace con Rust
# ==============================================================================
# Proyecto: AI Studio Browser Core Native
# Soporte de arquitecturas: arm64-v8a, armeabi-v7a, x86, x86_64
# ==============================================================================

cmake_minimum_required(VERSION 3.22.1)

project("browser_native" LANGUAGES C CXX)

# Estándares modernos C++26 y C23 fijados en gradle/libs.versions.toml
set(CMAKE_CXX_STANDARD 26)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_C_STANDARD 23)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)

# Definición de la biblioteca compartida principal JNI
add_library(
    browser_native
    SHARED
    native-bridge.cpp
)

# Directorio de inclusión para cabeceras C++
target_include_directories(
    browser_native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Localización de librerías nativas estándar del NDK de Android
find_library(log-lib log)
find_library(android-lib android)

# Enlace de dependencias
target_link_libraries(
    browser_native
    PRIVATE
    ${log-lib}
    ${android-lib}
)
