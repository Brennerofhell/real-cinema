package ch.realcinema.client;

import ch.realcinema.RealCinema;
import ch.realcinema.client.video.VideoManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = RealCinema.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** Beim Verlassen der Welt alle Dekoder-Threads und Texturen freigeben. */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        VideoManager.clear();
    }
}
