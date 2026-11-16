#!/usr/bin/env python3
"""Zusatzmodelle: Kinositz, Popcornmaschine, Filmdose (Item), Klappe (Item).
Aufruf: python tools/gen_extras.py   (siehe bbgen.py)
"""
import math
from bbgen import Model

EPS = 0.001
TW, TH = 128, 64


def pose(m, name, bone_vals, length=1.0):
    a = m.anim(name, length, "hold_on_last_frame")
    for bn, chans in bone_vals.items():
        for ch, v in chans.items():
            m.key(a, bn, ch, 0, v)
    return a


# ==================================================================================================================
# Kinositz (Klappsitz)
# ==================================================================================================================
def cinema_seat():
    m = Model("cinema_seat", TW, TH)
    m.bone("body", None, (0, 0, 0))
    m.cube("body", "foot", -6, 0, 1, 12, 1, 6, "dark")
    m.cube("body", "leg_l", -6, 1, 2, 1, 5, 3, "dark")
    m.cube("body", "leg_r", 5, 1, 2, 1, 5, 3, "dark")
    m.cube("body", "arm_l", -7, 7, -4, 1, 1, 8, "wood")
    m.cube("body", "arm_r", 6, 7, -4, 1, 1, 8, "wood")
    m.cube("body", "arm_post_l", -7, 4, -3, 1, 3, 1, "steel")
    m.cube("body", "arm_post_r", 6, 4, -3, 1, 3, 1, "steel")
    m.cube("body", "cup", 6, 8, -2, 1, 1, 3, "dark")
    m.cube("body", "backrest", -5, 5, 3, 10, 10, 3, "cloth")
    m.cube("body", "back_cap", -5, 15, 3, 10, 1, 3, "dark")
    m.cube("body", "number_plate", -1, 11, 6, 2, 1, 0.5, "brass")
    m.bone("seat", "body", (0, 4.5, 3))
    m.cube("seat", "cushion", -5, 4, -6, 10, 2, 9, "cloth")
    m.cube("seat", "lip", -5, 4, -7, 10, 1, 1, "cloth")
    m.cube("seat", "underside", -5, 3.5, -5, 10, 0.5, 8, "dark")
    pose(m, "seat_down", {"seat": {"rotation": [0, 0, 0]}})
    pose(m, "seat_up", {"seat": {"rotation": [80, 0, 0]}})
    a = m.anim("wiggle", 0.8, "once")   # optional: Sitz wackelt beim Hinsetzen
    for t, r in ((0, 0), (0.15, -4), (0.35, 3), (0.55, -1.5), (0.8, 0)):
        m.key(a, "seat", "rotation", t, [r, 0, 0])
    m.write()


# ==================================================================================================================
# Popcornmaschine
# ==================================================================================================================
def popcorn_machine():
    m = Model("popcorn_machine", TW, TH)
    m.bone("body", None, (0, 0, 0))
    m.cube("body", "cabinet", -6, 0, -5, 12, 6, 10, "btn_red")
    m.cube("body", "plinth", -6.5, 0, -5.5, 13, 1, 11, "dark")
    m.cube("body", "counter", -6.5, 6, -5.5, 13, 1, 11, "steel")
    for i, (x, z) in enumerate([(-6, -5), (5, -5), (-6, 4), (5, 4)]):
        m.cube("body", f"post{i}", x, 7, z, 1, 6, 1, "steel")
    m.cube("body", "pane_back", -5, 7, 4.5, 10, 6, 0.5, "glass")
    m.cube("body", "pane_l", -6, 7, -4, 0.5, 6, 8, "glass")
    m.cube("body", "pane_r", 5.5, 7, -4, 0.5, 6, 8, "glass")
    m.cube("body", "roof", -7, 13, -6, 14, 2, 12, "btn_red")
    m.cube("body", "roof_trim", -7, 12.5, -6, 14, 0.5, 12, "brass")
    m.cube("body", "sign", -4, 14, -6.5, 8, 2, 0.5, "label")
    m.cube("body", "knob_panel", -4, 2, -5.5, 8, 2, 0.5, "dark")
    m.cube("body", "scoop_tray", -5, 6.5, -7, 10, 0.5, 1.5, "steel")
    # Kessel (wackelt beim Poppen)
    m.bone("kettle", "body", (0, 11, 0))
    m.cube("kettle", "kettle", -2.5, 9.5, -1.5, 5, 2.5, 3, "steel")
    m.cube("kettle", "kettle_lid", -2, 12, -1, 4, 0.5, 2, "dark")
    m.cube("kettle", "kettle_handle", 2.5, 10.5, -0.5, 2, 0.5, 1, "dark")
    m.cube("body", "kettle_rod", -0.5, 12.5, -0.5, 1, 0.5, 1, "steel")
    # Popcorn (acht Bloecke, springen mit Phasenversatz)
    pos = [(-4, 7, -3), (-2, 7, 2), (0, 7, -2), (2, 7, 1), (4, 7, -3), (-3, 7, 0), (3, 7, 3), (1, 7, -4)]
    for i, (x, y, z) in enumerate(pos):
        m.bone(f"pc{i}", "body", (x, y, z))
        m.cube(f"pc{i}", f"pc{i}", x - 0.6, y, z - 0.6, 1.2, 1.2, 1.2, "popcorn")
    # Waermelampen (leuchten beim Poppen)
    m.bone("bulbs", "body", (0, 12.2, 0))
    m.cube("bulbs", "bulb_l", -5, 12, -3, 1.5, 0.5, 1.5, "lamp_glow")
    m.cube("bulbs", "bulb_r", 3.5, 12, 3, 1.5, 0.5, 1.5, "lamp_glow")
    m.bone("sign_glow", "body", (0, 15, -6.7))
    m.cube("sign_glow", "sign_glow", -3.5, 14.3, -6.7, 7, 1.4, 0.2, "led_amber")
    pose(m, "idle", {"bulbs": {"scale": [EPS] * 3}, "sign_glow": {"scale": [EPS] * 3}, "kettle": {"rotation": [0, 0, 0]},
                     **{f"pc{i}": {"position": [0, 0, 0]} for i in range(8)}})
    L = 1.0
    p = m.anim("popping", L, "loop")
    for i in range(8):
        ph = (i * 0.125) % L
        # Sprung: unten -> oben -> unten, mit Phasenversatz je Block
        times = sorted({round((ph + d) % L, 4) for d in (0, 0.15, 0.3)} | {0.0, L})
        for t in times:
            u = ((t - ph) % L)
            h = max(0.0, math.sin(math.pi * min(u / 0.3, 1.0))) * (1.8 + 0.2 * (i % 3))
            m.key(p, f"pc{i}", "position", t, [0, round(h, 3), 0])
    for t, r in ((0, 0), (0.1, 5), (0.2, -5), (0.3, 4), (0.4, -4), (0.5, 3), (0.6, -3), (0.7, 2), (0.8, -2), (0.9, 1), (1.0, 0)):
        m.key(p, "kettle", "rotation", t, [0, 0, r])
    for t, s in ((0, 1.0), (0.125, 1.1), (0.25, 0.95), (0.375, 1.08), (0.5, 1.0), (0.625, 1.1), (0.75, 0.95), (0.875, 1.05), (1.0, 1.0)):
        m.key(p, "bulbs", "scale", t, [s, s, s])
        m.key(p, "sign_glow", "scale", t, [1, s, 1])
    m.write()


# ==================================================================================================================
# Filmdose (Item)
# ==================================================================================================================
def film_canister():
    m = Model("film_canister", TW, TH, kind="item")
    m.bone("can", None, (0, 8, 0))
    m.octagram("can", "body", 0, 5, 0, 9, 7, 9, "steel", axis="y")
    m.octagram("can", "lid", 0, 9.25, 0, 9.6, 1.5, 9.6, "dark", axis="y")
    m.octagram("can", "rim", 0, 1.25, 0, 9.6, 1.5, 9.6, "dark", axis="y")
    m.cube("can", "handle", -2, 10.75, -0.5, 4, 1, 1, "brass")
    m.cube("can", "label", -2.5, 3, -5, 5, 3, 0.5, "label")
    m.cube("can", "band", -4, 6.2, -5, 8, 0.8, 0.5, "btn_red")
    a = m.anim("idle", 1.0, "loop")
    m.key(a, "can", "rotation", 0, [0, 0, 0])
    m.write()


# ==================================================================================================================
# Klappe (Item) mit Klapp-Animation
# ==================================================================================================================
def clapperboard():
    m = Model("clapperboard", TW, TH, kind="item")
    m.bone("slate", None, (0, 0, 0))
    m.cube("slate", "board", -6, 2, -0.5, 12, 8, 1, "dark")
    for i, y in enumerate([3, 5, 7]):
        m.cube("slate", f"line{i}", -5, y, -0.8, 10, 0.5, 0.5, "label")
    m.cube("slate", "handle", -6, 1, -0.5, 12, 1, 1, "wood")
    m.bone("clapper", "slate", (-6, 10, 0))
    for i in range(4):
        m.cube("clapper", f"stripe{i}", -6 + i * 3, 10, -1, 3, 2, 2, "label" if i % 2 == 0 else "dark")
    pose(m, "idle", {"clapper": {"rotation": [0, 0, 0]}})
    a = m.anim("clap", 0.6, "once")
    for t, r in ((0, 0), (0.15, 30), (0.4, 30), (0.45, 0), (0.5, 4), (0.6, 0)):
        m.key(a, "clapper", "rotation", t, [0, 0, r])
    m.write()


if __name__ == "__main__":
    for fn in (cinema_seat, popcorn_machine, film_canister, clapperboard):
        fn()
