package ch.realcinema.menu;

import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.DeviceType;
import ch.realcinema.film.FilmItems;
import ch.realcinema.registry.ModItems;
import ch.realcinema.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.UnaryOperator;

/**
 * Filmbrenner-Oberflaeche: ein Rollenfach, die Videoreferenz (Textfeld, per Paket gesetzt), Brennen-Knopf und Fortschritt.
 * Button 0 = Brennen. Datenfelder: 0 = Zustand (0 bereit, 1 brennt, 2 fertig), 1 = Ticks im Zustand, 2 = Referenz gesetzt.
 */
public final class FilmBurnerMenu extends CinemaMenu {
    public static final int BTN_BURN = 0;
    public static final int STATE_IDLE = 0, STATE_BURNING = 1, STATE_DONE = 2;
    public static final int MAX_REFERENCE = 200;

    public final DeviceBlockEntity be;
    public final BlockPos pos;
    private final ContainerData data;
    /** Nur Client: Text des Eingabefelds beim Oeffnen (vom Server mitgeschickt). */
    public final String initialReference;

    /** Server. */
    public FilmBurnerMenu(int id, Inventory inv, DeviceBlockEntity be) {
        this(id, inv, be, serverData(be), be.reference());
    }

    /** Client (aus dem Oeffnungs-Paket: Position, Referenz). */
    public FilmBurnerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()), new SimpleContainerData(3), buf.readUtf(MAX_REFERENCE));
    }

    private FilmBurnerMenu(int id, Inventory inv, DeviceBlockEntity be, ContainerData data, String reference) {
        super(ModMenus.FILM_BURNER.get(), id);
        this.be = be;
        this.pos = be.getBlockPos();
        this.data = data;
        this.initialReference = reference;
        addSlot(new Slot(new SlotContainer(be::film, be::setFilm, UnaryOperator.identity(), be::setChanged), 0, 26, 40) {
            @Override public boolean mayPlace(ItemStack stack) {
                return FilmItems.isEmptyRoll(stack) && state() == STATE_IDLE && be.film().isEmpty();
            }
            @Override public boolean mayPickup(Player player) { return state() != STATE_BURNING; }
            @Override public int getMaxStackSize() { return 1; }
            @Override public void onTake(Player player, ItemStack stack) {
                if (be.stateIndex() == STATE_DONE) be.setStateIndex(STATE_IDLE); // fertige Rolle entnommen
                super.onTake(player, stack);
            }
        });
        addPlayerSlots(inv);
        addDataSlots(data);
    }

    private static DeviceBlockEntity resolve(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof DeviceBlockEntity d && d.type() == DeviceType.FILM_BURNER) return d;
        throw new IllegalStateException("Kein Filmbrenner bei " + pos);
    }

    private static ContainerData serverData(DeviceBlockEntity be) {
        return new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case 0 -> be.stateIndex();
                    case 1 -> be.ticksInState();
                    default -> be.reference().isBlank() ? 0 : 1;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 3; }
        };
    }

    public int state() { return data.get(0); }

    /** 0..1 Brennfortschritt (nur waehrend des Brennens sinnvoll). */
    public float progress() {
        int total = DeviceType.FILM_BURNER.state(STATE_BURNING).autoTicks();
        return state() == STATE_BURNING ? Math.min(1f, data.get(1) / (float) total) : (state() == STATE_DONE ? 1f : 0f);
    }

    public boolean referenceSet() { return data.get(2) != 0; }

    /** Server: Referenz aus dem Textfeld uebernehmen. Bereinigt Steuerzeichen, kuerzt, ignoriert waehrend des Brennens. */
    public void setReference(String raw) {
        if (state() == STATE_BURNING || raw == null) return;
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) if (c >= 0x20 && c != 0x7F) sb.append(c);
        String clean = sb.toString().strip();
        if (clean.length() > MAX_REFERENCE) clean = clean.substring(0, MAX_REFERENCE);
        be.setReference(clean);
    }

    @Override
    protected boolean buttonPressed(Player player, int id) {
        if (id != BTN_BURN) return false;
        if (be.stateIndex() != STATE_IDLE) { player.displayClientMessage(Component.translatable("message.realcinema.burner_busy"), true); return false; }
        if (!be.film().is(ModItems.FILM_ROLL_EMPTY.get())) { player.displayClientMessage(Component.translatable("message.realcinema.need_roll"), true); return false; }
        if (be.reference().isBlank()) { player.displayClientMessage(Component.translatable("message.realcinema.need_reference"), true); return false; }
        be.setStateIndex(STATE_BURNING);
        if (be.getLevel() != null) be.getLevel().playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.7f, 1f);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return !be.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
