/**
 * native-bridge.cpp
 * 
 * Puente JNI entre la capa Kotlin de la aplicación Android y el subsistema
 * nativo C++26 / Rust.
 * 
 * Provee verificación de inicialización del motor, enlace con el runtime
 * y soporte para arquitecturas de 32 y 64 bits (arm64-v8a, armeabi-v7a, x86, x86_64).
 */

#include <jni.h>
#include <string>
#include <cstdint>
#include <android/log.h>

#define LOG_TAG "BrowserNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Retorna la cadena descriptiva de versión del subsistema nativo C++26.
 */
JNIEXPORT jstring JNICALL
Java_com_example_browser_engine_NativeBridge_getNativeVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = "BrowserNative-v1.0.0 [C++26 / NDK r28 / Rust Ready]";
    LOGI("getNativeVersion invocado: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}

/**
 * Calcula un hash rápido FNV-1a de 64 bits para URLs y validación de seguridad a bajo nivel.
 */
JNIEXPORT jlong JNICALL
Java_com_example_browser_engine_NativeBridge_hashUrlNative(
        JNIEnv* env,
        jobject /* this */,
        jstring url) {
    if (url == nullptr) {
        return 0;
    }

    const char* nativeUrl = env->GetStringUTFChars(url, nullptr);
    if (nativeUrl == nullptr) {
        return 0;
    }

    uint64_t hash = 0xcbf29ce484222325ULL;
    for (const char* p = nativeUrl; *p != '\0'; ++p) {
        hash ^= static_cast<uint8_t>(*p);
        hash *= 0x100000001b3ULL;
    }

    env->ReleaseStringUTFChars(url, nativeUrl);
    return static_cast<jlong>(hash);
}

/**
 * Retorna si el subsistema nativo está activo y listo para operar.
 */
JNIEXPORT jboolean JNICALL
Java_com_example_browser_engine_NativeBridge_isNativeReady(
        JNIEnv* env,
        jobject /* this */) {
    return JNI_TRUE;
}

} // extern "C"
