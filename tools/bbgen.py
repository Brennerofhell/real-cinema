"""Gemeinsame Bibliothek: Modell-DSL -> .bbmodel (Blockbench), .geo.json + .animation.json (GeckoLib), Texturen.

Koordinaten wie in Blockbench: x rechts, y oben, z nach hinten (Vorderseite = -z), Blockmitte x=0/z=0, y=0 ist der Boden.
Box-UV wird automatisch gepackt; Texturen sind prozedural (keine fremden Assets).
Geometrie-Export negiert wie Blockbench Pivot-x sowie Rotation x/y; Animationen werden unveraendert geschrieben
(beides gegen Blockbenchs eigenen Export verifiziert).
"""
import base64, json, math, os, random, uuid
from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

# Gemeinsame Materialpalette (einheitliche Gestaltung aller Geraete): Farbe, Rauschen, leuchtend
MATS = {
    "case": ((74, 92, 84), 7, False), "panel": ((98, 108, 110), 6, False), "dark": ((46, 48, 55), 5, False),
    "vent": ((26, 26, 30), 3, False), "brass": ((178, 140, 62), 8, False), "steel": ((150, 152, 160), 8, False),
    "glass": ((26, 38, 52), 3, False), "film": ((24, 20, 18), 3, False), "filmpack": ((46, 34, 26), 5, False),
    "bolt": ((128, 128, 134), 4, False), "fan": ((118, 120, 130), 5, False),
    "wood": ((120, 84, 52), 9, False), "popcorn": ((244, 228, 150), 6, False), "woodlight": ((150, 112, 72), 8, False),
    "cloth": ((128, 28, 36), 6, False), "canvas": ((226, 224, 214), 3, False),
    "cone": ((62, 62, 70), 4, False), "grille": ((60, 62, 68), 3, False),
    "btn_green": ((54, 160, 76), 4, False), "btn_yellow": ((214, 176, 48), 4, False), "btn_red": ((196, 52, 48), 4, False),
    "knob": ((196, 196, 202), 5, False), "label": ((214, 206, 180), 3, False),
    "lens_glow": ((255, 246, 205), 0, True), "lamp_glow": ((255, 196, 84), 0, True),
    "led_green": ((90, 255, 120), 0, True), "led_red": ((255, 80, 70), 0, True), "led_amber": ((255, 190, 60), 0, True),
    "laser": ((255, 90, 60), 0, True), "light_table": ((230, 240, 255), 0, True),
}
SHADE = {"up": 1.12, "north": 1.0, "south": 0.9, "east": 0.84, "west": 0.84, "down": 0.68}


def _clamp(v):
    return max(0, min(255, int(v)))


def _uid():
    return str(uuid.uuid4())


class Model:
    def __init__(self, name, tex_w=64, tex_h=64, kind="block", seed=7):
        self.name, self.tex_w, self.tex_h, self.kind, self.seed = name, tex_w, tex_h, kind, seed
        self.cubes, self.bones, self.anims = [], {}, {}

    # ---- Modell -----------------------------------------------------------------------------------------------
    def bone(self, name, parent=None, pivot=(0, 0, 0)):
        self.bones[name] = dict(parent=parent, pivot=list(pivot), order=len(self.bones))

    def cube(self, bone, name, x, y, z, sx, sy, sz, mat, rot=None):
        self.cubes.append(dict(name=name, bone=bone, frm=[x, y, z], size=[sx, sy, sz], mat=mat, rot=rot))

    def octagram(self, bone, prefix, cx, cy, cz, sx, sy, sz, mat, axis="x"):
        """Rundes Teil aus zwei um 45 Grad versetzten Quadern. axis = Drehachse (Dicke liegt auf dieser Achse)."""
        rot = {"x": [45, 0, 0], "y": [0, 45, 0], "z": [0, 0, 45]}[axis]
        self.cube(bone, prefix + "_a", cx - sx / 2, cy - sy / 2, cz - sz / 2, sx, sy, sz, mat)
        self.cube(bone, prefix + "_b", cx - sx / 2, cy - sy / 2, cz - sz / 2, sx, sy, sz, mat, rot=rot)

    # ---- Animation --------------------------------------------------------------------------------------------
    def anim(self, name, length, loop):
        """loop: 'loop' | 'once' | 'hold_on_last_frame'. Name wird zu animation.<model>.<name>."""
        full = f"animation.{self.name}.{name}"
        self.anims[full] = dict(length=length, loop=loop, bones={})
        return self.anims[full]

    @staticmethod
    def key(a, bone, channel, time, vec):
        a["bones"].setdefault(bone, {}).setdefault(channel, []).append((time, list(vec)))

    # ---- UV ---------------------------------------------------------------------------------------------------
    @staticmethod
    def _dims(c):
        sx, sy, sz = c["size"]
        ix, iy, iz = math.ceil(sx), math.ceil(sy), math.ceil(sz)
        return 2 * (ix + iz), iz + iy

    def pack(self):
        order = sorted(range(len(self.cubes)), key=lambda i: (-self._dims(self.cubes[i])[1], -self._dims(self.cubes[i])[0]))
        x = y = shelf = 0
        for i in order:
            w, h = self._dims(self.cubes[i])
            if x + w > self.tex_w:
                x, y, shelf = 0, y + shelf, 0
            if y + h > self.tex_h:
                raise SystemExit(f"[{self.name}] Textur {self.tex_w}x{self.tex_h} zu klein (Cube {self.cubes[i]['name']})")
            self.cubes[i]["uv"] = [x, y]
            x += w
            shelf = max(shelf, h)
        return y + shelf

    @staticmethod
    def face_rects(c):
        u, v = c["uv"]
        ix, iy, iz = (math.ceil(s) for s in (c["size"][0], c["size"][1], c["size"][2]))
        return {"east": (u, v + iz, iz, iy), "north": (u + iz, v + iz, ix, iy),
                "west": (u + iz + ix, v + iz, iz, iy), "south": (u + 2 * iz + ix, v + iz, ix, iy),
                "up": (u + iz, v, ix, iz), "down": (u + iz + ix, v, ix, iz)}

    def paint(self, overrides=None):
        rnd = random.Random(self.seed)
        mats = dict(MATS)
        mats.update(overrides or {})
        tex = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        glow = Image.new("RGBA", (self.tex_w, self.tex_h), (0, 0, 0, 0))
        tp, gp = tex.load(), glow.load()
        for c in self.cubes:
            base, noise, emissive = mats[c["mat"]]
            for face, (fx, fy, fw, fh) in self.face_rects(c).items():
                for px in range(fx, fx + fw):
                    for py in range(fy, fy + fh):
                        n = rnd.randint(-noise, noise)
                        r, g, b = (_clamp((ch + n) * SHADE[face]) for ch in base)
                        edge = fw > 3 and fh > 3 and (px in (fx, fx + fw - 1) or py in (fy, fy + fh - 1))
                        if edge and not emissive:
                            r, g, b = (_clamp(ch * 0.78) for ch in (r, g, b))
                        if c["mat"] == "film" and face in ("north", "south") and fh >= 2 and (px - fx) % 3 == 1 and (py - fy) in (0, fh - 1):
                            r, g, b = 150, 140, 120
                        if c["mat"] in ("vent", "grille") and (py - fy) % 2 == 1:
                            r, g, b = (_clamp(ch * 1.5) for ch in (r, g, b))
                        tp[px, py] = (r, g, b, 255)
                        if emissive:
                            gp[px, py] = (r, g, b, 255)
        return tex, glow

    # ---- Export -----------------------------------------------------------------------------------------------
    @staticmethod
    def face_uv(face, r, export=False):
        """[x1,y1,x2,y2]. Im Projekt einfache Rechtecke; Blockbenchs Geo-Export spiegelt up/down in beiden Achsen."""
        x1, y1, x2, y2 = r[0], r[1], r[0] + r[2], r[1] + r[3]
        if export and face in ("up", "down"):
            return [x2, y2, x1, y1]
        return [x1, y1, x2, y2]

    @staticmethod
    def thin(c):
        """Kanten unter 1 Einheit rendern im Box-UV unzuverlaessig -> Per-Face-UV."""
        return min(c["size"]) < 1

    @staticmethod
    def _center(c):
        return [c["frm"][i] + c["size"][i] / 2 for i in range(3)]

    def geo(self):
        out = []
        for bn, b in sorted(self.bones.items(), key=lambda kv: kv[1]["order"]):
            d = {"name": bn}
            if b["parent"]:
                d["parent"] = b["parent"]
            d["pivot"] = [-b["pivot"][0], b["pivot"][1], b["pivot"][2]]
            cs = []
            for c in (c for c in self.cubes if c["bone"] == bn):
                x, y, z = c["frm"]
                sx, sy, sz = c["size"]
                e = {"origin": [-(x + sx), y, z], "size": [sx, sy, sz], "uv": c["uv"]}
                if self.thin(c):
                    e["uv"] = {}
                    for f, r in self.face_rects(c).items():
                        u = self.face_uv(f, r, export=True)
                        e["uv"][f] = {"uv": [u[0], u[1]], "uv_size": [u[2] - u[0], u[3] - u[1]]}
                if c["rot"]:
                    cx, cy, cz = self._center(c)
                    e["pivot"] = [-cx, cy, cz]
                    e["rotation"] = [-c["rot"][0], -c["rot"][1], c["rot"][2]]
                cs.append(e)
            d["cubes"] = cs
            out.append(d)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.name}", "texture_width": self.tex_w, "texture_height": self.tex_h,
                            "visible_bounds_width": 3, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 0.75, 0]},
            "bones": out}]}

    @staticmethod
    def _t(t):
        return f"{t:.4f}".rstrip("0").rstrip(".") if t else "0"

    def animation_json(self):
        anims = {}
        for name, a in self.anims.items():
            bd = {}
            for bn, chans in a["bones"].items():
                bd[bn] = {}
                for ch, kfs in chans.items():
                    frames = {}
                    for t, v in kfs:
                        frames[self._t(t)] = [round(x, 4) + 0.0 for x in v]
                    # Blockbench schreibt eine einzelne Pose bei t=0 als schlichtes Array
                    bd[bn][ch] = list(frames.values())[0] if len(frames) == 1 and "0" in frames else frames
            entry = {"animation_length": a["length"], "bones": bd}
            if a["loop"] == "loop":
                entry["loop"] = True
            elif a["loop"] == "hold_on_last_frame":
                entry["loop"] = "hold_on_last_frame"
            anims[name] = entry
        return {"format_version": "1.8.0", "animations": anims}

    def bbmodel(self, textures):
        """textures: Liste (name, png_bytes). Alle Flaechen referenzieren Textur 0, weitere sind Varianten."""
        gid = {bn: _uid() for bn in self.bones}
        elements, cid = [], {}
        for i, c in enumerate(self.cubes):
            x, y, z = c["frm"]
            sx, sy, sz = c["size"]
            cid[id(c)] = _uid()
            faces = {f: {"uv": self.face_uv(f, r), "texture": 0} for f, r in self.face_rects(c).items()}
            el = {"name": c["name"], "box_uv": not self.thin(c), "rescale": False, "locked": False, "light_emission": 0,
                  "render_order": "default", "allow_mirror_modeling": True,
                  "from": [x, y, z], "to": [x + sx, y + sy, z + sz], "autouv": 0, "color": i % 8,
                  "origin": self._center(c) if c["rot"] else [0, 0, 0],
                  "uv_offset": c["uv"], "faces": faces, "type": "cube", "uuid": cid[id(c)]}
            if c["rot"]:
                el["rotation"] = c["rot"]
            elements.append(el)

        def node(bn):
            b = self.bones[bn]
            kids = [node(k) for k, v in sorted(self.bones.items(), key=lambda kv: kv[1]["order"]) if v["parent"] == bn]
            kids += [cid[id(c)] for c in self.cubes if c["bone"] == bn]
            return {"name": bn, "origin": b["pivot"], "color": 0, "uuid": gid[bn], "export": True, "mirror_uv": False,
                    "isOpen": False, "locked": False, "visibility": True, "autouv": 0, "children": kids}

        outliner = [node(bn) for bn, b in sorted(self.bones.items(), key=lambda kv: kv[1]["order"]) if not b["parent"]]
        anims = []
        for name, a in self.anims.items():
            animators = {}
            for bn, chans in a["bones"].items():
                kfs = [{"channel": ch, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "uuid": _uid(), "time": t,
                        "color": -1, "interpolation": "linear"} for ch, items in chans.items() for t, v in items]
                animators[gid[bn]] = {"name": bn, "type": "bone", "keyframes": kfs}
            loop = {"once": "once"}.get(a["loop"], a["loop"])
            anims.append({"uuid": _uid(), "name": name, "loop": loop, "override": False, "length": a["length"],
                          "snapping": 40, "selected": False, "anim_time_update": "", "blend_weight": "",
                          "start_delay": "", "loop_delay": "", "animators": animators})
        texs = []
        for idx, (tname, png) in enumerate(textures):
            texs.append({"path": "", "name": tname, "folder": "", "namespace": "", "id": str(idx), "particle": False,
                         "render_mode": "default", "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop",
                         "frame_order": "", "frame_interpolate": False, "visible": True, "internal": True, "saved": True,
                         "uuid": _uid(), "width": self.tex_w, "height": self.tex_h, "uv_width": self.tex_w,
                         "uv_height": self.tex_h, "source": "data:image/png;base64," + base64.b64encode(png).decode()})
        return {"meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
                "name": self.name, "model_identifier": self.name, "visible_box": [1, 1, 0],
                "variable_placeholders": "", "variable_placeholder_buttons": [], "timeline_setups": [],
                "unhandled_marker_colors": [], "resolution": {"width": self.tex_w, "height": self.tex_h},
                "elements": elements, "outliner": outliner, "textures": texs, "animations": anims}

    # ---- Schreiben --------------------------------------------------------------------------------------------
    @staticmethod
    def _write_json(path, obj, indent=2):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(obj, f, indent=indent)
            f.write("\n")

    def write(self, variants=None):
        """variants: {suffix: material-overrides}. Ohne Varianten entsteht <name>.png (+ Glowmask, falls leuchtend)."""
        used = self.pack()
        res = os.path.join(ROOT, "src", "main", "resources", "assets", "realcinema")
        bb = os.path.join(ROOT, "assets", "blockbench", self.name)
        tex_dir = os.path.join(res, "textures", self.kind)
        for d in (bb, tex_dir):
            os.makedirs(d, exist_ok=True)
        has_glow = any(MATS[c["mat"]][2] for c in self.cubes)   # GeckoLib stuerzt bei einer komplett transparenten Maske ab
        textures = []
        for suffix, ov in (variants or {"": None}).items():
            tex, glow = self.paint(ov)
            fname = f"{self.name}{('_' + suffix) if suffix else ''}"
            for d in (bb, tex_dir):
                tex.save(os.path.join(d, fname + ".png"))
                if has_glow:
                    glow.save(os.path.join(d, fname + "_glowmask.png"))
            with open(os.path.join(bb, fname + ".png"), "rb") as f:
                textures.append((fname + ".png", f.read()))
        self._write_json(os.path.join(bb, f"{self.name}.bbmodel"), self.bbmodel(textures), indent=None)
        self._write_json(os.path.join(res, "geo", self.kind, f"{self.name}.geo.json"), self.geo())
        self._write_json(os.path.join(res, "animations", self.kind, f"{self.name}.animation.json"), self.animation_json())
        print(f"{self.name}: {len(self.cubes)} Cubes, {len(self.bones)} Bones, {len(self.anims)} Animationen, "
              f"{len(textures)} Textur(en), UV bis y={used}/{self.tex_h}")
