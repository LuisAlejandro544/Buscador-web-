#!/usr/bin/env bash
# ==============================================================================
# Script: generate_debug_keystore.sh
# Propósito: Generar un archivo debug.keystore de firma Android desde cero (100% nuevo)
#            de forma totalmente desatendida y obligatoria, sin esperar interacción
#            ni depender de claves preexistentes.
# ==============================================================================

set -euo pipefail

# Ruta de destino del keystore (por defecto en la raíz del proyecto)
OUTPUT_KEYSTORE="${1:-debug.keystore}"
ALIAS="androiddebugkey"
PASSWORD="android"
VALIDITY_DAYS=10000
KEY_SIZE=2048
DNAME="CN=Android Debug,O=Android,C=US"

echo "=================================================================="
echo "  Iniciando generación forzada de debug.keystore desde cero...    "
echo "=================================================================="

# 1. Eliminar cualquier keystore previo para garantizar creación limpia desde 0
if [ -f "$OUTPUT_KEYSTORE" ]; then
    echo "⚠️  Keystore existente detectado en '$OUTPUT_KEYSTORE'."
    echo "🗑️  Eliminando keystore anterior para forzar generación nueva..."
    rm -f "$OUTPUT_KEYSTORE"
fi

# 2. Comprobar disponibilidad de keytool
if ! command -v keytool &> /dev/null; then
    echo "❌ Error: La herramienta 'keytool' no está disponible en el PATH."
    echo "   Asegúrate de tener instalado Java JDK (OpenJDK 17/21)."
    exit 1
fi

# 3. Generar el par de claves y el almacén debug.keystore sin interacción
echo "🔐 Generando par de claves RSA $KEY_SIZE bits con alias '$ALIAS'..."
keytool -genkeypair \
    -v \
    -keystore "$OUTPUT_KEYSTORE" \
    -storetype PKCS12 \
    -storepass "$PASSWORD" \
    -alias "$ALIAS" \
    -keypass "$PASSWORD" \
    -keyalg RSA \
    -keysize "$KEY_SIZE" \
    -validity "$VALIDITY_DAYS" \
    -dname "$DNAME" \
    -noprompt

# 4. Validar que el archivo se haya creado correctamente
if [ -f "$OUTPUT_KEYSTORE" ]; then
    FILE_SIZE=$(wc -c < "$OUTPUT_KEYSTORE" | tr -d ' ')
    echo "✅ Keystore generado con éxito en: '$OUTPUT_KEYSTORE' ($FILE_SIZE bytes)"
    echo "📋 Verificando contenido del almacén de claves generado:"
    keytool -list -v -keystore "$OUTPUT_KEYSTORE" -storepass "$PASSWORD" -alias "$ALIAS" | grep -E "Alias|Tipo de entrada|Válido desde|Huellas digitales|Nombre del Propietario" || true
    echo "=================================================================="
    echo "  Firma de depuración lista para compilar el APK Debug.           "
    echo "=================================================================="
else
    echo "❌ Error crítico: No se pudo generar el archivo '$OUTPUT_KEYSTORE'."
    exit 1
fi
