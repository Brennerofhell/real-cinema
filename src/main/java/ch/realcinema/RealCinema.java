package ch.realcinema;

import ch.realcinema.registry.ModBlockEntities;
import ch.realcinema.registry.ModBlocks;
import ch.realcinema.registry.ModCreativeTabs;
import ch.realcinema.registry.ModDataComponents;
import ch.realcinema.registry.ModItems;
import ch.realcinema.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(RealCinema.MOD_ID)
public class RealCinema {
    public static final String MOD_ID = "realcinema";

    public RealCinema(IEventBus modBus, ModContainer container) {
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
