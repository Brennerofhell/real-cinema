package ch.realcinema.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsame Basis der Geraete-Menues: Standard-Spielerinventar unten, Shift-Klick, Reichweitenpruefung.
 * Layout: Das Menue ist 176 x 184 gross, Inventar bei y = 102, Hotbar bei y = 160.
 */
public abstract class CinemaMenu extends AbstractContainerMenu {
    public static final int INV_X = 8, INV_Y = 102, HOTBAR_Y = 160;

    /** Anzahl der Geraete-Slots (stehen vor den 36 Spieler-Slots). */
    protected int containerSlots;

    protected CinemaMenu(@Nullable MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /** Muss nach dem Hinzufuegen der Geraete-Slots aufgerufen werden. */
    protected void addPlayerSlots(Inventory inventory) {
        containerSlots = slots.size();
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, INV_X + col * 18, HOTBAR_Y));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < containerSlots) {
            if (!moveItemStackTo(stack, containerSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, containerSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** Aus der Taste der GUI: der Server prueft immer selbst. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        return buttonPressed(player, id);
    }

    protected abstract boolean buttonPressed(Player player, int id);
}
