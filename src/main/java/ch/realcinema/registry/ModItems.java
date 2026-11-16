package ch.realcinema.registry;

import ch.realcinema.RealCinema;
import ch.realcinema.block.DeviceType;
import ch.realcinema.item.DeviceBlockItem;
import ch.realcinema.item.ClapperboardItem;
import ch.realcinema.item.FilmCanisterItem;
import ch.realcinema.item.GeoSimpleItem;
import ch.realcinema.item.ProjectorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RealCinema.MOD_ID);

    public static final DeferredItem<ProjectorItem> PROJECTOR = ITEMS.register("projector",
            () -> new ProjectorItem(ModBlocks.PROJECTOR.get(), new Item.Properties()));

    public static final Map<DeviceType, DeferredItem<DeviceBlockItem>> DEVICES = new EnumMap<>(DeviceType.class);

    static {
        for (DeviceType type : DeviceType.values()) {
            DEVICES.put(type, ITEMS.register(type.id,
                    () -> new DeviceBlockItem(type, ModBlocks.DEVICES.get(type).get(), new Item.Properties())));
        }
    }

    // Filmrollen: gleiche Geometrie, drei Texturen (leer / beschrieben / eingelegt)
    public static final DeferredItem<GeoSimpleItem> FILM_ROLL_EMPTY = ITEMS.register("film_roll_empty",
            () -> new GeoSimpleItem(new Item.Properties().stacksTo(16), "film_roll", "film_roll_empty", null));
    public static final DeferredItem<GeoSimpleItem> FILM_ROLL_WRITTEN = ITEMS.register("film_roll_written",
            () -> new GeoSimpleItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), "film_roll", "film_roll_written", null));
    public static final DeferredItem<GeoSimpleItem> FILM_ROLL_LOADED = ITEMS.register("film_roll_loaded",
            () -> new GeoSimpleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "film_roll", "film_roll_loaded", null));

    public static final DeferredItem<FilmCanisterItem> FILM_CANISTER = ITEMS.register("film_canister",
            () -> new FilmCanisterItem(new Item.Properties(), "film_canister", "film_canister"));
    public static final DeferredItem<ClapperboardItem> CLAPPERBOARD = ITEMS.register("clapperboard",
            () -> new ClapperboardItem(new Item.Properties(), "clapperboard", "clapperboard"));

    private ModItems() {}
}
