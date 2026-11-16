package ch.realcinema.video;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Dekodiert H.264 in MP4 (reines Java, jcodec) und skaliert jedes Bild mit schwarzen Balken auf eine feste
 * Zielgroesse. Enthaelt bewusst keine Minecraft-Klassen, damit es isoliert benutzt werden kann.
 * Nicht thread-sicher: ein Decoder gehoert genau einem Thread.
 */
public final class VideoDecoder implements AutoCloseable {
    private final SeekableByteChannel channel;
    private FrameGrab grab;
    private final double fps;
    private final double duration;
    private int frameIndex = 0;
    private final int width, height;

    private Transform toRgb;
    private Picture rgb;
    // Letterbox-Abbildung (wird bei Groessenaenderung neu berechnet)
    private int mapSrcW = -1, mapSrcH = -1, mapDstW = -1, mapDstH = -1;
    private int[] srcIndexX, srcIndexY;
    private int offX, offY, fitW, fitH;

    public VideoDecoder(Path file) throws IOException, JCodecException {
        this.channel = NIOUtils.readableChannel(file.toFile());
        this.grab = FrameGrab.createFrameGrab(channel);
        var meta = grab.getVideoTrack().getMeta();
        this.duration = meta.getTotalDuration();
        this.fps = duration > 0 && meta.getTotalFrames() > 0 ? meta.getTotalFrames() / duration : 25.0;
        var dim = grab.getMediaInfo().getDim();
        this.width = dim.getWidth();
        this.height = dim.getHeight();
    }

    public double fps() { return fps; }
    public int width() { return width; }
    public int height() { return height; }
    public double duration() { return duration; }

    /** Zeitstempel (Sekunden) des Bildes, das als naechstes dekodiert wird. */
    public double position() { return frameIndex / fps; }

    public void seek(double seconds) throws IOException, JCodecException {
        double s = Math.max(0, Math.min(seconds, Math.max(0, duration - 1.0 / fps)));
        grab.seekToSecondPrecise(s);
        frameIndex = (int) Math.round(s * fps);
    }

    /** Dekodiert das naechste Bild ohne es zu konvertieren (zum Aufholen). @return false am Ende. */
    public boolean skip() throws IOException {
        Picture p = grab.getNativeFrame();
        if (p == null) return false;
        frameIndex++;
        return true;
    }

    /** Dekodiert das naechste Bild und schreibt es als ARGB (0xAARRGGBB) in out[dstW*dstH]. @return false am Ende. */
    public boolean next(int[] out, int dstW, int dstH) throws IOException {
        Picture p = grab.getNativeFrame();
        if (p == null) return false;
        frameIndex++;
        convert(p, out, dstW, dstH);
        return true;
    }

    private void convert(Picture p, int[] out, int dstW, int dstH) {
        if (rgb == null || rgb.getWidth() != p.getWidth() || rgb.getHeight() != p.getHeight()) {
            rgb = Picture.create(p.getWidth(), p.getHeight(), ColorSpace.RGB);
            toRgb = ColorUtil.getTransform(p.getColor(), ColorSpace.RGB);
        }
        toRgb.transform(p, rgb);
        int srcW = p.getCroppedWidth(), srcH = p.getCroppedHeight(), stride = rgb.getWidth();
        if (srcW != mapSrcW || srcH != mapSrcH || dstW != mapDstW || dstH != mapDstH) buildMap(srcW, srcH, dstW, dstH);
        byte[] d = rgb.getPlaneData(0);
        java.util.Arrays.fill(out, 0, dstW * dstH, 0xFF000000);
        for (int y = 0; y < fitH; y++) {
            int rowSrc = srcIndexY[y] * stride;
            int rowDst = (y + offY) * dstW + offX;
            for (int x = 0; x < fitW; x++) {
                int i = (rowSrc + srcIndexX[x]) * 3;
                // jcodec speichert Bytes mit Offset -128
                int r = (d[i] + 128) & 0xFF, g = (d[i + 1] + 128) & 0xFF, b = (d[i + 2] + 128) & 0xFF;
                out[rowDst + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    private void buildMap(int srcW, int srcH, int dstW, int dstH) {
        double scale = Math.min((double) dstW / srcW, (double) dstH / srcH);
        fitW = Math.max(1, (int) Math.round(srcW * scale));
        fitH = Math.max(1, (int) Math.round(srcH * scale));
        offX = (dstW - fitW) / 2;
        offY = (dstH - fitH) / 2;
        srcIndexX = new int[fitW];
        srcIndexY = new int[fitH];
        for (int x = 0; x < fitW; x++) srcIndexX[x] = Math.min(srcW - 1, (int) ((x + 0.5) * srcW / fitW));
        for (int y = 0; y < fitH; y++) srcIndexY[y] = Math.min(srcH - 1, (int) ((y + 0.5) * srcH / fitH));
        mapSrcW = srcW; mapSrcH = srcH; mapDstW = dstW; mapDstH = dstH;
    }

    @Override
    public void close() {
        NIOUtils.closeQuietly(channel);
    }
}
