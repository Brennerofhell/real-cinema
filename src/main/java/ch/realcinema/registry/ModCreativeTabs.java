package ch.realcinema.registry;

import ch.realcinema.RealCinema;
import ch.realcinema.film.FilmData;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RealCinema.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + RealCinema.MOD_ID))
                    .icon(() -> ModItems.PROJECTOR.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        // Registrierung des Registers ist die Reihenfolge, daher stabil und ohne Handpflege.
                        ModItems.ITEMS.getEntries().forEach(item -> {
                            if (item.get() == ModItems.FILM_ROLL_WRITTEN.get()) {
                                // Beispielrolle mit Daten, sonst waere die beschriebene Rolle im Kreativmenue unbrauchbar.
                                ItemStack demo = new ItemStack(item.get());
                                demo.set(ModDataComponents.FILM.get(), new FilmData("Demo", FilmData.DEFAULT_LENGTH_TICKS));
                                out.accept(demo);
                            } else {
                                out.accept(item.get());
                            }
                        });
                    })
                    .build());

    private ModCreativeTabs() {}
}
