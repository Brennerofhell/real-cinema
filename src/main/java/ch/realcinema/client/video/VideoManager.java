package ch.realcinema.client.video;

import ch.realcinema.Config;
import ch.realcinema.RealCinema;
import ch.realcinema.block.ProjectorBlockEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Clientseitige Verwaltung aller laufenden Videos. Pro Projektor mit Bild gibt es einen {@link Entry}
 * (Dekoder-Thread + Textur). Die Leinwand fragt ihren Eintrag ueber ihre Master-Position ab.
 * Alles hier laeuft auf dem Render-Thread (Ticks und Rendern), nur der Decoder hat einen eigenen Thread.
 */
public final class VideoManager {
    public static final class Entry {
        final BlockPos projector;
        String reference = "";
        @Nullable VideoPlayback playback;
        @Nullable AudioPlayback audio;
        volatile double[] audioPos;
        @Nullable DynamicTexture texture;
        @Nullable ResourceLocation textureId;
        @Nullable BlockPos screen;
        long uploadedSerial = -1;
        String notified = "";

        Entry(BlockPos projector) { this.projector = projector; }

        public boolean ready() {
            return playback != null && texture != null && playback.status() == VideoPlayback.Status.READY;
        }

        public ResourceLocation textureId() { return textureId; }

        /** Kopiert das neueste Bild in die Textur, wenn es ein neues gibt. Nur auf dem Render-Thread aufrufen. */
        public void uploadIfNew() {
            if (!ready()) return;
            long s = playback.serial();
            if (s == uploadedSerial) return;
            uploadedSerial = s;
            int[] buf = new int[VideoPlayback.WIDTH * VideoPlayback.HEIGHT];
            playback.copyFrameTo(buf);
            NativeImage img = texture.getPixels();
            if (img == null) return;
            int w = VideoPlayback.WIDTH;
            for (int y = 0; y < VideoPlayback.HEIGHT; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = buf[y * w + x];
                    img.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16)); // ARGB -> ABGR
                }
            }
            texture.upload();
        }

        void release() {
            if (playback != null) playback.close();
            playback = null;
            if (audio != null) audio.close();
            audio = null;
            if (textureId != null) Minecraft.getInstance().getTextureManager().release(textureId);
            texture = null;
            textureId = null;
            uploadedSerial = -1;
        }
    }

    private static final Map<BlockPos, Entry> BY_PROJECTOR = new HashMap<>();
    private static final Map<BlockPos, Entry> BY_SCREEN = new HashMap<>();
    private static int counter = 0;
    @Nullable private static ResourceLocation blackId;

    private VideoManager() {}

    @Nullable
    public static Entry forScreen(BlockPos master) {
        return BY_SCREEN.get(master);
    }

    /** 1x1 schwarze Textur fuer die Balken um das Bild. */
    public static ResourceLocation black() {
        if (blackId == null) {
            NativeImage img = new NativeImage(1, 1, false);
            img.setPixelRGBA(0, 0, 0xFF000000);
            blackId = ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, "video/black");
            Minecraft.getInstance().getTextureManager().register(blackId, new DynamicTexture(img));
        }
        return blackId;
    }

    public static void tick(ProjectorBlockEntity be) {
        BlockPos pos = be.getBlockPos().immutable();
        if (!be.isShowingPicture()) {
            remove(pos);
            return;
        }
        Entry e = BY_PROJECTOR.computeIfAbsent(pos, Entry::new);

        // Leinwand-Zuordnung aktuell halten
        BlockPos screen = be.screenPos();
        if (!java.util.Objects.equals(screen, e.screen)) {
            if (e.screen != null && BY_SCREEN.get(e.screen) == e) BY_SCREEN.remove(e.screen);
            e.screen = screen;
        }
        if (screen != null) BY_SCREEN.put(screen, e);

        e.audioPos = soundPosition(be, screen);

        // Nur in der Naehe dekodieren
        var player = Minecraft.getInstance().player;
        BlockPos anchor = screen != null ? screen : pos;
        double max = Config.MAX_VIEW_DISTANCE.get();
        if (player == null || player.distanceToSqr(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5) > max * max) {
            e.release();
            return;
        }

        if (!e.reference.equals(be.clientReference())) {
            e.release();
            e.reference = be.clientReference();
            e.notified = "";
        }
        if (e.playback == null) {
            VideoLocator.Result res = VideoLocator.resolve(e.reference);
            switch (res.state()) {
                case READY -> start(e, be, res);
                case DOWNLOADING -> notifyOnce(e, "downloading", "message.realcinema.video_downloading", "");
                case MISSING -> notifyOnce(e, "missing", "message.realcinema.video_missing", res.detail());
                case BLOCKED -> notifyOnce(e, "blocked", "message.realcinema.video_blocked", res.detail());
                case ERROR -> notifyOnce(e, "error", "message.realcinema.video_error", res.detail());
            }
        } else if (e.playback.status() == VideoPlayback.Status.ERROR) {
            notifyOnce(e, "playerror", "message.realcinema.video_error", e.playback.error());
        } else if (e.audio != null && e.audio.isSuspect()) {
            notifyOnce(e, "audiopns", "message.realcinema.video_audio_pns", "");
        } else if (e.playback.isSlow()) {
            notifyOnce(e, "slow", "message.realcinema.video_slow", "");
        } else if (e.playback.isLarge()) {
            notifyOnce(e, "large", "message.realcinema.video_large", "");
        }
    }

    /** Mitte der Leinwandflaeche (leicht vor der Flaeche) oder, ohne Leinwand, die Position des Projektors. */
    private static double[] soundPosition(ProjectorBlockEntity be, @Nullable BlockPos screen) {
        var level = Minecraft.getInstance().level;
        if (screen == null || level == null) {
            BlockPos p = be.getBlockPos();
            return new double[]{p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5};
        }
        var state = level.getBlockState(screen);
        if (!(state.getBlock() instanceof ch.realcinema.block.DeviceBlock)) {
            return new double[]{screen.getX() + 0.5, screen.getY() + 0.5, screen.getZ() + 0.5};
        }
        var facing = state.getValue(ch.realcinema.block.DeviceBlock.FACING);
        var surface = ch.realcinema.block.ScreenHelper.surfaceOf(level, screen, facing);
        var right = ch.realcinema.block.ScreenHelper.right(facing);
        double x = screen.getX() + 0.5 + right.getStepX() * (surface.width() - 1) / 2.0 + facing.getStepX() * 0.4;
        double z = screen.getZ() + 0.5 + right.getStepZ() * (surface.width() - 1) / 2.0 + facing.getStepZ() * 0.4;
        double y = screen.getY() + surface.height() / 2.0;
        return new double[]{x, y, z};
    }

    private static void start(Entry e, ProjectorBlockEntity be, VideoLocator.Result res) {
        NativeImage img = new NativeImage(VideoPlayback.WIDTH, VideoPlayback.HEIGHT, true);
        e.texture = new DynamicTexture(img);
        e.textureId = ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, "video/p" + (counter++));
        Minecraft.getInstance().getTextureManager().register(e.textureId, e.texture);
        e.playback = new VideoPlayback(res.path(), be::playbackSeconds, be::isAdvancing);
        if (Config.ENABLE_AUDIO.get()) {
            e.audio = new AudioPlayback(res.path(), be::playbackSeconds, be::isAdvancing, () -> e.audioPos);
        }
    }

    private static void notifyOnce(Entry e, String kind, String key, String detail) {
        String token = kind + detail;
        if (token.equals(e.notified)) return;
        e.notified = token;
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.translatable(key, detail), true);
    }

    public static void remove(BlockPos projector) {
        Entry e = BY_PROJECTOR.remove(projector);
        if (e == null) return;
        if (e.screen != null && BY_SCREEN.get(e.screen) == e) BY_SCREEN.remove(e.screen);
        e.release();
    }

    /** Beim Verlassen der Welt alles freigeben. */
    public static void clear() {
        Set<BlockPos> all = new HashSet<>(BY_PROJECTOR.keySet());
        all.forEach(VideoManager::remove);
        BY_SCREEN.clear();
    }
}
