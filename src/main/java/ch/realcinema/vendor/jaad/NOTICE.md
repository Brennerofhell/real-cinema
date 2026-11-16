# JAAD (vendored)

Pure-Java AAC decoder, Package `aac` aus **JAAD** (de.sfuhrm:jaad 0.8.7, https://github.com/DV8FromTheWorld/JAADec).
Lizenz: **Public Domain** (laut Projekt-POM). Das Paket wurde von `net.sourceforge.jaad.aac` nach
`ch.realcinema.vendor.jaad.aac` umbenannt, damit es nicht mit den JAAD-Klassen kollidiert, die in jcodec eingebettet sind.

## Aenderungen gegenueber dem Original

Nur `aac/syntax/ICStream.java` (PNS, Rauschersetzung):

1. `randomState` war `static` und damit fuer alle Kanaele, Decoder und Threads gemeinsam. Jetzt pro `ICStream`-Instanz.
2. Der Zufallsgenerator war `randomState *= 1664525+1013904223` (Multiplikation mit der **geraden** Zahl 1015568748). Dabei
   schiebt sich der Zustand nach wenigen Schritten auf 0 und bleibt dort; die Rauschenergie ist dann 0, die Skalierung NaN, und
   jedes PNS-Band klingt stumm. Korrekt ist der lineare Kongruenzgenerator `state = state*1664525 + 1013904223`.

3. Inverse Quantisierung: `IQ_TABLE` hat nur 8191 Eintraege, der Standard erlaubt Werte bis 8191. Der Original-Code warf bei
   solchen Spitzenwerten eine `ArrayIndexOutOfBoundsException` (aufgetreten mit ffmpeg `-aac_coder fast`). Werte ausserhalb der
   Tabelle werden jetzt als x^(4/3) berechnet.

Damit decodieren Dateien mit ffmpegs Standard-AAC-Encoder (der PNS einsetzt) richtig.

Hinweis zur Einbindung: jcodecs MP4-Demuxer liefert AAC-Pakete mit ADTS-Kopf; `AudioTrackReader` entfernt ihn vor dem Dekodieren.
