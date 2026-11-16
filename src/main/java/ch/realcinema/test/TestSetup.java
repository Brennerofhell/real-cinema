package ch.realcinema.test;

import ch.realcinema.RealCinema;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Meldet die GameTests an. Sie laufen nur bei {@code runGameTestServer} bzw. dem /test-Befehl, nie im normalen Spielbetrieb. */
@EventBusSubscriber(modid = RealCinema.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TestSetup {
    private TestSetup() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(RealCinemaTests.class);
        event.register(GuiTests.class);
    }
}
