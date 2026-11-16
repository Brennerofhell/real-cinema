package ch.realcinema.compat;

import ch.realcinema.RealCinema;
import ch.realcinema.block.DeviceType;
import ch.realcinema.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-Anbindung (optional). Die Rezepte der Mod sind normale Werkbank-Rezepte und erscheinen in JEI von selbst.
 * Dieses Plugin ergaenzt zu jedem Geraet und Item eine Info-Seite mit der Bedienung.
 * Die Klasse wird nur geladen, wenn JEI installiert ist; ohne JEI hat sie keine Wirkung.
 */
@JeiPlugin
public class RealCinemaJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, "jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        info(registration, new ItemStack(ModItems.PROJECTOR.get()), "projector");
        for (DeviceType type : DeviceType.values()) {
            info(registration, new ItemStack(ModItems.DEVICES.get(type).get()), type.id);
        }
        info(registration, new ItemStack(ModItems.FILM_ROLL_EMPTY.get()), "film_roll_empty");
        info(registration, new ItemStack(ModItems.FILM_ROLL_WRITTEN.get()), "film_roll_written");
        info(registration, new ItemStack(ModItems.FILM_ROLL_LOADED.get()), "film_roll_loaded");
        info(registration, new ItemStack(ModItems.FILM_CANISTER.get()), "film_canister");
        info(registration, new ItemStack(ModItems.CLAPPERBOARD.get()), "clapperboard");
    }

    private static void info(IRecipeRegistration registration, ItemStack stack, String id) {
        registration.addItemStackInfo(stack, Component.translatable("jei." + RealCinema.MOD_ID + ".info." + id));
    }
}
