package ch.realcinema.registry;

import ch.realcinema.RealCinema;
import ch.realcinema.film.FilmData;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(RealCinema.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilmData>> FILM = COMPONENTS.registerComponentType(
            "film", builder -> builder.persistent(FilmData.CODEC).networkSynchronized(FilmData.STREAM_CODEC));

    private ModDataComponents() {}
}
