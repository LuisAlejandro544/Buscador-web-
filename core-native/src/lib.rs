//! # Core Native - Módulo de Alto Rendimiento en Rust
//!
//! Este módulo nativo provee la infraestructura inicial ("cultivar la tierra")
//! para operaciones críticas de rendimiento, seguridad de navegación,
//! cálculo rápido de hashes criptográficos y validación de URLs a bajo nivel.

use std::ffi::CStr;
use std::os::raw::c_char;

/// Versión del motor nativo Rust
pub const CORE_NATIVE_VERSION: &str = "0.1.0-rust2024";

/// Retorna la versión y estado del módulo nativo Rust como cadena C.
///
/// # Safety
/// Esta función exporta un puntero estático a una cadena C válida y terminada en nulo.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_get_version() -> *const c_char {
    static VERSION_C: &[u8] = b"core-native-v0.1.0-rust2024\0";
    VERSION_C.as_ptr() as *const c_char
}

/// Calcula un hash FNV-1a de 64 bits ultra-rápido para URLs o reglas de filtrado de red.
///
/// # Safety
/// El puntero `input_ptr` debe ser una cadena C terminada en null válida.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_hash_url(input_ptr: *const c_char) -> u64 {
    if input_ptr.is_null() {
        return 0;
    }

    let c_str = unsafe { CStr::from_ptr(input_ptr) };
    let bytes = c_str.to_bytes();

    // Algoritmo FNV-1a de 64 bits de alto rendimiento
    let mut hash: u64 = 0xcbf29ce484222325;
    for &byte in bytes {
        hash ^= byte as u64;
        hash = hash.wrapping_mul(0x100000001b3);
    }
    hash
}

/// Verifica a bajo nivel si un esquema de URL es seguro (https://).
///
/// # Safety
/// El puntero `url_ptr` debe ser una cadena C terminada en null válida.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_is_secure_url(url_ptr: *const c_char) -> bool {
    if url_ptr.is_null() {
        return false;
    }

    let c_str = unsafe { CStr::from_ptr(url_ptr) };
    let bytes = c_str.to_bytes();

    bytes.starts_with(b"https://")
}
