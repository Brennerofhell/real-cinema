#!/usr/bin/env python3
"""Erzeugt die kleine Testdatei fuer die Medien-GameTests: src/main/resources/data/realcinema/testdata/sample.mp4

1 Sekunde, 320x180, 25 fps, H.264 (x264, Main). Bild: linke Haelfte rot, rechte Haelfte gruen.
Ton: AAC-LC Stereo 44,1 kHz mit ffmpegs STANDARD-Encoder (also mit PNS etc.), 440 Hz links, 880 Hz rechts, Amplitude 0,5.

Braucht das Python-Paket 'av' (PyAV, bringt ffmpeg mit):  pip install av numpy
Aufruf: python tools/gen_test_media.py
"""
import math
import os

import av
import numpy as np

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "realcinema", "testdata", "sample.mp4")
W, H, FPS, SECS, RATE = 320, 180, 25, 1, 44100

os.makedirs(os.path.dirname(OUT), exist_ok=True)
c = av.open(OUT, "w")
v = c.add_stream("libx264", rate=FPS)
v.width, v.height, v.pix_fmt = W, H, "yuv420p"
v.options = {"profile": "main", "preset": "veryfast", "crf": "30"}
a = c.add_stream("aac", rate=RATE)
a.layout = "stereo"          # bewusst keine Encoder-Optionen: Standardverhalten von ffmpeg
for n in range(FPS * SECS):
    img = np.zeros((H, W, 3), dtype=np.uint8)
    img[:, : W // 2] = (200, 20, 20)
    img[:, W // 2:] = (20, 200, 20)
    for p in v.encode(av.VideoFrame.from_ndarray(img, format="rgb24")):
        c.mux(p)
for p in v.encode():
    c.mux(p)
t = np.arange(RATE * SECS) / RATE
pcm = np.stack([(0.5 * np.sin(2 * math.pi * 440 * t)).astype(np.float32),
                (0.5 * np.sin(2 * math.pi * 880 * t)).astype(np.float32)])
for i in range(0, pcm.shape[1] - 1024, 1024):
    fr = av.AudioFrame.from_ndarray(np.ascontiguousarray(pcm[:, i:i + 1024]), format="fltp", layout="stereo")
    fr.sample_rate = RATE
    fr.pts = i
    for p in a.encode(fr):
        c.mux(p)
for p in a.encode():
    c.mux(p)
c.close()
print("written", OUT, os.path.getsize(OUT), "bytes")
