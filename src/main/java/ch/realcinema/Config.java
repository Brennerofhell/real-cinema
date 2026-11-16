package ch.realcinema;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Clientseitige Einstellungen. Netzwerkzugriffe sind standardmaessig AUS (siehe README, Abschnitt Sicherheit). */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ALLOW_REMOTE_VIDEOS;
    public static final ModConfigSpec.IntValue MAX_DOWNLOAD_MB;
    public static final ModConfigSpec.IntValue MAX_VIEW_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_AUDIO;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("video");
        ALLOW_REMOTE_VIDEOS = b.comment(
                "Erlaubt dem Spiel, Videos von http(s)-Links herunterzuladen, die in Filmrollen stehen.",
                "ACHTUNG: Die Links stammen von anderen Spielern. Beim Abspielen verbindet sich dein Spiel mit dem Server hinter dem Link",
                "(deine IP-Adresse ist dort sichtbar). Nur aktivieren, wenn du den Spielern auf dem Server vertraust.")
                .define("allowRemoteVideos", false);
        MAX_DOWNLOAD_MB = b.comment("Maximale Groesse eines heruntergeladenen Videos in MB.")
                .defineInRange("maxDownloadMb", 256, 1, 4096);
        MAX_VIEW_DISTANCE = b.comment("Videos werden nur dekodiert, wenn du naeher als so viele Bloecke an der Leinwand bist.")
                .defineInRange("maxViewDistance", 48, 8, 128);
        ENABLE_AUDIO = b.comment("Spielt die AAC-Tonspur des Videos ab (Lautstaerke: Minecraft-Kategorie Plattenspieler/Musikbloecke).")
                .define("enableAudio", true);
        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
