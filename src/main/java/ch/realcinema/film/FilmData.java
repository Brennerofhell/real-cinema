package ch.realcinema.film;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Inhalt einer Filmrolle: ein Abschnitt eines Videos.
 *
 * @param reference   Videoreferenz (Dateiname oder Link)
 * @param lengthTicks Laenge dieses Abschnitts in Ticks (20 pro Sekunde)
 * @param startTicks  Beginn des Abschnitts im Video in Ticks (0 = Anfang). Schneiden und Zusammenfuegen aendern nur
 *                    diese beiden Zahlen, das Video selbst bleibt unveraendert.
 */
public record FilmData(String reference, int lengthTicks, int startTicks) {
    public static final int DEFAULT_LENGTH_TICKS = 20 * 60;
    public static final int MIN_CUTTABLE_TICKS = 40;

    public static final Codec<FilmData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("reference").forGetter(FilmData::reference),
            Codec.INT.fieldOf("length").forGetter(FilmData::lengthTicks),
            Codec.INT.optionalFieldOf("start", 0).forGetter(FilmData::startTicks)
    ).apply(i, FilmData::new));

    public static final StreamCodec<ByteBuf, FilmData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FilmData::reference,
            ByteBufCodecs.VAR_INT, FilmData::lengthTicks,
            ByteBufCodecs.VAR_INT, FilmData::startTicks,
            FilmData::new);

    public FilmData(String reference, int lengthTicks) {
        this(reference, lengthTicks, 0);
    }

    public int endTicks() {
        return startTicks + lengthTicks;
    }

    public int seconds() {
        return lengthTicks / 20;
    }

    /** Teilstueck [offset, offset+length) dieses Abschnitts. */
    public FilmData slice(int offsetTicks, int length) {
        return new FilmData(reference, length, startTicks + offsetTicks);
    }

    /** Schliesst genau an other an (this folgt direkt auf other oder umgekehrt) und hat dieselbe Referenz. */
    public boolean isContiguousWith(FilmData other) {
        return reference.equals(other.reference) && (endTicks() == other.startTicks || other.endTicks() == startTicks);
    }

    /** Vereinigt zwei zusammenhaengende Abschnitte. Nur sinnvoll, wenn {@link #isContiguousWith} gilt. */
    public FilmData join(FilmData other) {
        return new FilmData(reference, lengthTicks + other.lengthTicks, Math.min(startTicks, other.startTicks));
    }

    /** Zeitbereich als "m:ss-m:ss" fuer Tooltips. */
    public String range() {
        return clock(startTicks) + "-" + clock(endTicks());
    }

    private static String clock(int ticks) {
        int s = ticks / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }
}
