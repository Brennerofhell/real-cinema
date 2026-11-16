package ch.realcinema.video;

import ch.realcinema.vendor.jaad.aac.AACException;
import ch.realcinema.vendor.jaad.aac.Decoder;
import ch.realcinema.vendor.jaad.aac.SampleBuffer;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * Liest die erste AAC-Tonspur eines MP4 (jcodec zerlegt den Container) und dekodiert sie mit dem mitgelieferten, in
 * {@code ch.realcinema.vendor.jaad} korrigierten JAAD-Decoder zu 16-Bit-PCM (little endian, Mono oder Stereo).
 * Enthaelt keine Minecraft-Klassen. Nicht thread-sicher.
 */
public final class AudioTrackReader implements AutoCloseable {
    /** Ein Block dekodierter Audiodaten: {@code frames} Samples pro Kanal, verschachtelt, ab {@code startSeconds}. */
    public record Chunk(byte[] pcm, int frames, double startSeconds, int sampleRate, int channels) {}

    private static final int SILENCE_PACKET_BYTES = 100;
    private static final int SUSPECT_THRESHOLD = 25;

    private final SeekableByteChannel channel;
    private final SeekableDemuxerTrack track;
    private final Decoder decoder;
    private int suspiciousPackets = 0;
    private int failedPackets = 0;

    private AudioTrackReader(SeekableByteChannel channel, SeekableDemuxerTrack track, Decoder decoder) {
        this.channel = channel;
        this.track = track;
        this.decoder = decoder;
    }

    /** @return Reader oder null, wenn die Datei keine AAC-Tonspur hat (dann gibt es einfach keinen Ton). */
    public static AudioTrackReader open(Path file) throws IOException {
        SeekableByteChannel ch = NIOUtils.readableChannel(file.toFile());
        try {
            MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(ch);
            for (DemuxerTrack t : demuxer.getAudioTracks()) {
                DemuxerTrackMeta meta = t.getMeta();
                if (meta.getCodec() != Codec.AAC || !(t instanceof SeekableDemuxerTrack seekable)) continue;
                ByteBuffer cp = meta.getCodecPrivate().duplicate();
                byte[] config = new byte[cp.remaining()];
                cp.get(config);
                return new AudioTrackReader(ch, seekable, new Decoder(config));
            }
        } catch (AACException e) {
            NIOUtils.closeQuietly(ch);
            throw new IOException("AAC-Konfiguration nicht lesbar: " + e.getMessage(), e);
        } catch (Exception e) {
            NIOUtils.closeQuietly(ch);
            if (e instanceof IOException io) throw io;
            throw new IOException("Audio konnte nicht geoeffnet werden: " + e, e);
        }
        NIOUtils.closeQuietly(ch);
        return null;
    }

    public void seek(double seconds) throws IOException {
        track.seek(Math.max(0, seconds));
    }

    /**
     * Dekodiert Pakete, bis mindestens {@code minFrames} Samples pro Kanal vorliegen. Defekte Pakete werden uebersprungen.
     * @return null am Ende der Spur
     */
    public Chunk read(int minFrames) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(minFrames * 4 + 4096);
        int frames = 0, rate = 0, channels = 0;
        double start = -1;
        while (frames < minFrames) {
            Packet p = track.nextFrame();
            if (p == null) break;
            if (start < 0) start = p.getPtsD();
            int packetBytes = p.getData().remaining();
            SampleBuffer sb = new SampleBuffer();
            try {
                decoder.decodeFrame(stripAdts(NIOUtils.toArray(p.getData())), sb);
            } catch (AACException e) {
                failedPackets++;
                continue;
            }
            sb.setBigEndian(false);
            int ch = sb.getChannels();
            if (ch <= 0 || sb.getData().length == 0) { failedPackets++; continue; }
            if (rate == 0) { rate = sb.getSampleRate(); channels = Math.min(2, ch); }
            int before = out.size();
            frames += append(sb, channels, out);
            if (packetBytes > SILENCE_PACKET_BYTES && isSilent(out.toByteArray(), before)) suspiciousPackets++;
        }
        if (frames == 0) return null;
        return new Chunk(out.toByteArray(), frames, Math.max(0, start), rate, channels);
    }

    /**
     * Ein Paket mit viel Nutzlast, das trotzdem zu Stille dekodiert, ist kein echtes Schweigen (echte Stille kodiert sich in
     * wenigen Bytes). Das deutet auf eine nicht unterstuetzte Funktion der Tonspur hin.
     */
    public boolean suspectUnsupportedFeatures() {
        return suspiciousPackets >= SUSPECT_THRESHOLD;
    }

    /**
     * jcodecs MP4-Demuxer liefert AAC-Pakete mit vorangestelltem ADTS-Kopf (Sync 0xFFF). Der Decoder erwartet rohe Frames,
     * sonst werden die Kopfbytes als Audiodaten gelesen und das Ergebnis ist Stille. Der Kopf ist 7 Bytes lang, mit CRC 9.
     */
    static byte[] stripAdts(byte[] frame) {
        if (frame.length > 9 && (frame[0] & 0xFF) == 0xFF && (frame[1] & 0xF0) == 0xF0) {
            int header = (frame[1] & 0x01) == 1 ? 7 : 9;   // protection_absent = 1: kein CRC
            return java.util.Arrays.copyOfRange(frame, header, frame.length);
        }
        return frame;
    }

    /** Anzahl der Pakete, die nicht dekodiert werden konnten (zum Diagnostizieren). */
    public int failedPackets() {
        return failedPackets;
    }

    private static boolean isSilent(byte[] pcm, int from) {
        for (int i = from; i < pcm.length; i++) if (pcm[i] != 0) return false;
        return true;
    }

    /** Haengt die (bereits little-endian) 16-Bit-Samples an und reduziert auf {@code outChannels}. @return Samples pro Kanal */
    private static int append(SampleBuffer sb, int outChannels, ByteArrayOutputStream out) {
        byte[] d = sb.getData();
        int srcCh = sb.getChannels();
        int frames = d.length / (srcCh * 2);
        if (srcCh == outChannels) {
            out.write(d, 0, frames * srcCh * 2);
            return frames;
        }
        for (int i = 0; i < frames; i++) {
            for (int c = 0; c < outChannels; c++) {
                int idx = (i * srcCh + c) * 2;
                out.write(d[idx]);
                out.write(d[idx + 1]);
            }
        }
        return frames;
    }

    @Override
    public void close() {
        NIOUtils.closeQuietly(channel);
    }
}
