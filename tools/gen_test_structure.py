#!/usr/bin/env python3
"""Erzeugt die leere Strukturvorlage fuer die GameTests: src/main/resources/data/realcinema/structure/empty.nbt
(25x16x25 Bloecke Luft mit einem Stein in der Ecke). Reines NBT, keine Abhaengigkeiten.
Aufruf: python tools/gen_test_structure.py
"""
import gzip
import os
import struct

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "realcinema", "structure", "empty.nbt")

END, INT, LIST, COMPOUND, STRING = 0, 3, 9, 10, 8


def name(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def tag_int(n, v):
    return bytes([INT]) + name(n) + struct.pack(">i", v)


def tag_string(n, v):
    return bytes([STRING]) + name(n) + name(v)


def tag_int_list(n, vals):
    return bytes([LIST]) + name(n) + bytes([INT]) + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals)


def tag_compound_list(n, items):
    """items: Liste fertig kodierter Compound-Inhalte (ohne fuehrenden Typ/Namen, mit End-Byte)."""
    return bytes([LIST]) + name(n) + bytes([COMPOUND]) + struct.pack(">i", len(items)) + b"".join(items)


def compound(*children):
    return b"".join(children) + bytes([END])


def build(size=(25, 16, 25)):
    palette = [compound(tag_string("Name", "minecraft:stone"))]
    blocks = [compound(tag_int_list("pos", [0, 0, 0]), tag_int("state", 0))]
    body = compound(
        tag_int("DataVersion", 3955),
        tag_int_list("size", list(size)),
        tag_compound_list("palette", palette),
        tag_compound_list("blocks", blocks),
        bytes([LIST]) + name("entities") + bytes([END]) + struct.pack(">i", 0),
    )
    return bytes([COMPOUND]) + name("") + body


if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "wb") as f:
        f.write(gzip.compress(build(), mtime=0))
    print("written", OUT)
