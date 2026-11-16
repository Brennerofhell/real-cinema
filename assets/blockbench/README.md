# Blockbench-Assets

Alle 3D-Modelle der Mod. Bearbeitbare Quellen liegen hier, die exportierten Spieldateien unter
`src/main/resources/assets/realcinema/` (`geo/block`, `animations/block`, `textures/block`).

## Stand

Alle Modelle sind modelliert, texturiert, animiert und im Code registriert (Block/Item, Renderer, Sync). Video: siehe unten.

| Modell | Ordner | Cubes | Zustaende / Animationen | Bedienung (vorlaeufig) |
|---|---|---|---|---|
| Kinoprojektor | `projector/` | 70 | aus / hochfahren / laeuft / auslaufen; Lampe zuendet, flackert, verloescht | Rechtsklick |
| Filmschneider | `film_cutter/` | 34 | idle, cut (Klinge, auto 1.2 s), crank_turn (Kurbel + Spulen) | Klick: schneiden, Schleichen+Klick: Kurbel |
| Filmbrenner | `film_burner/` | 38 | idle, burning (5 s, Kopf faehrt, Laser, Spindel, LED), done | Klick: starten / quittieren |
| Kinoleinwand | `cinema_screen/` | 16 | Vorhang zu / auf (sanft ueberblendet) | Klick |
| Wand-/Standlautsprecher | `speaker_wall/`, `speaker_floor/` | 10 / 13 | aus / pulsierende Konen | Klick |
| Bedienpult | `control_panel/` | 19 | gestoppt / spielt / pausiert (Status-LEDs), Pegelanzeige | Klick: weiter, Schleichen: Stopp |
| Kinositz | `cinema_seat/` | 14 | Sitz unten / hochgeklappt | Klick |
| Popcornmaschine | `popcorn_machine/` | 30 | aus / poppt (Kessel wackelt, Popcorn springt, Lampen) | Klick |
| Filmrolle (3 Texturen) | `film_roll/` | 14 | idle, spin | Item |
| Filmdose | `film_canister/` | 9 | idle | Item |
| Filmklappe | `clapperboard/` | 9 | idle, clap (Rechtsklick, fuer alle sichtbar) | Item |

## Code-System (neues Geraet hinzufuegen)

1. Modell in `tools/gen_*.py` definieren, `python tools/gen_*.py` ausfuehren.
2. Eintrag in `DeviceType.java` (Modellname, Hitbox-Groesse, Zustaende mit Animationsnamen, Klick-Ziele, Auto-Timer).
3. Eintrag in `tools/gen_registry_data.py` (Namen) und `python tools/gen_registry_data.py`.

Bloecke, Items, Block-Entity, Renderer, Sync, Loot, Sprachdateien, Mining-Tag entstehen daraus ohne weiteren Code.
Alle Zustands-Animationen sind Posen oder Schleifen, GeckoLib blendet dazwischen. Dadurch spielt beim Laden einer Welt nichts
faelschlich ab. Der Projektor hat wegen des geschwindigkeitsgeregelten Hochlaufs eine eigene Klasse.

## Bedienung: Oberflaechen und Direktaktionen

Geraete mit Oberflaeche oeffnen sie per **Rechtsklick**. **Schleichen + Rechtsklick** ist die schnelle Direktaktion ohne Fenster.

| Geraet | Rechtsklick (Oberflaeche) | Schleichen + Rechtsklick | Mit Gegenstand in der Hand |
|---|---|---|---|
| Filmbrenner | Rollenfach, Textfeld "Videoreferenz" (Dateiname oder Link), Brennen-Knopf, Fortschrittsbalken | Brennen starten / fertige Rolle nehmen | Rechtsklick mit leerer Rolle legt sie ein, mit umbenanntem Gegenstand setzt die Referenz |
| Projektor | Rollenfach (nur wenn aus), Play / Pause / Stopp (Stopp spult zurueck), Fortschritt und Zeit, Leinwand gefunden? | schnell an/aus | beschriebene Rolle legt sie direkt ein |
| Bedienpult | Play / Pause / Stopp, Zustand, Zahl der Projektoren mit Rolle und der Lautsprecher in Reichweite | schnell stoppen | - |
| Filmschneider | zwei Rollenfaecher A und B, Schnitt bei 25 / 50 / 75 %, A + B zusammenfuegen; offene Rollen gehen beim Schliessen zurueck | Kurbel | Rechtsklick mit Rolle schneidet in der Mitte; Schleichen + zwei Rollen fuegt zusammen |
| Leinwand, Lautsprecher, Sitz, Popcornmaschine | - | - | Rechtsklick schaltet (Leinwand: alle verbundenen Bloecke) |

Server-seitig abgesichert: Das Menue gilt nur in Reichweite und solange der Block steht. Knoepfe und Referenzen werden vom Server
geprueft; das Textfeld schickt die Referenz per Paket (`SetReferencePayload`), das nur wirkt, wenn der Spieler genau dieses Menue
offen hat; Steuerzeichen werden entfernt, die Laenge auf 200 Zeichen begrenzt, waehrend des Brennens wird nichts geaendert.
Die Bildschirme zeichnen sich selbst (ohne Texturdatei, im Vanilla-Stil). Shift-Klick verschiebt Rollen zwischen Inventar und Faechern.

## Items

| Item | Funktion |
|---|---|
| Leere Filmrolle | Stapel bis 16. In den Filmbrenner legen. |
| Beschriebene Filmrolle | Traegt Videoreferenz, Laenge und Startpunkt (Abschnitt eines Videos). Tooltip zeigt alles. Einzeln (Stack 1). |
| Eingelegte Filmrolle | Dieselbe Rolle, solange sie im Projektor steckt (eigene Textur, schimmert). Beim Auswerfen oder Abbau wird sie wieder "beschrieben". |
| Filmdose | Behaelter fuer bis zu 8 Rollen. Rechtsklick: letzte Rolle herausnehmen. Schleichen + Rechtsklick mit Rolle in der anderen Hand: hineinlegen. Im Inventar wie ein Beutel (Rolle mit dem Cursor auf die Dose klicken, leerer Cursor auf die Dose = herausnehmen). Tooltip listet den Inhalt. |
| Filmklappe | Rechtsklick: klappt (Animation und Ton fuer alle) und bedient das naechste Bedienpult im Umkreis von 8 Bloecken ("Action!" = Start, Schleichen + Klappe = Stopp). 0,6 s Abklingzeit. |

## Spielablauf (implementiert, ungetestet im Spiel)

1. **Leere Rolle** craften, in den **Filmbrenner** legen (Rechtsklick mit Rolle).
2. **Videoreferenz** setzen: einen im Amboss umbenannten Gegenstand (z. B. Papier) auf den Brenner benutzen. Der Name ist die Referenz.
3. Brenner mit leerer Hand anklicken: brennt 5 s, danach erneut anklicken, um die **beschriebene Rolle** (Laenge 60 s, Start 0) zu entnehmen.
4. **Filmschneider** (Rolle in der Hand benutzen): **schneidet** in der Mitte in zwei Rollen, die jeweils den entsprechenden Abschnitt des Videos abspielen (das Video selbst wird nie veraendert, nur Startpunkt und Laenge).
   Schleichen + Rolle in beiden Haenden: **fuegt** zwei aneinanderschliessende Abschnitte desselben Videos wieder zusammen.
5. Rolle in den **Projektor** legen (Rechtsklick mit Rolle). Schleichen + leere Hand wirft sie aus (nur wenn aus).
6. **Bedienpult** in max. 8 Bloecken Abstand: Klick startet alle Projektoren mit Film und schaltet Lautsprecher ein, nochmal Klick = Pause, Schleichen = Stopp.
   Ohne Projektor mit Film passiert nichts (Hinweis in der Aktionsleiste). Am Filmende springt das Pult auf Stopp.

## Video auf der Leinwand

- **Leinwand:** Mehrere Leinwandbloecke mit gleicher Ausrichtung in einer senkrechten Ebene bilden eine Flaeche (bis 16x16).
  Rahmenkanten zu Nachbarn verschwinden, ein Klick schaltet den Vorhang aller verbundenen Bloecke. Der unterste linke Block
  (aus Sicht des Betrachters) zeichnet das Video fuer die ganze Flaeche, 16:9 eingepasst, Rest schwarz.
- **Zuordnung:** Beim Start sucht der Projektor die Leinwand vor sich (bis 32 Bloecke, seitlich +-6, vertikal -4..+8), die mit der
  Vorderseite zu ihm zeigt. Die Wahl wird synchronisiert.
- **Quelle der Videos (H.264 in MP4/MOV; MKV/WebM mit H.264 oder VP8 nur best effort):**
  - **Lokal:** Datei in `<spielordner>/realcinema_videos/` legen, als Referenz den **reinen Dateinamen** verwenden
    (z. B. `film.mp4` oder `film`). Pfade und Ordnertrenner werden abgelehnt.
  - **Link:** direkter `http(s)`-Link auf eine MP4-Datei, wird beim Abspielen heruntergeladen nach `<spielordner>/realcinema_cache/`.
- **Synchronisation:** Server schickt die Wiedergabeposition bei jeder Zustandsaenderung und alle 5 s; jeder Client rechnet seitdem
  weiter und holt per Seek auf, wenn er mehr als 0,5 s zurueck oder 2 s voraus liegt. Pause friert das Bild ein.
- **Dekodierung:** jcodec (reines Java, eingebettet), auf eigenem Thread, nur innerhalb `maxViewDistance` der Leinwand.
  Empfohlen sind 480p bis 720p mit H.264 Baseline/Main; grosse Videos (1080p und mehr) koennen langsamer sein als Echtzeit.
- **Hinweise:** bei fehlender Datei, gesperrtem Link oder Fehler erscheint eine Meldung in der Aktionsleiste.

### Sicherheit (Links)

Die Referenz auf einer Rolle stammt von einem anderen Spieler. Ein automatischer Download von dort wuerde die IP-Adresse aller
Zuschauer an einen fremden Server verraten und beliebig grosse Dateien ziehen. Deshalb ist `allowRemoteVideos` in der
Client-Config (`realcinema-client.toml`) **standardmaessig aus**; ein Link wird dann nur gemeldet, nie geladen. Ist er an, gilt
ein Groessenlimit (`maxDownloadMb`, Standard 256 MB). Lokale Dateien werden nie ausserhalb des Ordners `realcinema_videos` gelesen.

### Ton

Die AAC-Tonspur (MP4/MOV) wird ueber OpenAL als Positionston an der Leinwandmitte abgespielt, Lautstaerke ueber die
Minecraft-Kategorie "Plattenspieler/Musikbloecke", mit Abgleich zur Videoposition (Neuaufsetzen bei mehr als 0,35 s Abweichung).
Abschaltbar mit `enableAudio` in der Client-Config. Ein Fehler schaltet nur den Ton ab, nie das Bild.

**Dekoder:** jcodec zerlegt den Container, den AAC-Decoder liefert eine mitgelieferte, korrigierte Kopie von JAAD
(`ch.realcinema.vendor.jaad`, Public Domain, Aenderungen in `vendor/jaad/NOTICE.md`). Ohne diese Korrekturen waren Videos mit
ffmpegs Standard-Encoder stumm:

1. jcodecs MP4-Demuxer liefert AAC-Pakete mit ADTS-Kopf, der Decoder erwartet rohe Frames (Kopf wird entfernt).
2. JAADs PNS (Rauschersetzung) hatte einen Fehler im Zufallsgenerator (gerade Multiplikatorkonstante, statischer Zustand), der
   nach wenigen Baendern alles auf Stille setzte.
3. Die Tabelle der inversen Quantisierung war eine Stelle zu kurz (Absturz mit `-aac_coder fast`).

Geprueft mit Testdateien von ffmpeg (Standard-Encoder, Mono, Stereo, 44,1/48 kHz, `fast`-Coder): richtige Tonhoehen je Kanal
(440 Hz links, 880 Hz rechts) und derselbe Pegel wie ffmpeg selbst (RMS 0,353 bzw. 0,210 mit Rauschen), keine defekten Pakete.
Als GameTest abgesichert (`media_aac_with_pns_decodes_correctly`, Testdatei `data/realcinema/testdata/sample.mp4`, erzeugt von
`tools/gen_test_media.py`).

Erkennt das Spiel trotzdem eine Tonspur, die trotz Nutzlast zu Stille dekodiert (eine noch nicht unterstuetzte AAC-Funktion),
zeigt es einmal einen Hinweis und schreibt ihn ins Log; Behelf ist Umkodieren z. B. mit
`ffmpeg -i eingabe.mp4 -c:v copy -c:a aac -b:a 128k ausgabe.mp4`.

### Bekannte Probleme

- **Sehr grosse Videos** (ueber 720p) koennen langsamer als Echtzeit sein. Das Spiel meldet das (zu grosses Video bzw. dauerhaftes
  Hinterherhinken) in der Aktionsleiste.
- Nur AAC-LC (und was JAAD sonst kann) wurde getestet; HE-AAC (SBR/PS) ist nicht geprueft.

**Nicht implementiert:** Streaming-Seiten (YouTube o. ae.), andere Tonformate als AAC (MP3, Opus), Ton in MKV/WebM.

Rezepte liegen unter `data/realcinema/recipe/` und werden aus `tools/gen_registry_data.py` erzeugt.

## Pipeline

`tools/gen_projector.py`, `tools/gen_devices.py` und `tools/gen_extras.py` (gemeinsame Logik in `tools/bbgen.py`) erzeugen aus **einer** Modelldefinition alle Dateien, damit `.bbmodel`, Geo- und
Animationsexport nie auseinanderlaufen:

```
python tools/gen_projector.py
python tools/gen_devices.py
python tools/gen_extras.py
python tools/gen_registry_data.py
```

Ergebnis: `projector.bbmodel` (mit eingebetteter Textur), `projector.png`, `projector_glowmask.png`,
`projector.geo.json`, `projector.animation.json`.

Das `.bbmodel` ist normal in Blockbench editierbar. Nach Änderungen dort exportiert man im GeckoLib-/Bedrock-Format
(`File > Export > Export Bedrock Geometry` und `Export Animations`) und überschreibt die beiden Dateien im
Resource-Ordner. Wird stattdessen das Skript angepasst, überschreibt es die Blockbench-Datei wieder.

## Konventionen

- Texturauflösung 16 px pro Blockeinheit, Projektor 128×64 (Box-UV, automatisch gepackt).
- Vorderseite des Modells = -Z (Objektiv). Das Ausrichten übernimmt `HORIZONTAL_FACING` des Blocks.
- Leuchtende Flächen (Linse, Lampe) liegen zusätzlich in `<name>_glowmask.png` (GeckoLib `AutoGlowingGeoLayer`).
- Alle Texturen sind prozedural selbst erzeugt, keine fremden Assets.

## GeckoLib: Entscheidung

GeckoLib 4.9.3 (`software.bernie.geckolib:geckolib-neoforge-1.21.1`) wird verwendet. Vorteil gegenüber Vanilla-
Block-Entity-Renderern mit handgeschriebenen Rotationen:

- Blockbench-Animationen (Keyframes) laufen direkt im Spiel, ohne dass man sie in Java nachbauen muss.
- Die Abspielgeschwindigkeit eines Controllers ist zur Laufzeit regelbar. Der Projektor spielt seine
  Mechanik-Animation daher dauerhaft und ändert nur das Tempo (Hochlauf, Betrieb, Auslauf, Stillstand = Tempo 0).
  So bleibt die Spulenphase stetig, nichts springt beim Zustandswechsel zurück.
- Ein gebackenes Geo-Modell pro Gerät statt vieler einzelner Quads.

Nachteil: harte Abhängigkeit (muss als Mod installiert sein) und jeder animierte Block ist ein Block-Entity-Renderer
(kein Chunk-Mesh). Mitigation: Renderdistanz 48 Blöcke, nur ~70 Würfel pro Projektor.

## Multiplayer

Der Server hält nur den Zustand (`OFF/STARTING/RUNNING/STOPPING`) und sendet ihn per Block-Entity-Update. Jeder Client
leitet Tempo und Lampe daraus deterministisch ab (feste Beschleunigung pro Tick). Es werden keine Animationsdaten
übertragen. Wer später beitritt, sieht direkt den Endzustand.

## Validierung (tatsaechlich durchgefuehrt)

Gegen ein laufendes Blockbench 5.2.2 (GeckoLib-Plugin installiert) ueber den Blockbench-MCP-Server:

- Alle 12 `.bbmodel` laden fehlerfrei: 0 Warnungen, 0 Fehler des Blockbench-Validators.
- Blockbenchs eigener Bedrock-Geometrie-Export ist fuer alle Modelle **identisch** mit den hier abgelegten `.geo.json`
  (Bones, Pivots, Parent, Cubes, Rotationen, UV inkl. Per-Face-UV).
- Blockbenchs eigener Animations-Export ist inhaltlich identisch mit den `.animation.json`
  (Blockbench schreibt Animationen ohne Vorzeichenwechsel und faltet gleichmaessige Skalierung zu einer Zahl; wir
  schreiben dieselben Werte als Array, das GeckoLib ebenfalls liest).
- Blockbench-Renders (Offscreen-Ansicht) aus zwei Blickwinkeln pro Modell sichtgeprueft.

Headless geprueft: Mod und Registrierung laden auf einem NeoForge-Server ohne Fehler.
Nicht geprueft: Rendering und Animationswiedergabe im Spiel (Client wurde nicht gestartet), Netzwerk-Sync mit mehreren Spielern.

Cubes mit Kanten unter 1 Einheit nutzen Per-Face-UV (`meta.box_uv = false`, Box-UV je Cube gesetzt), weil Blockbench dort
im Box-UV degenerierte Flaechen erzeugt.

## Tests

`./gradlew runGameTestServer` startet einen Server ohne Fenster und fuehrt 33 GameTests aus (`src/main/java/ch/realcinema/test`).
Sie spielen die Logik durch (auch die Menues der Oberflaechen mit einem echten Server-Spieler): Registrierung, alle Zustandsautomaten, Filmbrenner (einlegen, Referenz, brennen, entnehmen),
Schneiden und Zusammenfuegen, Projektor (Einlegen, Start nur mit Film, Hochlauf, Filmende, Auslauf, Lampe), Bedienpult und
Filmklappe, Leinwand (Master, Abmessungen, gemeinsames Schalten, Zuordnung zum Projektor), Speichern/Laden, Filmdose, Rechtsklick
ueber den echten Block-Pfad, Droppen beim Abbau, Hitboxen. Die leere Strukturvorlage dafuer erzeugt `tools/gen_test_structure.py`.

**Nicht abgedeckt** (Client-Sache, braucht ein Fenster): Zeichnen von Modellen, Videobild und der Bildschirme (Layout, Knoepfe, Textfeld), Animationen, Ton, Mehrspieler-Sync.

## Testumgebung (mit JEI)

- `./gradlew runClient` startet Minecraft 1.21.1 mit der Mod, GeckoLib und **JEI 19.70** (nur in der Test-Laufzeit, keine Pflicht-
  Abhaengigkeit der Mod; im Spielordner `run/`). JEI zeigt alle Rezepte der Mod und zu jedem Geraet und Item eine **Info-Seite** mit der
  Bedienung (`RealCinemaJeiPlugin`, Texte in den Sprachdateien `jei.realcinema.info.*`).
- `./gradlew runGameTestServer` fuehrt die 33 GameTests aus (ebenfalls mit JEI in der Laufzeit).
- Testvideos: MP4 (H.264, AAC) nach `run/realcinema_videos/` legen, im Spiel die Referenz auf eine Filmrolle brennen.
  Die Referenz ist der Dateiname (Papier im Amboss umbenennen, im Filmbrenner benutzen).
- Schnellstart im Kreativmodus: `/give @s realcinema:projector`, dazu Leinwand, Bedienpult, Filmbrenner, leere Rolle (alles im
  Kreativ-Reiter "Real Cinema"). Die JEI-Info-Seite erklaert jeden Schritt.

- Leuchtende Flaechen (Linse, Lampe) liegen zusaetzlich in `<name>_glowmask.png` (GeckoLib `AutoGlowingGeoLayer`). **Eine Glowmaske darf nur existieren, wenn sie mindestens ein sichtbares Pixel hat:** GeckoLib stuerzt sonst beim Rendern ab (so geschehen im Kreativ-Inventar). Der Generator schreibt sie deshalb nur bei leuchtenden Wuerfeln, `DeviceType#glow()` muss dazu passen, und der GameTest `device_assets_are_consistent` prueft das.
