#!/usr/bin/env python3
"""Minimal RCON client used by deploy-main.sh.

Reads rcon.port / rcon.password from a server.properties file and runs the
command given on the command line against localhost.
"""

from __future__ import annotations

import os
import socket
import struct
import sys
from pathlib import Path

AUTH = 3
EXEC = 2
RESPONSE = 0


def encode(request_id: int, kind: int, body: str) -> bytes:
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def decode(sock: socket.socket) -> tuple[int, int, str]:
    raw_length = b""
    while len(raw_length) < 4:
        chunk = sock.recv(4 - len(raw_length))
        if not chunk:
            raise ConnectionError("RCON connection closed")
        raw_length += chunk
    (length,) = struct.unpack("<i", raw_length)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise ConnectionError("RCON connection closed")
        data += chunk
    request_id, kind = struct.unpack("<ii", data[:8])
    return request_id, kind, data[8:-2].decode("utf-8", "replace")


def load_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.exists():
        return values
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().replace("\\:", ":")
    return values


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: rcon.py <command>", file=sys.stderr)
        return 2
    command = " ".join(sys.argv[1:])
    properties_path = os.environ.get(
        "MC_RCON_PROPERTIES", "/opt/mc/servers/main/server.properties")
    properties = load_properties(Path(properties_path))
    if properties.get("enable-rcon", "false").lower() != "true":
        print("RCON is disabled", file=sys.stderr)
        return 1
    port = int(properties.get("rcon.port", "25575"))
    password = properties.get("rcon.password", "")

    with socket.create_connection(("127.0.0.1", port), timeout=10) as sock:
        sock.sendall(encode(1, AUTH, password))
        request_id, _, _ = decode(sock)
        if request_id == -1:
            print("RCON authentication failed", file=sys.stderr)
            return 1
        sock.sendall(encode(2, EXEC, command))
        _, _, body = decode(sock)
        if body:
            print(body)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
