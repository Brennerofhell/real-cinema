package ch.realcinema.network;

import ch.realcinema.RealCinema;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = RealCinema.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkSetup {
    private NetworkSetup() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetReferencePayload.TYPE, SetReferencePayload.CODEC, SetReferencePayload::handle);
    }
}
