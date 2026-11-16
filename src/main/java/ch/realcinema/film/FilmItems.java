package ch.realcinema.film;

import ch.realcinema.registry.ModDataComponents;
import ch.realcinema.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Die drei Rollen-Items sind Zustaende derselben Sache: leer, beschrieben (traegt {@link FilmData}) und eingelegt
 * (beschrieben, steckt in einem Projektor). Die Umwandlung behaelt alle Komponenten.
 */
public final class FilmItems {
    private FilmItems() {}

    public static boolean isEmptyRoll(ItemStack s) {
        return s.is(ModItems.FILM_ROLL_EMPTY.get());
    }

    /** Beschriebene oder eingelegte Rolle (jede Rolle mit Filmdaten). */
    public static boolean isWritten(ItemStack s) {
        return s.has(ModDataComponents.FILM.get());
    }

    public static boolean isAnyRoll(ItemStack s) {
        return isEmptyRoll(s) || isWritten(s);
    }

    public static ItemStack toLoaded(ItemStack s) {
        return s.is(ModItems.FILM_ROLL_LOADED.get()) ? s.copyWithCount(1) : s.transmuteCopy(ModItems.FILM_ROLL_LOADED.get(), 1);
    }

    public static ItemStack toWritten(ItemStack s) {
        return s.is(ModItems.FILM_ROLL_LOADED.get()) ? s.transmuteCopy(ModItems.FILM_ROLL_WRITTEN.get(), 1) : s.copyWithCount(1);
    }

    /**
     * Schneidet eine Rolle in zwei Abschnitte desselben Videos.
     * @param fraction Anteil der ersten Rolle (0..1), wird so begrenzt, dass beide Rollen mindestens 1 s lang sind
     * @return {erste, zweite} oder null, wenn keine Filmdaten oder zu kurz
     */
    @Nullable
    public static ItemStack[] cut(ItemStack roll, double fraction) {
        FilmData d = roll.get(ModDataComponents.FILM.get());
        if (d == null || d.lengthTicks() < FilmData.MIN_CUTTABLE_TICKS) return null;
        int first = (int) Math.round(d.lengthTicks() * fraction);
        first = Math.max(20, Math.min(d.lengthTicks() - 20, first));
        ItemStack a = toWritten(roll);
        a.set(ModDataComponents.FILM.get(), d.slice(0, first));
        ItemStack b = toWritten(roll);
        b.set(ModDataComponents.FILM.get(), d.slice(first, d.lengthTicks() - first));
        return new ItemStack[]{a, b};
    }

    /** Fuegt zwei aneinanderschliessende Abschnitte desselben Videos zusammen, sonst null. */
    @Nullable
    public static ItemStack join(ItemStack a, ItemStack b) {
        FilmData da = a.get(ModDataComponents.FILM.get()), db = b.get(ModDataComponents.FILM.get());
        if (da == null || db == null || !da.isContiguousWith(db)) return null;
        ItemStack joined = toWritten(a);
        joined.set(ModDataComponents.FILM.get(), da.join(db));
        return joined;
    }
}
