package ch.realcinema.client.video;

import ch.realcinema.Config;
import ch.realcinema.RealCinema;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Wandelt die Videoreferenz einer Filmrolle in eine lokale Datei um.
 * <ul>
 *   <li>Lokal: nur ein reiner Dateiname (kein Pfad, keine Trennzeichen) im Ordner {@code <spielordner>/realcinema_videos/}.
 *       Damit kann eine Referenz nie auf andere Dateien des Rechners zeigen.</li>
 *   <li>Link: nur {@code http(s)} und nur, wenn in der Client-Config ausdruecklich erlaubt. Der Download laeuft asynchron mit
 *       Groessenlimit in {@code <spielordner>/realcinema_cache/}.</li>
 * </ul>
 */
public final class VideoLocator {
    public enum State { READY, DOWNLOADING, MISSING, BLOCKED, ERROR }

    public record Result(State state, Path path, String detail) {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Pattern URL = Pattern.compile("^https?://\\S+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILENAME = Pattern.compile("^[A-Za-z0-9 _.()\\-]{1,100}$");
    private static final Map<String, Result> DOWNLOADS = new ConcurrentHashMap<>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "realcinema-download");
        t.setDaemon(true);
        return t;
    });

    private VideoLocator() {}

    public static Path videoDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("realcinema_videos");
    }

    private static Path cacheDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("realcinema_cache");
    }

    /** Darf pro Tick aufgerufen werden (billig). */
    public static Result resolve(String reference) {
        String ref = reference == null ? "" : reference.trim();
        if (ref.isEmpty()) return new Result(State.MISSING, null, "");
        if (URL.matcher(ref).matches()) return resolveUrl(ref);
        if (!FILENAME.matcher(ref).matches() || ref.contains("..")) {
            return new Result(State.BLOCKED, null, "Ungueltiger Dateiname");
        }
        Path dir = videoDir();
        Path file = dir.resolve(ref);
        if (!Files.isRegularFile(file) && !ref.contains(".")) file = dir.resolve(ref + ".mp4");
        file = file.normalize();
        if (!file.startsWith(dir.normalize())) return new Result(State.BLOCKED, null, "Ungueltiger Pfad");
        return Files.isRegularFile(file) ? new Result(State.READY, file, ref) : new Result(State.MISSING, null, ref);
    }

    private static Result resolveUrl(String url) {
        if (!Config.ALLOW_REMOTE_VIDEOS.get()) return new Result(State.BLOCKED, null, "Remote-Videos sind in der Config aus");
        Result known = DOWNLOADS.get(url);
        if (known != null) return known;
        Path target = cacheDir().resolve(hash(url) + ".mp4");
        if (Files.isRegularFile(target)) {
            Result r = new Result(State.READY, target, url);
            DOWNLOADS.put(url, r);
            return r;
        }
        Result pending = new Result(State.DOWNLOADING, null, url);
        if (DOWNLOADS.putIfAbsent(url, pending) == null) POOL.execute(() -> download(url, target));
        return pending;
    }

    private static void download(String url, Path target) {
        long max = Config.MAX_DOWNLOAD_MB.get() * 1024L * 1024L;
        Path tmp = target.resolveSibling(target.getFileName() + ".part");
        try {
            Files.createDirectories(target.getParent());
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "RealCinema/" + RealCinema.MOD_ID).GET().build();
            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) throw new IllegalStateException("HTTP " + resp.statusCode());
            long declared = resp.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (declared > max) throw new IllegalStateException("Datei zu gross (" + declared / 1024 / 1024 + " MB)");
            long total = 0;
            try (InputStream in = resp.body(); var out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > max) throw new IllegalStateException("Download ueberschreitet das Limit");
                    out.write(buf, 0, n);
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            DOWNLOADS.put(url, new Result(State.READY, target, url));
        } catch (Exception e) {
            LOG.warn("Video-Download fehlgeschlagen ({}): {}", url, e.toString());
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            DOWNLOADS.put(url, new Result(State.ERROR, null, e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    private static String hash(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
