#!/usr/bin/env bash
# ==============================================================================
# audio_processor.sh - Utilidad de Procesamiento, Conversión y División de Audio
# ==============================================================================
# Script en bash para convertir formatos de audio (WAV, MP3, OGG, etc.) y dividir
# archivos de audio que contienen múltiples sonidos o efectos (por ejemplo, chimes
# o packs de efectos de sonido) en archivos independientes listos para Android.
# ==============================================================================

set -euo pipefail

function print_help() {
    echo "Uso: $0 <comando> [argumentos...]"
    echo ""
    echo "Comandos disponibles:"
    echo "  convert <archivo_origen> <archivo_destino>"
    echo "      Convierte un audio a otro formato (ej: .wav a .mp3, .ogg o viceversa)."
    echo ""
    echo "  trim <archivo_origen> <inicio_segundos> <fin_segundos> <archivo_destino>"
    echo "      Extrae un fragmento de audio con micro-desvanecimiento (anti-clipping)."
    echo ""
    echo "  split-chimes <archivo_origen> <directorio_salida> [prefijo]"
    echo "      Divide el pack multi-sonido (achievment chimes) en 6 sonidos limpios individuales."
    echo ""
    echo "  split-silence <archivo_origen> <directorio_salida> [prefijo] [umbral_dB] [duracion_min_silencio]"
    echo "      Detecta silencios automáticamente y divide el audio en archivos independientes."
    echo ""
    echo "  info <archivo_audio>"
    echo "      Muestra la información técnica detallada del archivo de audio."
    echo ""
    echo "Ejemplos:"
    echo "  $0 convert sonido.wav sonido.ogg"
    echo "  $0 trim sonido.wav 1.05 2.10 chime_2.wav"
    echo "  $0 split-chimes tools/audio/635665__laurenponder__achievment-chimes.wav app/src/main/res/raw/ download_success"
    echo "  $0 info sonido.wav"
}

if [ $# -lt 1 ]; then
    print_help
    exit 1
fi

COMMAND="$1"
shift

case "$COMMAND" in
    convert)
        if [ $# -lt 2 ]; then
            echo "Error: Faltan argumentos. Uso: $0 convert <archivo_origen> <archivo_destino>"
            exit 1
        fi
        SRC="$1"
        DST="$2"
        echo "==> Convirtiendo '$SRC' a '$DST'..."
        ffmpeg -y -i "$SRC" "$DST"
        echo "==> Conversión finalizada con éxito."
        ;;

    trim)
        if [ $# -lt 4 ]; then
            echo "Error: Faltan argumentos. Uso: $0 trim <archivo_origen> <inicio> <fin> <archivo_destino>"
            exit 1
        fi
        SRC="$1"
        START="$2"
        END="$3"
        DST="$4"
        DUR=$(python3 -c "print(f'{$END - $START:.3f}')")
        OUT_FADE_START=$(python3 -c "print(f'{max(0, $END - $START - 0.02):.3f}')")

        echo "==> Extrayendo segmento de $START s a $END s (duración: $DUR s)..."
        ffmpeg -y -i "$SRC" -ss "$START" -to "$END" \
            -af "afade=t=in:ss=0:d=0.005,afade=t=out:st=$OUT_FADE_START:d=0.02" \
            "$DST"
        echo "==> Segmento guardado en '$DST'."
        ;;

    split-chimes)
        if [ $# -lt 2 ]; then
            echo "Error: Faltan argumentos. Uso: $0 split-chimes <archivo_origen> <directorio_salida> [prefijo]"
            exit 1
        fi
        SRC="$1"
        OUT_DIR="$2"
        PREFIX="${3:-download_success}"

        mkdir -p "$OUT_DIR"

        echo "==> Extrayendo los 6 tonos individuales de '$SRC' a '$OUT_DIR' con prefijo '$PREFIX'..."

        # Lista de segmentos identificados analíticamente: (inicio, fin, nombre_archivo)
        # 1: 0.05s a 0.95s
        ffmpeg -y -i "$SRC" -ss 0.05 -to 0.95 -af "afade=t=in:ss=0:d=0.005,afade=t=out:st=0.88:d=0.02" "${OUT_DIR}/${PREFIX}_1.wav"
        # 2: 1.05s a 2.10s
        ffmpeg -y -i "$SRC" -ss 1.05 -to 2.10 -af "afade=t=in:ss=0:d=0.005,afade=t=out:st=1.03:d=0.02" "${OUT_DIR}/${PREFIX}_2.wav"
        # 3: 2.18s a 2.80s
        ffmpeg -y -i "$SRC" -ss 2.18 -to 2.80 -af "afade=t=in:ss=0:d=0.005,afade=t=out:st=0.60:d=0.02" "${OUT_DIR}/${PREFIX}_3.wav"
        # 4: 2.85s a 3.65s
        ffmpeg -y -i "$SRC" -ss 2.85 -to 3.65 -af "afade=t=in:ss=0:d=0.005,afade=t=out:st=0.78:d=0.02" "${OUT_DIR}/${PREFIX}_4.wav"
        # 5: 3.68s a 4.60s
        ffmpeg -y -i "$SRC" -ss 3.68 -to 4.60 -af "afade=t=in:ss=0:d=0.005,afade=t=out:st=0.90:d=0.02" "${OUT_DIR}/${PREFIX}_5.wav"
        # 6: 4.65s a 6.35s
        ffmpeg -y -i "$SRC" -ss 4.65 -to 6.35 -af "afade=t=in:ss=0:d=0.005,afade=t=out:st=1.68:d=0.02" "${OUT_DIR}/${PREFIX}_6.wav"

        echo "==> ¡Extracción completada! Archivos generados:"
        ls -lh "${OUT_DIR}/${PREFIX}"_*.wav
        ;;

    split-silence)
        if [ $# -lt 2 ]; then
            echo "Error: Faltan argumentos. Uso: $0 split-silence <archivo_origen> <directorio_salida> [prefijo] [umbral_dB] [duracion_silencio]"
            exit 1
        fi
        SRC="$1"
        OUT_DIR="$2"
        PREFIX="${3:-sound}"
        NOISE_THRESHOLD="${4:--30dB}"
        MIN_SILENCE="${5:-0.2}"

        mkdir -p "$OUT_DIR"
        echo "==> Analizando silencios en '$SRC' (umbral: $NOISE_THRESHOLD, min: ${MIN_SILENCE}s)..."

        python3 - <<EOF
import subprocess
import os

src = "$SRC"
out_dir = "$OUT_DIR"
prefix = "$PREFIX"
threshold = "$NOISE_THRESHOLD"
min_sil = "$MIN_SILENCE"

# Obtener duración total
dur_cmd = ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", src]
total_dur = float(subprocess.check_output(dur_cmd).decode().strip())

# Detectar silencios
cmd = ["ffmpeg", "-i", src, "-af", f"silencedetect=noise={threshold}:d={min_sil}", "-f", "null", "-"]
res = subprocess.run(cmd, stderr=subprocess.PIPE, text=True)

silence_ranges = []
current_start = None

for line in res.stderr.splitlines():
    if "silence_start:" in line:
        current_start = float(line.split("silence_start:")[1].strip())
    elif "silence_end:" in line and current_start is not None:
        end_val = float(line.split("silence_end:")[1].split("|")[0].strip())
        silence_ranges.append((current_start, end_val))
        current_start = None

# Calcular segmentos de sonido
segments = []
pos = 0.0
for s_start, s_end in silence_ranges:
    if s_start > pos + 0.1:
        segments.append((pos, s_start))
    pos = s_end

if pos < total_dur - 0.1:
    segments.append((pos, total_dur))

print(f"==> Encontrados {len(segments)} segmentos de sonido.")
for idx, (st, en) in enumerate(segments, 1):
    out_file = os.path.join(out_dir, f"{prefix}_{idx}.wav")
    seg_dur = en - st
    fade_out = max(0, seg_dur - 0.02)
    cut_cmd = [
        "ffmpeg", "-y", "-i", src,
        "-ss", f"{st:.3f}", "-to", f"{en:.3f}",
        "-af", f"afade=t=in:ss=0:d=0.005,afade=t=out:st={fade_out:.3f}:d=0.02",
        out_file
    ]
    subprocess.run(cut_cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(f"    -> {out_file} [{st:.2f}s - {en:.2f}s] (duración: {seg_dur:.2f}s)")
EOF
        echo "==> División por silencio completada."
        ;;

    info)
        if [ $# -lt 1 ]; then
            echo "Error: Falta argumento. Uso: $0 info <archivo_audio>"
            exit 1
        fi
        ffprobe -hide_banner "$1"
        ;;

    *)
        echo "Error: Comando desconocido '$COMMAND'."
        print_help
        exit 1
        ;;
esac
