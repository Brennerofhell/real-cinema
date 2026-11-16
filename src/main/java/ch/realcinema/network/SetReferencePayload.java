package ch.realcinema.network;

import ch.realcinema.RealCinema;
import ch.realcinema.menu.FilmBurnerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> Server: Der Spieler hat im Filmbrenner-Textfeld die Videoreferenz geaendert.
 * Der Server vertraut dem Inhalt nicht: Das Paket wirkt nur, wenn der Spieler gerade genau dieses Menue offen hat; Laenge und
 * Steuerzeichen werden im Menue begrenzt bzw. entfernt.
 */
public record SetReferencePayload(int containerId, String reference) implements CustomPacketPayload {
    public static final Type<SetReferencePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, "set_reference"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetReferencePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetReferencePayload::containerId,
            ByteBufCodecs.stringUtf8(FilmBurnerMenu.MAX_REFERENCE * 2), SetReferencePayload::reference,
            SetReferencePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server-Handler. */
    public static void handle(SetReferencePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof FilmBurnerMenu menu && menu.containerId == payload.containerId()) {
                menu.setReference(payload.reference());
            }
        });
    }
}
