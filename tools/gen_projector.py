#!/usr/bin/env python3
"""Kinoprojektor: erzeugt bbmodel, Texturen, geo.json und animation.json (siehe bbgen.py).
Aufruf: python tools/gen_projector.py
"""
from bbgen import Model

m = Model("projector", 128, 64)
cube, bone, octagram = m.cube, m.bone, m.octagram

# --- Gehaeuse (statisch) -----------------------------------------------------------------------------------
bone("body", None, (0, 0, 0))
cube("body", "base", -7, 0, -7, 14, 2, 15, "dark")
cube("body", "case", -5, 2, -3, 10, 7, 9, "case")
cube("body", "panel_l", -6, 3, -1, 1, 5, 6, "panel")
cube("body", "panel_r", 5, 3, -1, 1, 5, 6, "panel")
cube("body", "mount_plate", -3, 3.5, -4, 6, 6, 1, "dark")
cube("body", "gate", -2, 9, -2, 4, 1, 3, "steel")
for i, (bx, by) in enumerate([(-4.5, 8), (3.5, 8), (-4.5, 3), (3.5, 3)]):
    cube("body", f"bolt{i}", bx, by, -3.5, 1, 1, 0.5, "bolt")
# Lampenhaus (hohl, damit Luefter und Glut sichtbar bleiben)
cube("body", "lh_left", -3, 2, 6, 1, 9, 1, "case")
cube("body", "lh_right", 2, 2, 6, 1, 9, 1, "case")
cube("body", "lh_top", -2, 10, 6, 4, 1, 1, "case")
cube("body", "lh_bottom", -2, 2, 6, 4, 1, 1, "case")
cube("body", "lh_cap", -3, 11, 5, 6, 1, 3, "panel")
for i, y in enumerate([3.5, 6, 8.5]):
    cube("body", f"slat{i}", -2.5, y, 7, 5, 1, 1, "vent")
# Objektiv
cube("body", "lens_barrel", -2, 4.5, -7, 4, 4, 3, "dark")
cube("body", "ring_l", -2.5, 4, -8, 1, 5, 1, "brass")
cube("body", "ring_r", 1.5, 4, -8, 1, 5, 1, "brass")
cube("body", "ring_t", -1.5, 8, -8, 3, 1, 1, "brass")
cube("body", "ring_b", -1.5, 4, -8, 3, 1, 1, "brass")
cube("body", "lens_pane", -1.5, 5, -7.6, 3, 3, 0.5, "glass")
# Spulenhalter und statische Filmstreifen
cube("body", "post_front", -1, 9.5, -4, 2, 3, 1, "dark")
cube("body", "post_rear", -1, 9, 3, 2, 3.5, 1, "dark")
cube("body", "film_front", -1.5, 9, -5.1, 3, 1.9, 0.2, "film")
cube("body", "film_rear", -1.5, 9, 2.4, 3, 1.9, 0.2, "film")

# --- Linsen-/Lampenglut (leuchtend, animiert ueber Skalierung) ------------------------------------------------
bone("lamp_lens", "body", (0, 6.5, -7.6))
cube("lamp_lens", "lens_glow", -1.4, 5.1, -7.8, 2.8, 2.8, 0.2, "lens_glow")
bone("lamp_slits", "body", (0, 6.5, 6.1))
for i, y in enumerate([3.75, 6.25, 8.75]):
    cube("lamp_slits", f"slit{i}", -1.8, y, 6, 3.6, 0.5, 0.2, "lamp_glow")


def reel(bn, cz):
    cy = 12.5
    bone(bn, "body", (0, cy, cz))
    octagram(bn, bn + "_fl", -1.75, cy, cz, 0.5, 4.8, 4.8, "steel")
    octagram(bn, bn + "_fr", 1.75, cy, cz, 0.5, 4.8, 4.8, "steel")
    octagram(bn, bn + "_pk", 0, cy, cz, 3, 3.4, 3.4, "filmpack")
    cube(bn, bn + "_core", -2, cy - 0.5, cz - 0.5, 4, 1, 1, "brass")
    for side, x in (("l", -2.5), ("r", 2)):
        cube(bn, f"{bn}_sp{side}1", x, cy - 2.2, cz - 0.4, 0.5, 4.4, 0.8, "panel")
        cube(bn, f"{bn}_sp{side}2", x, cy - 0.4, cz - 2.2, 0.5, 0.8, 4.4, "panel")


reel("reel_supply", -3.5)
reel("reel_take", 3.5)
bone("sprocket_feed", "body", (0, 9.6, -5.4))
octagram("sprocket_feed", "spf", 0, 9.6, -5.4, 3, 1.6, 1.6, "brass")
bone("sprocket_take", "body", (0, 9.6, 3.3))
octagram("sprocket_take", "spt", 0, 9.6, 3.3, 3, 1.6, 1.6, "brass")
bone("gear_a", "body", (6.5, 6, 0.5))
octagram("gear_a", "ga", 6.5, 6, 0.5, 1, 3.4, 3.4, "brass")
bone("gear_b", "body", (6.5, 6, 4.2))
octagram("gear_b", "gb", 6.5, 6, 4.2, 1, 2.5, 2.5, "brass")
bone("fan", "body", (0, 6.5, 6.6))
cube("fan", "fan_hub", -0.5, 6, 6.4, 1, 1, 0.5, "steel")
cube("fan", "fan_b1", -2.2, 6.1, 6.4, 4.4, 0.8, 0.4, "fan")
cube("fan", "fan_b2", -0.4, 4.3, 6.4, 0.8, 4.4, 0.4, "fan")
cube("fan", "fan_b3", -2.2, 6.1, 6.4, 4.4, 0.8, 0.4, "fan", rot=[0, 0, 45])
cube("fan", "fan_b4", -2.2, 6.1, 6.4, 4.4, 0.8, 0.4, "fan", rot=[0, 0, -45])
bone("claw", "body", (0, 9.4, -1.2))
cube("claw", "claw", -0.5, 9.4, -1.8, 1, 1.5, 0.5, "brass")
bone("loop_front", "body", (0, 9.5, -6.3))
cube("loop_front", "loop_front", -1.5, 9, -6.4, 3, 1, 0.2, "film")
bone("loop_rear", "body", (0, 9.5, 5.2))
cube("loop_rear", "loop_rear", -1.5, 9, 5.1, 3, 1, 0.2, "film")

# --- Animationen ---------------------------------------------------------------------------------------------
# mechanism_spin: Tempo 1.0 = Spulen 0.5 U/s. Das Abspieltempo setzt der Code je Betriebszustand.
key = m.key
L = 2.0
spin = m.anim("mechanism_spin", L, "loop")
for bn in ("reel_supply", "reel_take"):
    key(spin, bn, "rotation", 0, [0, 0, 0]); key(spin, bn, "rotation", L, [-360, 0, 0])
for bn in ("sprocket_feed", "sprocket_take"):
    key(spin, bn, "rotation", 0, [0, 0, 0]); key(spin, bn, "rotation", L, [-720, 0, 0])
key(spin, "gear_a", "rotation", 0, [0, 0, 0]); key(spin, "gear_a", "rotation", L, [-1440, 0, 0])
key(spin, "gear_b", "rotation", 0, [0, 0, 0]); key(spin, "gear_b", "rotation", L, [1800, 0, 0])
key(spin, "fan", "rotation", 0, [0, 0, 0]); key(spin, "fan", "rotation", L, [0, 0, 2880])
CYCLES = 8
for i in range(CYCLES * 2 + 1):
    key(spin, "claw", "position", i * L / (CYCLES * 2), [0, 0.6 if i % 2 else 0, 0])
for i in range(5):
    key(spin, "loop_front", "scale", i * L / 4, [1, 1.5 if i % 2 else 1, 1])
    key(spin, "loop_rear", "scale", i * L / 4, [1, 1 if i % 2 else 1.5, 1])

EPS = 0.001
lamp_off = m.anim("lamp_off", 1.0, "hold_on_last_frame")
cool = m.anim("lamp_cooldown", 1.5, "hold_on_last_frame")
warm = m.anim("lamp_warmup", 2.0, "hold_on_last_frame")
run = m.anim("lamp_run", 0.5, "loop")
for bn in ("lamp_lens", "lamp_slits"):
    key(lamp_off, bn, "scale", 0, [EPS] * 3)
    for i in range(11):   # Hochfahren: zoegerliches Zuenden (quadratisch)
        f = i / 10
        key(warm, bn, "scale", f * 2.0, [max(EPS, f * f)] * 3)
    for i in range(7):    # Auslaufen: Glut verblasst schnell
        f = i / 6
        key(cool, bn, "scale", f * 1.5, [max(EPS, (1 - f) ** 2)] * 3)
    for t, s in ((0, 1.0), (0.125, 1.05), (0.25, 0.97), (0.375, 1.03), (0.5, 1.0)):
        key(run, bn, "scale", t, [s, s, s])

if __name__ == "__main__":
    m.write()
