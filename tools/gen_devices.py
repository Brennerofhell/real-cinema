#!/usr/bin/env python3
"""Alle Geraete ausser dem Projektor: Filmrolle, Filmschneider, Filmbrenner, Kinoleinwand, Lautsprecher, Bedienpult.
Aufruf: python tools/gen_devices.py
Jedes Modell erzeugt Blockbench-Projekt (.bbmodel), Texturen, GeckoLib-Geo und -Animationen (siehe bbgen.py).
"""
import math
from bbgen import Model

EPS = 0.001
TW, TH = 128, 64   # einheitliche Texturauflösung (16 px pro Blockeinheit)


def hold(m, name, bone_vals, length=1.0, loop="hold_on_last_frame"):
    """Statische Pose: bone_vals = {bone: {channel: [x,y,z]}}."""
    a = m.anim(name, length, loop)
    for bn, chans in bone_vals.items():
        for ch, v in chans.items():
            m.key(a, bn, ch, 0, v)
    return a


# ==================================================================================================================
# 1. Filmrolle (Item): leer / beschrieben / eingelegt = gleiche Geometrie, drei Texturen
# ==================================================================================================================
def film_roll():
    m = Model("film_roll", TW, TH, kind="item")
    m.bone("roll", None, (0, 8, 0))
    cy = 8
    m.octagram("roll", "fl_back", 0, cy, -1.75, 10, 10, 0.5, "steel", axis="z")
    m.octagram("roll", "fl_front", 0, cy, 1.75, 10, 10, 0.5, "steel", axis="z")
    m.octagram("roll", "pack", 0, cy, 0, 7.2, 7.2, 3, "filmpack", axis="z")
    m.cube("roll", "core", -1, cy - 1, -2.5, 2, 2, 5, "brass")
    for side, z in (("b", -2.5), ("f", 2)):
        m.cube("roll", f"spoke_{side}1", -4.4, cy - 0.4, z, 8.8, 0.8, 0.5, "panel")
        m.cube("roll", f"spoke_{side}2", -0.4, cy - 4.4, z, 0.8, 8.8, 0.5, "panel")
    m.cube("roll", "tag", -1.5, cy + 2.2, 2.5, 3, 1.5, 0.3, "bolt")        # farbiges Etikett (Zustand je Textur)
    m.cube("roll", "label", -2, cy - 3.6, 2.5, 4, 1.2, 0.3, "label")       # Beschriftungsfeld
    m.cube("roll", "tail", 3.2, cy - 4.2, -1.5, 0.2, 3, 3, "film")         # herausstehendes Filmende
    a = m.anim("idle", 1.0, "loop")
    m.key(a, "roll", "rotation", 0, [0, 0, 0])
    spin = m.anim("spin", 2.0, "loop")  # optional (z. B. im Projektor eingelegt)
    m.key(spin, "roll", "rotation", 0, [0, 0, 0]); m.key(spin, "roll", "rotation", 2.0, [0, 0, -360])
    m.write(variants={
        "empty":   {"filmpack": ((176, 164, 140), 5, False), "bolt": ((120, 120, 128), 4, False), "label": ((214, 206, 180), 3, False)},
        "written": {"filmpack": ((46, 34, 26), 5, False), "bolt": ((56, 112, 206), 4, False), "label": ((246, 246, 240), 3, False)},
        "loaded":  {"filmpack": ((46, 34, 26), 5, False), "bolt": ((60, 190, 92), 4, False), "label": ((246, 246, 240), 3, False)},
    })


# ==================================================================================================================
# 2. Filmschneider (Schneidetisch)
# ==================================================================================================================
def film_cutter():
    m = Model("film_cutter", TW, TH)
    m.bone("body", None, (0, 0, 0))
    m.cube("body", "top", -6, 8, -6, 12, 1, 12, "wood")
    m.cube("body", "shelf", -5, 2, -5, 10, 1, 10, "woodlight")
    for i, (x, z) in enumerate([(-6, -6), (5, -6), (-6, 5), (5, 5)]):
        m.cube("body", f"leg{i}", x, 0, z, 1, 8, 1, "dark")
    m.cube("body", "light_table", -4, 9, -2.5, 8, 0.3, 5, "light_table")
    m.cube("body", "film_strip", -6, 9.3, -1.5, 12, 0.2, 3, "film")
    m.cube("body", "rail_front", -6, 9.3, -2.6, 12, 0.6, 0.6, "steel")
    m.cube("body", "rail_back", -6, 9.3, 2, 12, 0.6, 0.6, "steel")
    m.cube("body", "post_l", -0.5, 9, -3, 1, 6, 1, "steel")
    m.cube("body", "post_r", -0.5, 9, 2, 1, 6, 1, "steel")
    m.cube("body", "beam", -0.5, 15, -3, 1, 1, 6, "steel")
    m.cube("body", "axle", 6, 5.5, -0.5, 1.5, 1, 1, "steel")
    # Schneidmechanismus
    m.bone("blade", "body", (0, 12, 0))
    m.cube("blade", "blade", -0.25, 11.5, -2.5, 0.5, 3, 5, "steel")
    m.cube("blade", "blade_edge", -0.25, 11.2, -2.5, 0.5, 0.3, 5, "bolt")
    m.cube("blade", "blade_handle", -0.5, 14.5, -0.5, 1, 1, 1, "btn_red")
    # Kurbel
    m.bone("crank", "body", (7.25, 6, 0))
    m.octagram("crank", "crank_wheel", 7.25, 6, 0, 0.5, 4.4, 4.4, "brass")
    m.cube("crank", "crank_handle", 7.5, 7.7, -0.4, 0.5, 0.8, 0.8, "btn_red")
    # Spulenhalter (liegende Spulen, drehen um y)
    for side, x in (("l", -4.5), ("r", 4.5)):
        bn = f"reel_{side}"
        m.bone(bn, "body", (x, 10.5, 3.9))
        m.octagram(bn, bn + "_lo", x, 9.45, 3.9, 4.2, 0.3, 4.2, "steel", axis="y")
        m.octagram(bn, bn + "_hi", x, 11.55, 3.9, 4.2, 0.3, 4.2, "steel", axis="y")
        m.octagram(bn, bn + "_pk", x, 10.5, 3.9, 3, 1.8, 3, "filmpack", axis="y")
        m.cube(bn, bn + "_core", x - 0.4, 9.3, 3.5, 0.8, 2.6, 0.8, "brass")
    # Animationen
    hold(m, "idle", {"blade": {"position": [0, 0, 0]}})
    cut = m.anim("cut", 1.2, "once")
    for t, y in ((0, 0), (0.15, 0.4), (0.45, -2.2), (0.6, -2.2), (1.1, 0), (1.2, 0)):
        m.key(cut, "blade", "position", t, [0, y, 0])
    crank = m.anim("crank_turn", 2.0, "loop")
    m.key(crank, "crank", "rotation", 0, [0, 0, 0]); m.key(crank, "crank", "rotation", 2.0, [-360, 0, 0])
    for bn, s in (("reel_l", -1), ("reel_r", 1)):
        m.key(crank, bn, "rotation", 0, [0, 0, 0]); m.key(crank, bn, "rotation", 2.0, [0, 360 * s, 0])
    m.write()


# ==================================================================================================================
# 3. Filmbrenner
# ==================================================================================================================
def film_burner():
    m = Model("film_burner", TW, TH)
    m.bone("body", None, (0, 0, 0))
    m.cube("body", "base", -6.5, 0, -6.5, 13, 1, 13, "dark")
    m.cube("body", "case", -6, 1, -6, 12, 7, 12, "case")
    m.cube("body", "top", -6, 8, -6, 12, 1, 12, "panel")
    m.cube("body", "front_strip", -5, 3, -6.5, 10, 3, 0.5, "dark")
    m.cube("body", "slot", -4, 5.2, -6.6, 5, 0.6, 0.2, "vent")
    m.cube("body", "label", -4.5, 1.5, -6.3, 4, 1, 0.3, "label")
    m.cube("body", "data_port", 6, 3, -1, 0.8, 2, 2, "brass")
    for i, x in enumerate([-3.5, 3.5]):
        m.cube("body", f"bolt{i}", x, 7, -6.2, 1, 1, 0.3, "bolt")
    # Brennbuehne: Film unter Portal, Brennkopf faehrt auf Schienen
    m.cube("body", "stage", -5.5, 9, -3, 6, 0.5, 6, "dark")
    m.cube("body", "stage_film", -5.5, 9.5, -0.75, 6, 0.2, 1.5, "film")
    m.cube("body", "guide_f", -5.5, 9.5, -1.5, 6, 0.5, 0.5, "steel")
    m.cube("body", "guide_b", -5.5, 9.5, 1, 6, 0.5, 0.5, "steel")
    m.cube("body", "gantry_f", -5.5, 11.5, -3.5, 6, 0.5, 0.5, "steel")
    m.cube("body", "gantry_b", -5.5, 11.5, 3, 6, 0.5, 0.5, "steel")
    m.cube("body", "gpost_fl", -5.5, 9.5, -3.5, 0.5, 2, 0.5, "steel")
    m.cube("body", "gpost_fr", -0.5, 9.5, -3.5, 0.5, 2, 0.5, "steel")
    m.cube("body", "gpost_bl", -5.5, 9.5, 3, 0.5, 2, 0.5, "steel")
    m.cube("body", "gpost_br", -0.5, 9.5, 3, 0.5, 2, 0.5, "steel")
    # Brennkopf (bewegt) mit Laser (leuchtet)
    m.bone("head", "body", (-3, 11, 0))
    m.cube("head", "carriage", -3.5, 11.2, -3.5, 1, 1.2, 7, "dark")
    m.cube("head", "emitter", -3.4, 10.2, -0.4, 0.8, 1, 0.8, "steel")
    m.bone("laser", "head", (-3, 10, 0))
    m.cube("laser", "laser_glow", -3.15, 9.7, -0.15, 0.3, 0.6, 0.3, "laser")
    # Spindel mit Rolle (dreht um y)
    m.bone("spindle", "body", (3, 10.5, 1))
    m.octagram("spindle", "sp_lo", 3, 9.3, 1, 4.8, 0.4, 4.8, "steel", axis="y")
    m.octagram("spindle", "sp_hi", 3, 11.7, 1, 4.8, 0.4, 4.8, "steel", axis="y")
    m.octagram("spindle", "sp_pk", 3, 10.5, 1, 3.4, 2, 3.4, "filmpack", axis="y")
    m.cube("spindle", "sp_core", 2.5, 9, 0.5, 1, 3, 1, "brass")
    # Luefter hinten
    m.bone("fan", "body", (0, 4.5, 6.2))
    m.cube("fan", "fan_hub", -0.5, 4, 6.0, 1, 1, 0.5, "steel")
    m.cube("fan", "fan_b1", -2.2, 4.1, 6.0, 4.4, 0.8, 0.4, "fan")
    m.cube("fan", "fan_b2", -0.4, 2.3, 6.0, 0.8, 4.4, 0.4, "fan")
    for i, y in enumerate([2.2, 4.2, 6.2]):
        m.cube("body", f"slat{i}", -2.5, y, 6.6, 5, 1, 0.8, "vent")
    # Status-LEDs (leuchtend, per Skalierung geschaltet)
    for bn, x, mat in (("led_ready", -3, "led_green"), ("led_busy", -1.8, "led_amber"), ("led_error", -0.6, "led_red")):
        m.bone(bn, "body", (x + 0.5, 3.9, -6.7))
        m.cube(bn, bn, x, 3.4, -6.8, 1, 1, 0.3, mat)
    # Animationen
    hold(m, "idle", {"led_ready": {"scale": [1, 1, 1]}, "led_busy": {"scale": [EPS] * 3},
                     "led_error": {"scale": [EPS] * 3}, "laser": {"scale": [EPS] * 3},
                     "head": {"position": [0, 0, 0]}}, loop="hold_on_last_frame")
    b = m.anim("burning", 3.0, "loop")
    m.key(b, "led_ready", "scale", 0, [EPS] * 3)
    m.key(b, "led_error", "scale", 0, [EPS] * 3)
    for i in range(7):   # LED blinkt, Laser flackert, Kopf faehrt hin und her
        m.key(b, "led_busy", "scale", i * 0.5, [1, 1, 1] if i % 2 == 0 else [0.2, 0.2, 0.2])
    for i in range(13):
        m.key(b, "laser", "scale", i * 0.25, [1, 1.4 if i % 2 else 1, 1])
    for t, x in ((0, -2), (1.5, 2), (3.0, -2)):
        m.key(b, "head", "position", t, [x, 0, 0])
    m.key(b, "spindle", "rotation", 0, [0, 0, 0]); m.key(b, "spindle", "rotation", 3.0, [0, -360, 0])
    m.key(b, "fan", "rotation", 0, [0, 0, 0]); m.key(b, "fan", "rotation", 3.0, [0, 0, 2160])
    done = m.anim("done", 1.0, "hold_on_last_frame")
    m.key(done, "led_ready", "scale", 0, [1, 1, 1])
    m.key(done, "led_busy", "scale", 0, [EPS] * 3)
    m.key(done, "laser", "scale", 0, [EPS] * 3)
    m.write()


# ==================================================================================================================
# 4. Kinoleinwand (modular: Kanten und Vorhaenge sind eigene Bones, der Code blendet sie je Nachbar aus)
# ==================================================================================================================
def cinema_screen():
    m = Model("cinema_screen", TW, TH)
    m.bone("backing", None, (0, 0, 0))
    m.cube("backing", "backing", -7, 1, 7, 14, 14, 1, "dark")
    m.bone("canvas", None, (0, 8, 6))
    m.cube("canvas", "canvas", -7, 1, 6, 14, 14, 1, "canvas")
    m.bone("edge_top", None, (0, 15.5, 6.5))
    m.cube("edge_top", "edge_top", -8, 15, 5, 16, 1, 3, "dark")
    m.bone("edge_bottom", None, (0, 0.5, 6.5))
    m.cube("edge_bottom", "edge_bottom", -8, 0, 5, 16, 1, 3, "dark")
    m.bone("edge_left", None, (-7.5, 8, 6.5))
    m.cube("edge_left", "edge_left", -8, 1, 5, 1, 14, 3, "dark")
    m.cube("edge_left", "trim_left", -7, 1, 5, 0.5, 14, 0.5, "brass")
    m.bone("edge_right", None, (7.5, 8, 6.5))
    m.cube("edge_right", "edge_right", 7, 1, 5, 1, 14, 3, "dark")
    m.cube("edge_right", "trim_right", 6.5, 1, 5, 0.5, 14, 0.5, "brass")
    for side, x0, sgn, pv in (("left", -7, 1, -7), ("right", 7, -1, 7)):
        bn = f"curtain_{side}"
        m.bone(bn, None, (pv, 8, 4.5))
        for i in range(4):
            x = x0 + sgn * i * 1.75 - (1.75 if sgn < 0 else 0)
            m.cube(bn, f"{bn}_{i}", x, 1, 4 + (0.5 if i % 2 else 0), 1.75, 14, 0.5, "cloth")
    hold(m, "curtain_closed", {"curtain_left": {"scale": [1, 1, 1]}, "curtain_right": {"scale": [1, 1, 1]}})
    hold(m, "curtain_opened", {"curtain_left": {"scale": [0.18, 1, 1.6]}, "curtain_right": {"scale": [0.18, 1, 1.6]}})
    op = m.anim("curtain_open", 1.5, "hold_on_last_frame")
    cl = m.anim("curtain_close", 1.5, "hold_on_last_frame")
    for bn in ("curtain_left", "curtain_right"):
        for i in range(7):
            f = i / 6
            ease = f * f * (3 - 2 * f)
            m.key(op, bn, "scale", f * 1.5, [1 - 0.82 * ease, 1, 1 + 0.6 * ease])
            m.key(cl, bn, "scale", f * 1.5, [0.18 + 0.82 * ease, 1, 1.6 - 0.6 * ease])
    m.write()


# ==================================================================================================================
# 5. Lautsprecher (Wand + Stand)
# ==================================================================================================================
def speaker_wall():
    m = Model("speaker_wall", TW, TH)
    m.bone("body", None, (0, 0, 0))
    m.cube("body", "cabinet", -4, 3, 2, 8, 10, 6, "dark")
    m.cube("body", "trim_top", -4.5, 12.5, 1.5, 9, 0.5, 6.5, "wood")
    m.cube("body", "trim_bottom", -4.5, 3, 1.5, 9, 0.5, 6.5, "wood")
    m.cube("body", "ring_woofer", -3.2, 4.0, 1.5, 6.4, 6.4, 0.5, "panel")
    m.cube("body", "port", -3, 3.6, 1.7, 6, 0.4, 0.3, "vent")
    m.bone("woofer", "body", (0, 7.2, 1.5))
    m.octagram("woofer", "wf_cone", 0, 7.2, 1.2, 4.4, 4.4, 0.6, "cone", axis="z")
    m.cube("woofer", "wf_cap", -0.8, 6.4, 0.9, 1.6, 1.6, 0.4, "steel")
    m.bone("tweeter", "body", (0, 11.2, 1.5))
    m.octagram("tweeter", "tw_dome", 0, 11.2, 1.3, 2.6, 2.6, 0.5, "brass", axis="z")
    hold(m, "idle", {"woofer": {"scale": [1, 1, 1]}, "tweeter": {"scale": [1, 1, 1]}})
    p = m.anim("pulse", 0.5, "loop")
    for t, z, tz in ((0, 1, 1), (0.125, 1.5, 1.2), (0.25, 1, 1), (0.375, 1.35, 1.25), (0.5, 1, 1)):
        m.key(p, "woofer", "scale", t, [1, 1, z])
        m.key(p, "tweeter", "scale", t, [1, 1, tz])
    m.write()


def speaker_floor():
    m = Model("speaker_floor", TW, TH)
    m.bone("body", None, (0, 0, 0))
    m.cube("body", "plinth", -4.5, 0, -3.5, 9, 1, 7, "dark")
    m.cube("body", "cabinet", -4, 1, -3, 8, 14, 6, "wood")
    m.cube("body", "top", -4.5, 15, -3.5, 9, 1, 7, "woodlight")
    m.cube("body", "baffle", -3.5, 1.5, -3.2, 7, 13, 0.2, "grille")
    m.cube("body", "ring_woofer", -3.4, 2.0, -3.5, 6.8, 6.8, 0.4, "panel")
    m.cube("body", "ring_mid", -2.3, 9.2, -3.5, 4.6, 4.6, 0.4, "panel")
    m.bone("woofer", "body", (0, 5.4, -3.5))
    m.octagram("woofer", "wf_cone", 0, 5.4, -3.7, 4.4, 4.4, 0.7, "cone", axis="z")
    m.cube("woofer", "wf_cap", -0.8, 4.6, -4.2, 1.6, 1.6, 0.4, "steel")
    m.bone("mid", "body", (0, 11.5, -3.5))
    m.octagram("mid", "md_cone", 0, 11.5, -3.7, 2.8, 2.8, 0.6, "cone", axis="z")
    m.cube("mid", "md_cap", -0.5, 11, -4.1, 1, 1, 0.3, "brass")
    m.cube("body", "port", -2.5, 0.2, -3.4, 5, 0.8, 0.4, "vent")
    hold(m, "idle", {"woofer": {"scale": [1, 1, 1]}, "mid": {"scale": [1, 1, 1]}})
    p = m.anim("pulse", 0.5, "loop")
    for t, z, mz in ((0, 1, 1), (0.125, 1.6, 1.2), (0.25, 1, 1), (0.375, 1.4, 1.3), (0.5, 1, 1)):
        m.key(p, "woofer", "scale", t, [1, 1, z])
        m.key(p, "mid", "scale", t, [1, 1, mz])
    m.write()


# ==================================================================================================================
# 6. Bedienpult
# ==================================================================================================================
def control_panel():
    m = Model("control_panel", TW, TH)
    TILT = -15.0
    slope = math.tan(math.radians(-TILT))   # y steigt mit z
    cy_panel, cz_panel = 6.4, -0.5

    def surface_y(z):   # Oberkante der schraegen Platte bei z (leicht ueberschaetzt, damit nichts einsinkt)
        return cy_panel + 0.45 / math.cos(math.radians(TILT)) + (z - cz_panel) * math.sin(math.radians(-TILT))

    m.bone("body", None, (0, 0, 0))
    m.cube("body", "base", -8, 0, -5, 16, 5, 10, "case")
    m.cube("body", "slope", -7.5, cy_panel - 0.4, cz_panel - 4.5, 15, 0.8, 9, "dark", rot=[TILT, 0, 0])
    m.cube("body", "riser", -8, 5, 3, 16, 6, 2, "case")
    m.cube("body", "label", -6, 1.5, -5.2, 12, 2, 0.3, "label")
    m.cube("body", "meter_window", -2.5, 8.6, 2.8, 8, 2.2, 0.2, "glass")
    m.cube("body", "speaker_grille", -7, 8.6, 2.8, 3.8, 2.2, 0.2, "grille")

    def button(bn, x, z, mat):
        y = surface_y(z)
        m.bone(bn, "body", (x, y, z))
        m.cube(bn, bn, x - 1.1, y, z - 1.1, 2.2, 0.7, 2.2, mat, rot=[TILT, 0, 0])

    button("btn_start", 6.2, -3.2, "btn_green")
    button("btn_pause", 3.4, -3.2, "btn_yellow")
    button("btn_stop", 0.6, -3.2, "btn_red")
    # Status-LEDs oberhalb der Tasten
    for bn, x, mat in (("led_play", 6.2, "led_green"), ("led_pause", 3.4, "led_amber"), ("led_stop", 0.6, "led_red")):
        y = surface_y(-0.4)
        m.bone(bn, "body", (x, y + 0.2, -0.4))
        m.cube(bn, bn, x - 0.5, y, -0.9, 1, 0.5, 1, mat, rot=[TILT, 0, 0])
    # Lautstaerkeregler
    ky = surface_y(-1.5)
    m.bone("knob_volume", "body", (-4.5, ky, -1.5))
    m.octagram("knob_volume", "knob", -4.5, ky + 0.5, -1.5, 3, 1, 3, "knob", axis="y")
    m.cube("knob_volume", "knob_mark", -4.7, ky + 1.0, -2.9, 0.4, 0.3, 1.4, "btn_red")
    # Pegelanzeige (vier Balken, von vorn gesehen links -> rechts gruen, gruen, gelb, rot)
    for i, (mat, h) in enumerate((("led_green", 1.6), ("led_green", 1.6), ("led_amber", 1.6), ("led_red", 1.6))):
        x = 3.8 - i * 1.8
        bn = f"bar{i + 1}"
        m.bone(bn, "body", (x + 0.6, 8.8, 2.7))
        m.cube(bn, bn, x, 8.8, 2.7, 1.2, h, 0.2, mat)
    # Animationen
    hold(m, "idle", {"led_play": {"scale": [EPS] * 3}, "led_pause": {"scale": [EPS] * 3}, "led_stop": {"scale": [1, 1, 1]},
                     "bar1": {"scale": [1, EPS, 1]}, "bar2": {"scale": [1, EPS, 1]},
                     "bar3": {"scale": [1, EPS, 1]}, "bar4": {"scale": [1, EPS, 1]}})
    hold(m, "status_playing", {"led_play": {"scale": [1, 1, 1]}, "led_pause": {"scale": [EPS] * 3}, "led_stop": {"scale": [EPS] * 3}})
    hold(m, "status_paused", {"led_play": {"scale": [EPS] * 3}, "led_pause": {"scale": [1, 1, 1]}, "led_stop": {"scale": [EPS] * 3}})
    hold(m, "status_stopped", {"led_play": {"scale": [EPS] * 3}, "led_pause": {"scale": [EPS] * 3}, "led_stop": {"scale": [1, 1, 1]}})
    for name in ("start", "pause", "stop"):
        a = m.anim(f"press_{name}", 0.3, "once")
        for t, y in ((0, 0), (0.1, -0.5), (0.3, 0)):
            m.key(a, f"btn_{name}", "position", t, [0, y, 0])
    k = m.anim("knob_turn", 1.0, "hold_on_last_frame")
    m.key(k, "knob_volume", "rotation", 0, [0, 0, 0]); m.key(k, "knob_volume", "rotation", 1.0, [0, -270, 0])
    hold(m, "meter_off", {f"bar{i}": {"scale": [1, EPS, 1]} for i in range(1, 5)})
    meter = m.anim("meter_active", 1.0, "loop")
    pattern = {"bar1": (1, 0.8, 1, 0.9, 1), "bar2": (0.7, 1, 0.6, 1, 0.7), "bar3": (0.3, 0.8, 0.4, 0.9, 0.3), "bar4": (EPS, 0.4, EPS, 0.5, EPS)}
    for bn, vals in pattern.items():
        for i, v in enumerate(vals):
            m.key(meter, bn, "scale", i * 0.25, [1, max(EPS, v), 1])
    m.write()


if __name__ == "__main__":
    for fn in (film_roll, film_cutter, film_burner, cinema_screen, speaker_wall, speaker_floor, control_panel):
        fn()
