from __future__ import annotations

import hashlib
import json
import struct
import tempfile
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT.parent / "build" / "cifviewer-resourcepack.zip"
RESOLUTION = 9
PACK_FORMAT = 97

FACES = {
    "up": (0, 1, 0),
    "down": (0, -1, 0),
    "north": (0, 0, -1),
    "south": (0, 0, 1),
    "east": (1, 0, 0),
    "west": (-1, 0, 0),
}
UVS = {
    "up": [0, 0, 4, 4],
    "down": [8, 0, 12, 4],
    "north": [4, 0, 8, 4],
    "south": [4, 0, 8, 4],
    "east": [4, 0, 8, 4],
    "west": [4, 0, 8, 4],
}


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def build_texture() -> bytes:
    width = height = 16
    rows = []
    for y in range(height):
        row = bytearray()
        for x in range(width):
            if x < 4 and y < 4:
                color = (255, 255, 255)
            elif 4 <= x < 8 and y < 4:
                color = (212, 212, 212)
            elif 8 <= x < 12 and y < 4:
                color = (144, 144, 144)
            else:
                color = (0, 0, 0)
            row.extend(color)
        rows.append(b"\x00" + bytes(row))
    raw = b"".join(rows)
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
        + png_chunk(b"IDAT", zlib.compress(raw, 9))
        + png_chunk(b"IEND", b"")
    )


def build_model() -> dict:
    step = 16.0 / RESOLUTION
    radius = 8.0

    def offset(index: int) -> float:
        return (index + 0.5) * step - 8.0

    inside = set()
    for i in range(RESOLUTION):
        for j in range(RESOLUTION):
            for k in range(RESOLUTION):
                x, y, z = offset(i), offset(j), offset(k)
                if x * x + y * y + z * z <= radius * radius:
                    inside.add((i, j, k))

    elements = []
    for i, j, k in sorted(inside):
        faces = {}
        for face, (dx, dy, dz) in FACES.items():
            if (i + dx, j + dy, k + dz) in inside:
                continue
            faces[face] = {
                "uv": UVS[face],
                "texture": "#all",
                "tintindex": 0,
            }
        if not faces:
            continue
        elements.append(
            {
                "from": [round(i * step, 4), round(j * step, 4), round(k * step, 4)],
                "to": [round((i + 1) * step, 4), round((j + 1) * step, 4), round((k + 1) * step, 4)],
                "faces": faces,
            }
        )

    return {
        "textures": {
            "particle": "cifviewer:item/sphere",
            "all": "cifviewer:item/sphere",
        },
        "elements": elements,
    }


def build_stick_model() -> dict:
    faces = {
        face: {"uv": [0, 0, 16, 16], "texture": "#all", "tintindex": 0}
        for face in FACES
    }
    return {
        "textures": {
            "particle": "cifviewer:item/stick",
            "all": "cifviewer:item/stick",
        },
        "elements": [
            {
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "faces": faces,
            }
        ],
    }


def build_stick_texture() -> bytes:
    width = height = 16
    rows = []
    for _ in range(height):
        row = bytearray()
        for _ in range(width):
            row.extend((255, 255, 255))
        rows.append(b"\x00" + bytes(row))
    raw = b"".join(rows)
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
        + png_chunk(b"IDAT", zlib.compress(raw, 9))
        + png_chunk(b"IEND", b"")
    )


def build_item_definition(model: str) -> dict:
    return {
        "model": {
            "type": "minecraft:model",
            "model": f"cifviewer:item/{model}",
            "tints": [
                {
                    "type": "minecraft:dye",
                    "default": 16777215,
                }
            ],
        }
    }


def main() -> None:
    sphere_model = build_model()
    stick_model = build_stick_model()
    pack_meta = {
        "pack": {
            "pack_format": PACK_FORMAT,
            "description": "CIF Viewer 3D models",
        }
    }

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temporary:
        root = Path(temporary)
        files = {
            "pack.mcmeta": json.dumps(pack_meta, indent=2) + "\n",
            "assets/cifviewer/items/sphere.json": json.dumps(build_item_definition("sphere"), indent=2) + "\n",
            "assets/cifviewer/items/stick.json": json.dumps(build_item_definition("stick"), indent=2) + "\n",
            "assets/cifviewer/models/item/sphere.json": json.dumps(sphere_model, indent=2) + "\n",
            "assets/cifviewer/models/item/stick.json": json.dumps(stick_model, indent=2) + "\n",
        }
        for relative, content in files.items():
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
        texture_dir = root / "assets/cifviewer/textures/item"
        texture_dir.mkdir(parents=True, exist_ok=True)
        (texture_dir / "sphere.png").write_bytes(build_texture())
        (texture_dir / "stick.png").write_bytes(build_stick_texture())

        with zipfile.ZipFile(OUTPUT, "w", zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(root.rglob("*")):
                if path.is_file():
                    entry = zipfile.ZipInfo(
                        path.relative_to(root).as_posix(),
                        date_time=(1980, 1, 1, 0, 0, 0),
                    )
                    entry.compress_type = zipfile.ZIP_DEFLATED
                    entry.external_attr = 0o644 << 16
                    archive.writestr(entry, path.read_bytes())

    digest = hashlib.sha1(OUTPUT.read_bytes()).hexdigest()
    print(f"wrote {OUTPUT}")
    print(f"sphere elements: {len(sphere_model['elements'])}")
    print(f"sha1: {digest}")


if __name__ == "__main__":
    main()
