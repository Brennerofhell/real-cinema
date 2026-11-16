package ch.realcinema.client.video;

import ch.realcinema.video.VideoDecoder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Dekodiert ein Video auf einem eigenen Thread passend zur Zielzeit des Projektors.
 * Der Renderthread liest das fertige Bild ueber {@link #copyFrameTo(int[])}.
 * Faellt der Decoder zurueck (mehr als 0,5 s) oder liegt er zu weit voraus (mehr als 2 s), wird per Seek neu synchronisiert.
 */
public final class VideoPlayback implements AutoCloseable {
    public static final int WIDTH = 640, HEIGHT = 360;
    private static final Logger LOG = LogUtils.getLogger();

    public enum Status { LOADING, READY, ERROR }

    private final Path path;
    private final DoubleSupplier targetSeconds;
    private final BooleanSupplier playing;
    private final Thread thread;
    private volatile boolean stop;
    private volatile Status status = Status.LOADING;
    private volatile String error = "";
    private volatile long serial;
    private volatile boolean slow;
    private volatile boolean large;
    private final java.util.ArrayDeque<Long> resyncs = new java.util.ArrayDeque<>();

    private int[] front = new int[WIDTH * HEIGHT];
    private int[] back = new int[WIDTH * HEIGHT];
    private final Object lock = new Object();

    public VideoPlayback(Path path, DoubleSupplier targetSeconds, BooleanSupplier playing) {
        this.path = path;
        this.targetSeconds = targetSeconds;
        this.playing = playing;
        this.thread = new Thread(this::run, "realcinema-video");
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.NORM_PRIORITY - 1); // Spiel-Thread hat Vorrang vor dem Video
        this.thread.start();
    }

    public Status status() { return status; }
    public String error() { return error; }

    /** Der Decoder schafft die Echtzeit nicht (mehrfach hintereinander neu synchronisiert). */
    public boolean isSlow() { return slow; }

    /** Das Quellvideo ist groesser als 1280x720 und deshalb deutlich teurer zu dekodieren. */
    public boolean isLarge() { return large; }

    /** Zaehlt hoch, sobald ein neues Bild bereitliegt. */
    public long serial() { return serial; }

    public void copyFrameTo(int[] dst) {
        synchronized (lock) {
            System.arraycopy(front, 0, dst, 0, Math.min(dst.length, front.length));
        }
    }

    private void publish() {
        synchronized (lock) {
            int[] t = front;
            front = back;
            back = t;
        }
        serial++;
    }

    private void run() {
        try (VideoDecoder dec = new VideoDecoder(path)) {
            double frameDur = 1.0 / dec.fps();
            large = dec.width() > 1280 || dec.height() > 720;
            boolean needFirst = true;
            while (!stop) {
                double target = targetSeconds.getAsDouble();
                boolean play = playing.getAsBoolean();
                double drift = target - dec.position();
                if (needFirst || drift > 2.0 || drift < -0.5) {
                    if (!needFirst && drift > 2.0) noteResync();   // zurueckgefallen (nicht: voraus oder erster Start)
                    dec.seek(Math.max(0, target));
                    if (!dec.next(back, WIDTH, HEIGHT)) { sleep(100); continue; }
                    publish();
                    status = Status.READY;
                    needFirst = false;
                    continue;
                }
                if (!play) { sleep(40); continue; }
                if (dec.position() > target) { sleep(4); continue; }        // voraus: warten
                if (target - dec.position() > frameDur * 1.5) {             // hinten: ohne Konvertierung aufholen
                    if (!dec.skip()) sleep(100);
                } else {
                    if (!dec.next(back, WIDTH, HEIGHT)) { sleep(100); continue; }
                    publish();
                }
            }
        } catch (Throwable t) {
            if (!stop) {
                LOG.warn("Video konnte nicht abgespielt werden ({}): {}", path.getFileName(), t.toString());
                error = t.getMessage() == null ? t.toString() : t.getMessage();
                status = Status.ERROR;
            }
        }
    }

    /** Mehr als 3 Aufholspruenge in 10 s bedeuten: Dekodieren ist dauerhaft langsamer als Echtzeit. */
    private void noteResync() {
        long now = System.currentTimeMillis();
        resyncs.addLast(now);
        while (!resyncs.isEmpty() && now - resyncs.peekFirst() > 10_000) resyncs.pollFirst();
        slow = resyncs.size() > 3;
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
