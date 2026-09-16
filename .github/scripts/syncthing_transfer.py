#!/usr/bin/env python3
# ==============================================================================
# Script de transferencia P2P desatendida mediante Syncthing
# Transfiere app-debug.apk a la carpeta /storage/emulated/0/Navegador en Android
# ==============================================================================

import os
import sys
import time
import shutil
import subprocess
import urllib.request
import urllib.error
import json

RUNNER_DEVICE_ID = "IAXLEGX-HNWFEWZ-P4OQVFW-VBKMPQ2-ZJI6MX6-OE6YPBD-S4BC346-266H4AO"
API_KEY = "gh-actions-syncthing-secret-key"
GUI_PORT = 8384
FOLDER_ID = "navegador-apk"

def main():
    phone_device_id = os.environ.get("PHONE_SYNCTHING_ID", "").strip().upper()
    apk_src = os.environ.get("APK_SRC", "app/build/outputs/apk/debug/app-debug.apk")

    if not phone_device_id:
        print("ℹ️ Secreto PHONE_SYNCTHING_ID no configurado en GitHub Secrets.")
        print("ℹ️ Saltando sincronización P2P directa. El APK estará disponible en los Artefactos de GitHub.")
        sys.exit(0)

    if not os.path.exists(apk_src):
        print(f"❌ Error: No se encontró el APK en {apk_src}")
        sys.exit(1)

    apk_size_mb = os.path.getsize(apk_src) / (1024 * 1024)
    print("============================================================")
    print(f"🚀 Iniciando transferencia P2P con Syncthing ({apk_size_mb:.2f} MB)")
    print(f"📱 ID del Teléfono destino: {phone_device_id}")
    print(f"🤖 ID de GitHub Actions:     {RUNNER_DEVICE_ID}")
    print(f"📂 Carpeta destino:          Navegador ({FOLDER_ID})")
    print("============================================================")

    # 1. Preparar directorio compartido con el APK y el marcador .stfolder
    share_dir = "/tmp/syncthing_share"
    os.makedirs(share_dir, exist_ok=True)
    os.makedirs(os.path.join(share_dir, ".stfolder"), exist_ok=True)
    dest_apk = os.path.join(share_dir, "app-debug.apk")
    shutil.copy2(apk_src, dest_apk)

    # 2. Preparar directorio de configuración de Syncthing
    home_dir = "/tmp/syncthing_home"
    os.makedirs(home_dir, exist_ok=True)

    cert_path = os.path.join(".github", "syncthing", "cert.pem")
    key_path = os.path.join(".github", "syncthing", "key.pem")

    if os.path.exists(cert_path) and os.path.exists(key_path):
        shutil.copy2(cert_path, os.path.join(home_dir, "cert.pem"))
        shutil.copy2(key_path, os.path.join(home_dir, "key.pem"))
    else:
        print("❌ Error: No se encontraron cert.pem y key.pem en .github/syncthing/")
        sys.exit(1)

    # 3. Generar archivo config.xml optimizado para sincronización P2P
    config_xml = f"""<configuration version="37">
    <folder id="{FOLDER_ID}" label="Navegador" path="{share_dir}" type="sendonly" rescanIntervalS="2" fsWatcherEnabled="true" fsWatcherDelayS="1">
        <filesystemType>basic</filesystemType>
        <device id="{RUNNER_DEVICE_ID}"></device>
        <device id="{phone_device_id}"></device>
        <minDiskFree unit="%">1</minDiskFree>
        <markerName>.stfolder</markerName>
    </folder>
    <device id="{RUNNER_DEVICE_ID}" name="GitHub Actions" compression="metadata" introducer="false">
        <address>dynamic</address>
    </device>
    <device id="{phone_device_id}" name="Telefono-Usuario" compression="metadata" introducer="false" autoAcceptFolders="true">
        <address>dynamic</address>
    </device>
    <gui enabled="true" tls="false">
        <address>127.0.0.1:{GUI_PORT}</address>
        <apikey>{API_KEY}</apikey>
    </gui>
    <options>
        <listenAddress>default</listenAddress>
        <globalAnnounceEnabled>true</globalAnnounceEnabled>
        <localAnnounceEnabled>true</localAnnounceEnabled>
        <relaysEnabled>true</relaysEnabled>
        <relayReconnectIntervalM>1</relayReconnectIntervalM>
        <natEnabled>true</natEnabled>
        <reconnectionIntervalS>5</reconnectionIntervalS>
        <progressUpdateIntervalS>2</progressUpdateIntervalS>
        <startBrowser>false</startBrowser>
    </options>
</configuration>"""

    with open(os.path.join(home_dir, "config.xml"), "w") as f:
        f.write(config_xml)

    # 4. Iniciar demonio de Syncthing en segundo plano
    print("⏳ Levantando nodo P2P de Syncthing...")
    proc = subprocess.Popen(
        ["syncthing", "--no-browser", f"--home={home_dir}"],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True
    )

    # Esperar a que la API REST responda
    api_ready = False
    for _ in range(30):
        try:
            req = urllib.request.Request(
                f"http://127.0.0.1:{GUI_PORT}/rest/system/ping",
                headers={"X-API-Key": API_KEY}
            )
            with urllib.request.urlopen(req, timeout=2) as resp:
                if resp.status == 200:
                    api_ready = True
                    break
        except Exception:
            time.sleep(1)

    if not api_ready:
        print("⚠️ No se pudo iniciar el servicio Syncthing en el runner.")
        proc.terminate()
        sys.exit(0)

    print("✅ Nodo Syncthing iniciado. Buscando teléfono en la red P2P...")
    print("💡 Nota: Asegúrate de tener Syncthing-fork abierto en tu móvil.")

    max_seconds = 600  # 10 minutos máximo
    start_time = time.time()
    last_print = 0
    synced = False

    while time.time() - start_time < max_seconds:
        elapsed = int(time.time() - start_time)
        try:
            # Consultar conexiones activas
            conn_req = urllib.request.Request(
                f"http://127.0.0.1:{GUI_PORT}/rest/system/connections",
                headers={"X-API-Key": API_KEY}
            )
            with urllib.request.urlopen(conn_req, timeout=3) as c_resp:
                conns = json.loads(c_resp.read().decode("utf-8")).get("connections", {})
                phone_conn = conns.get(phone_device_id, {})
                is_connected = phone_conn.get("connected", False)

            if is_connected:
                # Consultar progreso de sincronización hacia el teléfono
                comp_url = f"http://127.0.0.1:{GUI_PORT}/rest/db/completion?device={phone_device_id}&folder={FOLDER_ID}"
                comp_req = urllib.request.Request(comp_url, headers={"X-API-Key": API_KEY})
                with urllib.request.urlopen(comp_req, timeout=3) as comp_resp:
                    comp_data = json.loads(comp_resp.read().decode("utf-8"))
                    completion = comp_data.get("completion", 0.0)
                    need_bytes = comp_data.get("needBytes", 1)

                if completion >= 100.0 or need_bytes == 0:
                    print("============================================================")
                    print("🎉 ¡Sincronización P2P completada al 100%!")
                    print("📲 El archivo app-debug.apk ya se encuentra en tu teléfono.")
                    print("📂 Ruta: /storage/emulated/0/Navegador/app-debug.apk")
                    print("============================================================")
                    synced = True
                    break
                else:
                    if elapsed - last_print >= 5:
                        print(f"📡 Teléfono conectado. Transfiriendo APK: {completion:.1f}% ({elapsed}s)")
                        last_print = elapsed
            else:
                if elapsed - last_print >= 10:
                    print(f"🔍 Esperando conexión P2P con el teléfono... ({elapsed}s transcurridos)")
                    last_print = elapsed

        except Exception as e:
            time.sleep(2)

        time.sleep(2)

    # Detener demonio
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except Exception:
        proc.kill()

    if not synced:
        print("============================================================")
        print("⏱️ Tiempo límite de sincronización alcanzado.")
        print("El APK se guardó de respaldo en los Artefactos de GitHub.")
        print("Verifica en tu teléfono:")
        print(" 1. Que Syncthing-fork esté abierto o activo.")
        print(" 2. Que el dispositivo 'GitHub Actions' esté aceptado en la app.")
        print("============================================================")

if __name__ == "__main__":
    main()
