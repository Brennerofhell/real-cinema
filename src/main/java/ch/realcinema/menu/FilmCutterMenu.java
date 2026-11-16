package ch.realcinema.menu;

import ch.realcinema.block.DeviceBehaviors;
import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.DeviceType;
import ch.realcinema.film.FilmItems;
import ch.realcinema.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Filmschneider-Oberflaeche: zwei Rollenfaecher (A und B). Buttons: 0 / 1 / 2 = A bei 25 / 50 / 75 % schneiden,
 * 3 = A und B zusammenfuegen. Ergebnisse landen im Inventar, offene Faecher werden beim Schliessen zurueckgegeben.
 */
public final class FilmCutterMenu extends CinemaMenu {
    public static final int BTN_CUT_25 = 0, BTN_CUT_50 = 1, BTN_CUT_75 = 2, BTN_JOIN = 3;
    private static final double[] FRACTIONS = {0.25, 0.5, 0.75};

    public final DeviceBlockEntity be;
    public final BlockPos pos;
    private final Container container = new SimpleContainer(2);

    /** Server. */
    public FilmCutterMenu(int id, Inventory inv, DeviceBlockEntity be) {
        super(ModMenus.FILM_CUTTER.get(), id);
        this.be = be;
        this.pos = be.getBlockPos();
        addSlots(inv);
    }

    /** Client. */
    public FilmCutterMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(ModMenus.FILM_CUTTER.get(), id);
        BlockPos p = buf.readBlockPos();
        if (!(inv.player.level().getBlockEntity(p) instanceof DeviceBlockEntity d) || d.type() != DeviceType.FILM_CUTTER) {
            throw new IllegalStateException("Kein Filmschneider bei " + p);
        }
        this.be = d;
        this.pos = p;
        addSlots(inv);
    }

    private void addSlots(Inventory inv) {
        for (int i = 0; i < 2; i++) {
            addSlot(new Slot(container, i, 26, 34 + i * 24) {
                @Override public boolean mayPlace(ItemStack stack) { return FilmItems.isWritten(stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        addPlayerSlots(inv);
    }

    public ItemStack slotA() { return container.getItem(0); }
    public ItemStack slotB() { return container.getItem(1); }

    @Override
    protected boolean buttonPressed(Player player, int id) {
        if (id >= BTN_CUT_25 && id <= BTN_CUT_75) {
            ItemStack[] parts = FilmItems.cut(slotA(), FRACTIONS[id]);
            if (parts == null) { player.displayClientMessage(Component.translatable("message.realcinema.too_short"), true); return false; }
            container.setItem(0, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(parts[0]);
            player.getInventory().placeItemBackInInventory(parts[1]);
            DeviceBehaviors.cutterEffects(be);
            return true;
        }
        if (id == BTN_JOIN) {
            ItemStack joined = FilmItems.join(slotA(), slotB());
            if (joined == null) { player.displayClientMessage(Component.translatable("message.realcinema.cannot_join"), true); return false; }
            container.setItem(0, ItemStack.EMPTY);
            container.setItem(1, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(joined);
            DeviceBehaviors.cutterEffects(be);
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        clearContainer(player, container); // Rollen nicht im Fach vergessen
    }

    @Override
    public boolean stillValid(Player player) {
        return !be.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
