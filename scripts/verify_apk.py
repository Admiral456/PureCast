#!/usr/bin/env python3
"""Smoke-check that packaged APKs retain PureCast's critical runtime features."""

from __future__ import annotations

import sys
import zipfile
from pathlib import Path

REQUIRED_DEX_STRINGS = (
    "/v1/join",
    "/v1/queue/add",
    "/v1/queue/remove",
    "/v1/queue/move",
    "/v1/queue/current",
    "/v1/leave",
    "/media/",
    "/tv/",
    "SetAVTransportURI",
    "GetPositionInfo",
    "GetTransportInfo",
    "Vyhledat televizory",
    "Moje televizory",
    "Uložit televizor",
    "Uložený televizor",
    "Teď nenalezen – zkus znovu vyhledat",
    "stable UPnP identifikátor",
    "Sdílená fronta",
    "Předchozí",
    "Další",
    "Pozastavit",
    "Pokračovat",
    "Zastavit",
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/apng",
    "image/heic",
    "image/heif",
    "image/avif",
    "image/jxl",
    "image/tiff",
    "image/svg+xml",
    "image/x-adobe-dng",
    "image/qoi",
    "image/x-exr",
    "image/ktx2",
    "image/fits",
    "image/x-canon-crw",
    "Televizor zobrazil originální obrázek",
    "Prohlížeč televize nedokázal zobrazit",
    "Síťové spojení se přerušilo. Obnovuji přehrávání",
    "Přehrávání se nepodařilo obnovit po několika pokusech",
    "Jednorázový kód pro PureCast",
    "Zadej tento kód v aplikaci PureCast v telefonu",
    "Spárovat televizor",
    "Televizor je spárovaný a připojený přes Wi-Fi.",
    "/control/",
    "Příliš mnoho chybných pokusů",
    "Naskenuj QR kód fotoaparátem telefonu",
    "Otevře se aplikace PureCast a televizor se spáruje automaticky",
    "purecast://pair",
    "/pair-qr/",
    "Galerie je zatím prázdná",
    "Fotky ",
    "Nejnovější",
    "Název A–Z",
    "Největší",
    "Nejmenší",
    "Všechna alba",
    'id="picture"',
    'data-mime="',
    "getAttribute('data-mime')",
)

REQUIRED_ABIS = {
    "armeabi-v7a",
    "arm64-v8a",
    "x86",
    "x86_64",
}


def verify_apk(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(f"APK does not exist: {path}")

    with zipfile.ZipFile(path) as apk:
        names = set(apk.namelist())
        if "AndroidManifest.xml" not in names:
            raise ValueError("AndroidManifest.xml is missing")
        if "resources.arsc" not in names:
            raise ValueError("Compiled Android resources are missing")

        # AAPT2 compiles vector and adaptive-icon XML into Android resources.
        # Their source XML files are not guaranteed to remain as ZIP entries.
        resource_table = apk.read("resources.arsc")
        required_icons = (
            b"ic_launcher",
            b"ic_launcher_round",
            b"ic_launcher_foreground",
            b"ic_launcher_monochrome",
        )
        missing_icons = [
            icon.decode("ascii")
            for icon in required_icons
            if icon not in resource_table
        ]
        if missing_icons:
            raise ValueError(
                "Compiled PureCast launcher icon resources are missing: "
                + ", ".join(missing_icons)
            )

        dex_names = sorted(
            name for name in names
            if name == "classes.dex"
            or (name.startswith("classes") and name.endswith(".dex"))
        )
        if not dex_names or "classes.dex" not in dex_names:
            raise ValueError("No primary DEX file found")

        dex_data = b"".join(apk.read(name) for name in dex_names)
        missing = [
            marker for marker in REQUIRED_DEX_STRINGS
            if marker.encode("utf-8") not in dex_data
        ]
        if missing:
            raise ValueError(
                "Critical application markers are missing from DEX: "
                + ", ".join(missing)
            )

        packaged_abis = {
            name.split("/")[1]
            for name in names
            if name.startswith("lib/") and name.endswith(".so")
            and len(name.split("/")) >= 3
        }
        if packaged_abis and not REQUIRED_ABIS.issubset(packaged_abis):
            absent = sorted(REQUIRED_ABIS - packaged_abis)
            raise ValueError("Missing native ABI libraries: " + ", ".join(absent))

        uncompressed_dex = sum(apk.getinfo(name).file_size for name in dex_names)
        compressed_dex = sum(apk.getinfo(name).compress_size for name in dex_names)

    print(
        f"PASS {path.name}: {path.stat().st_size:,} APK bytes; "
        f"{len(dex_names)} DEX file(s); "
        f"{uncompressed_dex:,} uncompressed DEX bytes; "
        f"{compressed_dex:,} compressed DEX bytes"
    )


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: verify_apk.py path/to/app.apk")
    verify_apk(Path(sys.argv[1]))
