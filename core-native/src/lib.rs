//! # Core Native - Módulo de Alto Rendimiento en Rust
//!
//! Este módulo nativo provee operaciones críticas de rendimiento, seguridad de navegación,
//! cálculo rápido de hashes criptográficos, validación de URLs y el **Motor de Filtrado de Red Nativo**
//! potenciado por la biblioteca de alto rendimiento `adblock` (utilizada por Brave y Firefox).

use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::sync::{Mutex, OnceLock};

use adblock::engine::Engine;
use adblock::lists::{FilterSet, ParseOptions};
use serde::{Deserialize, Serialize};

/// Versión del motor nativo Rust
pub const CORE_NATIVE_VERSION: &str = "0.2.0-rust2024-adblock";

/// Estadísticas del motor de filtrado nativo exportables a JSON
#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct FilterStats {
    pub rules_count: usize,
    pub blocked_count: u64,
    pub allowed_count: u64,
    pub is_ready: bool,
    pub version: String,
}

/// Estado en memoria del motor de filtrado
pub struct NativeFilterState {
    pub filter_set: FilterSet,
    pub engine: Option<Engine>,
    pub rules_count: usize,
    pub blocked_count: u64,
    pub allowed_count: u64,
}

static FILTER_STATE: OnceLock<Mutex<NativeFilterState>> = OnceLock::new();

fn get_filter_state() -> &'static Mutex<NativeFilterState> {
    FILTER_STATE.get_or_init(|| {
        let mut filter_set = FilterSet::new(true);

        // Reglas base esenciales integradas para bloqueo inmediato de telemetría y publicidad
        let default_rules = [
            "||doubleclick.net^",
            "||google-analytics.com^",
            "||googlesyndication.com^",
            "||adservice.google.com^",
            "||facebook.com/tr^",
            "||outbrain.com^",
            "||taboola.com^",
            "||adnxs.com^",
            "||rubiconproject.com^",
            "||criteo.com^",
            "||scorecardresearch.com^",
            "||quantserve.com^",
            "||hotjar.com^",
            "||chartbeat.com^",
        ];

        let mut count = 0;
        for rule in &default_rules {
            filter_set.add_filter_list(rule, ParseOptions::default());
            count += 1;
        }

        let engine = Engine::from_filter_set(filter_set.clone(), true);

        Mutex::new(NativeFilterState {
            filter_set,
            engine: Some(engine),
            rules_count: count,
            blocked_count: 0,
            allowed_count: 0,
        })
    })
}

/// Retorna la versión y estado del módulo nativo Rust como cadena C.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_get_version() -> *const c_char {
    static VERSION_C: &[u8] = b"core-native-v0.2.0-rust2024-adblock\0";
    VERSION_C.as_ptr() as *const c_char
}

/// Calcula un hash FNV-1a de 64 bits ultra-rápido para URLs o reglas de filtrado de red.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_hash_url(input_ptr: *const c_char) -> u64 {
    if input_ptr.is_null() {
        return 0;
    }

    let c_str = unsafe { CStr::from_ptr(input_ptr) };
    let bytes = c_str.to_bytes();

    let mut hash: u64 = 0xcbf29ce484222325;
    for &byte in bytes {
        hash ^= byte as u64;
        hash = hash.wrapping_mul(0x100000001b3);
    }
    hash
}

/// Verifica a bajo nivel si un esquema de URL es seguro (https://).
#[unsafe(no_mangle)]
pub extern "C" fn core_native_is_secure_url(url_ptr: *const c_char) -> bool {
    if url_ptr.is_null() {
        return false;
    }

    let c_str = unsafe { CStr::from_ptr(url_ptr) };
    let bytes = c_str.to_bytes();

    bytes.starts_with(b"https://")
}

/// Inicializa el motor de filtrado nativo Rust.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_filter_init() -> bool {
    let state_lock = get_filter_state();
    if let Ok(state) = state_lock.lock() {
        state.engine.is_some()
    } else {
        false
    }
}

/// Añade reglas de filtrado en lote en formato estándar (EasyList / uBlock / ABP / Hosts)
/// y recompila el motor de consulta en memoria.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_filter_add_rules(rules_ptr: *const c_char) -> u32 {
    if rules_ptr.is_null() {
        return 0;
    }

    let c_str = unsafe { CStr::from_ptr(rules_ptr) };
    let Ok(rules_str) = c_str.to_str() else {
        return 0;
    };

    let state_lock = get_filter_state();
    let Ok(mut state) = state_lock.lock() else {
        return 0;
    };

    let mut added_lines = 0usize;
    for line in rules_str.lines() {
        let trimmed = line.trim();
        if !trimmed.is_empty() && !trimmed.starts_with('!') && !trimmed.starts_with('#') {
            state.filter_set.add_filter_list(trimmed, ParseOptions::default());
            added_lines += 1;
        }
    }

    state.rules_count += added_lines;
    state.engine = Some(Engine::from_filter_set(state.filter_set.clone(), true));

    state.rules_count as u32
}

/// Consulta si una petición de red dada debe ser bloqueada por el motor adblock.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_filter_should_block(
    url_ptr: *const c_char,
    source_url_ptr: *const c_char,
    request_type_ptr: *const c_char,
) -> bool {
    if url_ptr.is_null() {
        return false;
    }

    let url_str = match unsafe { CStr::from_ptr(url_ptr) }.to_str() {
        Ok(s) => s,
        Err(_) => return false,
    };

    let source_str = if source_url_ptr.is_null() {
        ""
    } else {
        match unsafe { CStr::from_ptr(source_url_ptr) }.to_str() {
            Ok(s) => s,
            Err(_) => "",
        }
    };

    let req_type_str = if request_type_ptr.is_null() {
        "other"
    } else {
        match unsafe { CStr::from_ptr(request_type_ptr) }.to_str() {
            Ok(s) => s,
            Err(_) => "other",
        }
    };

    let state_lock = get_filter_state();
    let Ok(mut state) = state_lock.lock() else {
        return false;
    };

    if let Some(engine) = &state.engine {
        let block_result = engine.check_network_urls(url_str, source_str, req_type_str);
        if block_result.matched {
            state.blocked_count += 1;
            true
        } else {
            state.allowed_count += 1;
            false
        }
    } else {
        false
    }
}

/// Retorna las estadísticas del motor de filtrado en formato JSON estructurado.
#[unsafe(no_mangle)]
pub extern "C" fn core_native_filter_get_stats() -> *const c_char {
    let state_lock = get_filter_state();
    let stats = if let Ok(state) = state_lock.lock() {
        FilterStats {
            rules_count: state.rules_count,
            blocked_count: state.blocked_count,
            allowed_count: state.allowed_count,
            is_ready: state.engine.is_some(),
            version: CORE_NATIVE_VERSION.to_string(),
        }
    } else {
        FilterStats {
            rules_count: 0,
            blocked_count: 0,
            allowed_count: 0,
            is_ready: false,
            version: CORE_NATIVE_VERSION.to_string(),
        }
    };

    let json_str = serde_json::to_string(&stats).unwrap_or_else(|_| "{}".to_string());
    let c_string = CString::new(json_str).unwrap_or_default();
    c_string.into_raw() as *const c_char
}
