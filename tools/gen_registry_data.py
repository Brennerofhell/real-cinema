#!/usr/bin/env python3
"""Erzeugt die datengetriebenen Registry-Dateien (Blockstates, Blockmodelle, Itemmodelle, Loot, Tags, Sprachdateien) fuer
alle Geraete und Items. Die Liste unten muss zu DeviceType.java und ModItems.java passen.
Aufruf: python tools/gen_registry_data.py
"""
import json, os

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
NS = "realcinema"

# id -> (englisch, deutsch)
BLOCKS = {
    "projector": ("Cinema Projector", "Kinoprojektor"),
    "film_cutter": ("Film Cutter", "Filmschneider"),
    "film_burner": ("Film Burner", "Filmbrenner"),
    "cinema_screen": ("Cinema Screen", "Kinoleinwand"),
    "speaker_wall": ("Wall Speaker", "Wandlautsprecher"),
    "speaker_floor": ("Floor Speaker", "Standlautsprecher"),
    "control_panel": ("Control Panel", "Bedienpult"),
    "cinema_seat": ("Cinema Seat", "Kinositz"),
    "popcorn_machine": ("Popcorn Machine", "Popcornmaschine"),
}
ITEMS = {
    "film_roll_empty": ("Empty Film Reel", "Leere Filmrolle"),
    "film_roll_written": ("Written Film Reel", "Beschriebene Filmrolle"),
    "film_roll_loaded": ("Loaded Film Reel", "Eingelegte Filmrolle"),
    "film_canister": ("Film Canister", "Filmdose"),
    "clapperboard": ("Clapperboard", "Filmklappe"),
}
BLOCK_DISPLAY = {"gui": {"rotation": [30, 225, 0], "scale": 0.6, "translation": [0, -1, 0]},
                 "ground": {"scale": 0.35}, "fixed": {"rotation": [0, 180, 0], "scale": 0.55},
                 "thirdperson_righthand": {"scale": 0.35}, "firstperson_righthand": {"scale": 0.35}}
# Items (Rollen, Dose, Klappe): Modelle sind gleich gross und um y=8 zentriert wie die Bloecke. Dieselbe Verschiebung wie bei
# Bloecken, nur etwas groesser skaliert, weil die Modelle kleiner sind. (Skala 1.0 ohne Verschiebung war zu gross und rutschte
# aus dem Slot nach unten links.)
ITEM_DISPLAY = {"gui": {"rotation": [25, 215, 0], "scale": 0.75, "translation": [0, -1, 0]},
                "ground": {"scale": 0.45, "translation": [0, 1, 0]},
                "fixed": {"rotation": [0, 180, 0], "scale": 0.7},
                "thirdperson_righthand": {"scale": 0.45}, "firstperson_righthand": {"scale": 0.45}}


MESSAGES = {
    "film_inserted": ("Film reel inserted", "Filmrolle eingelegt"),
    "projector_busy": ("The projector must be off and empty", "Der Projektor muss aus und leer sein"),
    "no_film": ("No film reel inserted", "Keine Filmrolle eingelegt"),
    "roll_inserted": ("Empty reel inserted", "Leere Rolle eingelegt"),
    "reference_set": ("Video reference set: %s", "Videoreferenz gesetzt: %s"),
    "burner_busy": ("The burner is busy or already loaded", "Der Brenner ist beschäftigt oder bereits beladen"),
    "need_roll": ("Insert an empty film reel first", "Zuerst eine leere Filmrolle einlegen"),
    "need_reference": ("Set a video reference first (use a renamed item)", "Zuerst eine Videoreferenz setzen (umbenannten Gegenstand benutzen)"),
    "roll_taken": ("Film reel taken", "Filmrolle entnommen"),
    "too_short": ("This film is too short to cut", "Dieser Film ist zu kurz zum Schneiden"),
    "film_cut": ("Film cut: two reels of %s s", "Film geschnitten: zwei Rollen à %s s"),
    "video_missing": ("Video file not found: %s (put it in realcinema_videos/)", "Videodatei nicht gefunden: %s (in realcinema_videos/ ablegen)"),
    "video_blocked": ("Video blocked: %s", "Video blockiert: %s"),
    "video_downloading": ("Downloading video...", "Video wird heruntergeladen ..."),
    "video_slow": ("This video decodes slower than real time. Use a smaller file (720p or less).", "Dieses Video wird langsamer als Echtzeit dekodiert. Eine kleinere Datei (720p oder weniger) verwenden."),
    "video_large": ("Large video (over 720p): it may stutter. 720p or less is recommended.", "Großes Video (über 720p): es kann ruckeln. Empfohlen sind 720p oder weniger."),
    "video_audio_pns": ("Audio track not readable (unsupported AAC feature). Re-encode: ffmpeg -c:a aac", "Tonspur nicht lesbar (nicht unterstützte AAC-Funktion). Neu kodieren: ffmpeg -c:a aac"),
    "video_error": ("Video error: %s", "Videofehler: %s"),
    "join_need_two": ("Hold a second reel in the other hand to join", "Zum Zusammenfügen eine zweite Rolle in die andere Hand nehmen"),
    "cannot_join": ("These reels do not belong together (other video or not adjacent)", "Diese Rollen gehören nicht zusammen (anderes Video oder nicht angrenzend)"),
    "film_joined": ("Film joined: %s s", "Film zusammengefügt: %s s"),
    "canister_full": ("The canister is full", "Die Filmdose ist voll"),
    "canister_empty": ("The canister is empty", "Die Filmdose ist leer"),
    "canister_need_roll": ("Hold a film reel in the other hand", "Eine Filmrolle in die andere Hand nehmen"),
    "no_panel_near": ("No control panel within 8 blocks", "Kein Bedienpult im Umkreis von 8 Blöcken"),
    "no_projector_ready": ("No projector with a film reel in range", "Kein Projektor mit Filmrolle in Reichweite"),
}
TOOLTIPS = {
    "film_reference": ("Reference: %s", "Referenz: %s"),
    "film_length": ("Length: %s s", "Länge: %s s"),
    "film_segment": ("Segment: %s", "Abschnitt: %s"),
    "canister_count": ("Reels: %s/%s", "Rollen: %s/%s"),
}


# JEI-Info-Seiten: id -> (englisch, deutsch)
INFO = {
    "projector": ("Shows a film reel on a cinema screen. Right-click opens its screen: reel slot, play, pause, stop and progress. Right-click with a written reel inserts it directly; sneak + right-click starts/stops quickly. It looks for a screen up to 32 blocks in front of it. A control panel or clapperboard also starts it.",
                  "Zeigt eine Filmrolle auf einer Kinoleinwand. Rechtsklick öffnet die Oberfläche: Rollenfach, Play, Pause, Stopp und Fortschritt. Rechtsklick mit beschriebener Rolle legt sie direkt ein; Schleichen + Rechtsklick startet/stoppt schnell. Sucht eine Leinwand bis 32 Blöcke vor sich. Auch per Bedienpult oder Filmklappe startbar."),
    "film_cutter": ("Right-click with a written reel to cut it in the middle into two reels (two parts of the same video). Sneak with a reel in each hand to join two adjoining parts again.",
                    "Rechtsklick mit beschriebener Rolle schneidet sie in der Mitte in zwei Rollen (zwei Abschnitte desselben Videos). Schleichen mit je einer Rolle in beiden Händen fügt zwei aneinanderschließende Abschnitte wieder zusammen."),
    "film_burner": ("1) Right-click with an empty reel. 2) Right-click with an item renamed in an anvil: the name is the video reference (file name or link). 3) Click with an empty hand to burn (5 s). 4) Click again to take the written reel.",
                    "1) Rechtsklick mit leerer Rolle. 2) Rechtsklick mit einem im Amboss umbenannten Gegenstand: Der Name ist die Videoreferenz (Dateiname oder Link). 3) Mit leerer Hand anklicken: brennt 5 s. 4) Erneut anklicken, um die beschriebene Rolle zu nehmen."),
    "cinema_screen": ("Place several side by side and stacked to form one large surface (up to 16x16). One click opens or closes the curtains of all connected blocks. Its front must face the projector.",
                      "Mehrere nebeneinander und übereinander ergeben eine große Fläche (bis 16x16). Ein Klick öffnet oder schließt die Vorhänge aller verbundenen Blöcke. Die Vorderseite muss zum Projektor zeigen."),
    "speaker_wall": ("Switched on by a control panel within 8 blocks while a film is playing. Click to toggle by hand.",
                     "Wird vom Bedienpult im Umkreis von 8 Blöcken eingeschaltet, solange ein Film läuft. Klick schaltet von Hand."),
    "speaker_floor": ("Switched on by a control panel within 8 blocks while a film is playing. Click to toggle by hand.",
                      "Wird vom Bedienpult im Umkreis von 8 Blöcken eingeschaltet, solange ein Film läuft. Klick schaltet von Hand."),
    "control_panel": ("Click: start all projectors with a reel within 8 blocks and switch on speakers; click again: pause; sneak-click: stop. It does nothing without a projector holding a reel. At the end of the film it resets itself.",
                      "Klick: startet alle Projektoren mit Rolle im Umkreis von 8 Blöcken und schaltet Lautsprecher ein; nochmal: Pause; Schleichen + Klick: Stopp. Ohne Projektor mit Rolle passiert nichts. Am Filmende setzt es sich selbst zurück."),
    "cinema_seat": ("Click to fold the seat up or down.", "Klick klappt den Sitz hoch oder runter."),
    "popcorn_machine": ("Click to start or stop popping.", "Klick startet oder stoppt das Poppen."),
    "film_roll_empty": ("Put it into a film burner to write a video reference onto it.", "In einen Filmbrenner legen, um eine Videoreferenz darauf zu schreiben."),
    "film_roll_written": ("Carries a video reference, a length and a start point. Insert it into a projector. Cut and join it at a film cutter.",
                          "Trägt Videoreferenz, Länge und Startpunkt. In einen Projektor einlegen. Am Filmschneider schneiden und zusammenfügen."),
    "film_roll_loaded": ("The written reel while it sits in a projector. It turns back into a written reel when ejected.",
                         "Die beschriebene Rolle, solange sie in einem Projektor steckt. Beim Auswerfen wird sie wieder zur beschriebenen Rolle."),
    "film_canister": ("Holds up to 8 reels. Right-click takes the last one out; sneak + right-click with a reel in the other hand puts it in. In the inventory it works like a bundle.",
                      "Fasst bis zu 8 Rollen. Rechtsklick nimmt die letzte heraus; Schleichen + Rechtsklick mit Rolle in der anderen Hand legt sie hinein. Im Inventar wie ein Beutel."),
    "clapperboard": ("Right-click claps (everyone sees and hears it) and operates the nearest control panel within 8 blocks: start; sneak + clap: stop.",
                     "Rechtsklick klappt (alle sehen und hören es) und bedient das nächste Bedienpult im Umkreis von 8 Blöcken: Start; Schleichen + Klappe: Stopp."),
}


# Oberflaechen: Schluessel (komplett) -> (englisch, deutsch)
GUI = {
    "container.realcinema.film_burner": ("Film Burner", "Filmbrenner"),
    "container.realcinema.film_cutter": ("Film Cutter", "Filmschneider"),
    "container.realcinema.control_panel": ("Control Panel", "Bedienpult"),
    "container.realcinema.projector": ("Cinema Projector", "Kinoprojektor"),
    "gui.realcinema.reference": ("Video reference", "Videoreferenz"),
    "gui.realcinema.reference_hint": ("file name or link", "Dateiname oder Link"),
    "gui.realcinema.burn": ("Burn", "Brennen"),
    "gui.realcinema.status_ready": ("ready", "bereit"),
    "gui.realcinema.status_burning": ("burning...", "brennt ..."),
    "gui.realcinema.status_done": ("done - take the reel", "fertig - Rolle nehmen"),
    "gui.realcinema.status_need_roll": ("insert an empty reel", "leere Rolle einlegen"),
    "gui.realcinema.play": ("Play", "Play"),
    "gui.realcinema.pause": ("Pause", "Pause"),
    "gui.realcinema.stop": ("Stop", "Stopp"),
    "gui.realcinema.join": ("Join A + B", "A + B zusammenfügen"),
    "gui.realcinema.projector_state.off": ("off", "aus"),
    "gui.realcinema.projector_state.starting": ("starting", "startet"),
    "gui.realcinema.projector_state.running": ("running", "läuft"),
    "gui.realcinema.projector_state.stopping": ("stopping", "läuft aus"),
    "gui.realcinema.screen_linked": ("screen: found", "Leinwand: ja"),
    "gui.realcinema.screen_unknown": ("screen: -", "Leinwand: -"),
    "gui.realcinema.panel_state.0": ("Stopped", "Gestoppt"),
    "gui.realcinema.panel_state.1": ("Playing", "Spielt"),
    "gui.realcinema.panel_state.2": ("Paused", "Pausiert"),
    "gui.realcinema.panel_projectors": ("Projectors with a reel: %s", "Projektoren mit Rolle: %s"),
    "gui.realcinema.panel_speakers": ("Speakers in range: %s", "Lautsprecher in Reichweite: %s"),
}

# Rezepte: id -> (Muster, Zutaten, Anzahl)
RECIPES = {
    "film_roll_empty": ([" N ", "NPN", " N "], {"N": "minecraft:iron_nugget", "P": "minecraft:paper"}, 2),
    "film_canister": (["NNN", "N N", "NNN"], {"N": "minecraft:iron_nugget"}, 1),
    "clapperboard": (["SSS", "BBB"], {"S": "minecraft:stick", "B": "minecraft:black_wool"}, 1),
    "projector": (["III", "GLR", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "L": "minecraft:redstone_lamp", "R": "minecraft:redstone"}, 1),
    "film_cutter": (["III", " S ", "PPP"], {"I": "minecraft:iron_ingot", "S": "minecraft:shears", "P": "minecraft:oak_planks"}, 1),
    "film_burner": (["III", "IRI", "IFI"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "F": "minecraft:furnace"}, 1),
    "cinema_screen": (["WWW", "WWW", "SSS"], {"W": "minecraft:white_wool", "S": "minecraft:stick"}, 1),
    "speaker_wall": (["III", "INI", "IRI"], {"I": "minecraft:iron_ingot", "N": "minecraft:note_block", "R": "minecraft:redstone"}, 1),
    "speaker_floor": (["IPI", "INI", "IRI"], {"I": "minecraft:iron_ingot", "P": "minecraft:oak_planks", "N": "minecraft:note_block", "R": "minecraft:redstone"}, 1),
    "control_panel": (["BBB", "RLR", "III"], {"B": "minecraft:stone_button", "R": "minecraft:redstone", "L": "minecraft:lever", "I": "minecraft:iron_ingot"}, 1),
    "cinema_seat": (["W  ", "WWW", "P P"], {"W": "minecraft:red_wool", "P": "minecraft:oak_planks"}, 1),
    "popcorn_machine": (["TTT", "GFG", "III"], {"T": "minecraft:red_terracotta", "G": "minecraft:glass", "F": "minecraft:furnace", "I": "minecraft:iron_ingot"}, 1),
}


def fix(display):
    """Minecraft verlangt fuer scale/rotation/translation Arrays; Skalare als [s, s, s] schreiben."""
    out = {}
    for k, v in display.items():
        out[k] = {kk: ([vv] * 3 if kk == "scale" and not isinstance(vv, list) else vv) for kk, vv in v.items()}
    return out


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main():
    for bid in BLOCKS:
        w(f"{RES}/assets/{NS}/blockstates/{bid}.json", {"variants": {"": {"model": f"{NS}:block/{bid}"}}})
        w(f"{RES}/assets/{NS}/models/block/{bid}.json", {"textures": {"particle": f"{NS}:block/{bid}"}})
        w(f"{RES}/assets/{NS}/models/item/{bid}.json", {"parent": "builtin/entity", "display": fix(BLOCK_DISPLAY)})
        w(f"{RES}/data/{NS}/loot_table/blocks/{bid}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}:{bid}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{bid}"})
    for iid in ITEMS:
        w(f"{RES}/assets/{NS}/models/item/{iid}.json", {"parent": "builtin/entity", "display": fix(ITEM_DISPLAY)})
    for rid, (pattern, key, count) in RECIPES.items():
        w(f"{RES}/data/{NS}/recipe/{rid}.json", {
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
            "key": {k: {"item": v} for k, v in key.items()}, "result": {"id": f"{NS}:{rid}", "count": count}})
    w(f"{RES}/data/minecraft/tags/block/mineable/pickaxe.json", {"values": [f"{NS}:{b}" for b in BLOCKS]})
    for lang, idx, tab in (("en_us", 0, "Real Cinema"), ("de_de", 1, "Real Cinema")):
        d = {f"block.{NS}.{b}": v[idx] for b, v in BLOCKS.items()}
        d.update({f"item.{NS}.{i}": v[idx] for i, v in ITEMS.items()})
        d[f"itemGroup.{NS}"] = tab
        d.update({f"message.{NS}.{k}": v[idx] for k, v in MESSAGES.items()})
        d.update({f"tooltip.{NS}.{k}": v[idx] for k, v in TOOLTIPS.items()})
        d.update({f"jei.{NS}.info.{k}": v[idx] for k, v in INFO.items()})
        d.update({k: v[idx] for k, v in GUI.items()})
        w(f"{RES}/assets/{NS}/lang/{lang}.json", d)
    print(f"{len(BLOCKS)} Bloecke, {len(ITEMS)} Items")


if __name__ == "__main__":
    main()
