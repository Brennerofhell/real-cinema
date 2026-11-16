package ch.realcinema.client;

import ch.realcinema.RealCinema;
import ch.realcinema.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import ch.realcinema.client.screen.ControlPanelScreen;
import ch.realcinema.client.screen.FilmBurnerScreen;
import ch.realcinema.client.screen.FilmCutterScreen;
import ch.realcinema.client.screen.ProjectorScreen;
import ch.realcinema.registry.ModMenus;

@EventBusSubscriber(modid = RealCinema.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PROJECTOR.get(), ctx -> new ProjectorRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.DEVICE.get(), ctx -> new DeviceRenderer());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FILM_BURNER.get(), FilmBurnerScreen::new);
        event.register(ModMenus.PROJECTOR.get(), ProjectorScreen::new);
        event.register(ModMenus.CONTROL_PANEL.get(), ControlPanelScreen::new);
        event.register(ModMenus.FILM_CUTTER.get(), FilmCutterScreen::new);
    }
}
