package ch.realcinema.client.video;

import ch.realcinema.video.AudioTrackReader;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Spielt die Tonspur eines Videos ueber OpenAL (der Sound-Kontext von Minecraft) als eigene Quelle an der Leinwand ab.
 * Positionston und Lautstaerke (Kategorie "Plattenspieler/Musikbloecke") folgen den Minecraft-Einstellungen.
 *
 * Synchronisation: Die tatsaechlich gespielte Position wird aus der Dauer der noch eingereihten Puffer geschaetzt.
 * Weicht sie um mehr als {@link #MAX_DRIFT} von der Projektorposition ab, werden die Puffer verworfen und neu aufgesetzt.
 * Fehler schalten nur den Ton ab, nie das Bild.
 */
public final class AudioPlayback implements AutoCloseable {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int BUFFERS = 6;
    private static final int CHUNK_FRAMES = 8192;
    private static final double MAX_DRIFT = 0.35;

    private final Path path;
    private final DoubleSupplier targetSeconds;
    private final BooleanSupplier playing;
    private final Supplier<double[]> position;
    private final Thread thread;
    private volatile boolean stop;
    private volatile boolean suspect;

    public AudioPlayback(Path path, DoubleSupplier targetSeconds, BooleanSupplier playing, Supplier<double[]> position) {
        this.path = path;
        this.targetSeconds = targetSeconds;
        this.playing = playing;
        this.position = position;
        this.thread = new Thread(this::run, "realcinema-audio");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    /** Die Tonspur dekodiert trotz Nutzlast zu Stille (typisch: ffmpeg-AAC mit PNS). */
    public boolean isSuspect() { return suspect; }

    private static void check(String what) {
        int err = AL10.alGetError();
        if (err != AL10.AL_NO_ERROR) throw new IllegalStateException("OpenAL " + what + ": Fehler " + err);
    }

    private void run() {
        int source = 0;
        int[] bufs = new int[BUFFERS];
        boolean buffersCreated = false;
        try (AudioTrackReader reader = AudioTrackReader.open(path)) {
            if (reader == null) {
                LOG.info("Video {} hat keine AAC-Tonspur, es gibt keinen Ton.", path.getFileName());
                return;
            }
            AL10.alGetError(); // alten Fehlerzustand verwerfen
            source = AL10.alGenSources();
            check("alGenSources");
            AL10.alGenBuffers(bufs);
            buffersCreated = true;
            check("alGenBuffers");
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_FALSE);
            AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 6f);
            AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 48f);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 1f);

            ArrayDeque<Integer> free = new ArrayDeque<>();
            for (int b : bufs) free.add(b);
            ArrayDeque<Double> queuedDur = new ArrayDeque<>();
            double queuedUntil = 0;
            boolean needSeek = true;

            while (!stop) {
                // fertig abgespielte Puffer zurueckholen
                int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                while (processed-- > 0) {
                    free.add(AL10.alSourceUnqueueBuffers(source));
                    queuedDur.pollFirst();
                }
                applySourceState(source);

                double target = targetSeconds.getAsDouble();
                boolean play = playing.getAsBoolean();
                double remaining = 0;
                for (double d : queuedDur) remaining += d;
                remaining -= AL10.alGetSourcef(source, org.lwjgl.openal.AL11.AL_SEC_OFFSET);
                double playingPos = queuedUntil - Math.max(0, remaining);

                if (!play) {
                    if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) AL10.alSourcePause(source);
                    sleep(30);
                    continue;
                }

                if (needSeek || (!queuedDur.isEmpty() && Math.abs(target - playingPos) > MAX_DRIFT)) {
                    AL10.alSourceStop(source);
                    int p = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                    while (p-- > 0) free.add(AL10.alSourceUnqueueBuffers(source));
                    queuedDur.clear();
                    reader.seek(target);
                    needSeek = false;
                }

                while (!free.isEmpty() && queuedDur.size() < BUFFERS) {
                    AudioTrackReader.Chunk c = reader.read(CHUNK_FRAMES);
                    if (c == null) break;
                    if (!suspect && reader.suspectUnsupportedFeatures()) {
                        suspect = true;
                        LOG.warn("Ton von {} ist vermutlich still: Die AAC-Spur nutzt eine Funktion, die der Decoder nicht kann. "
                                + "Umkodieren z. B. mit: ffmpeg -i <eingabe> -c:v copy -c:a aac -b:a 128k <ausgabe>", path.getFileName());
                    }
                    if (queuedDur.isEmpty()) queuedUntil = c.startSeconds();
                    int b = free.poll();
                    ByteBuffer data = BufferUtils.createByteBuffer(c.pcm().length);
                    data.put(c.pcm()).flip();
                    int format = c.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                    AL10.alBufferData(b, format, data, c.sampleRate());
                    AL10.alSourceQueueBuffers(source, b);
                    check("queue");
                    double dur = c.frames() / (double) c.sampleRate();
                    queuedDur.addLast(dur);
                    queuedUntil += dur;
                }
                if (!queuedDur.isEmpty() && AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                    AL10.alSourcePlay(source);
                }
                sleep(15);
            }
        } catch (Throwable t) {
            if (!stop) LOG.warn("Ton fuer {} abgeschaltet: {}", path.getFileName(), t.toString());
        } finally {
            try {
                if (source != 0) {
                    AL10.alSourceStop(source);
                    int p = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                    while (p-- > 0) AL10.alSourceUnqueueBuffers(source);
                    AL10.alDeleteSources(source);
                }
                if (buffersCreated) AL10.alDeleteBuffers(bufs);
            } catch (Throwable ignored) {
                // Beim Beenden des Spiels kann der Kontext schon weg sein.
            }
        }
    }

    private void applySourceState(int source) {
        float vol = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
        AL10.alSourcef(source, AL10.AL_GAIN, Math.max(0f, Math.min(1f, vol)));
        double[] p = position.get();
        if (p != null) AL10.alSource3f(source, AL10.AL_POSITION, (float) p[0], (float) p[1], (float) p[2]);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    @Override
    public void close() {
        stop = true;
        thread.interrupt();
    }
}
