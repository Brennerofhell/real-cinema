package ch.realcinema.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/** Ein-Slot-Container, der auf ein Feld einer Block-Entity zeigt (Lesen, Schreiben, Umwandlung beim Entnehmen). */
public final class SlotContainer implements Container {
    private final Supplier<ItemStack> getter;
    private final Consumer<ItemStack> setter;
    private final UnaryOperator<ItemStack> onTake;
    private final Runnable changed;

    public SlotContainer(Supplier<ItemStack> getter, Consumer<ItemStack> setter, UnaryOperator<ItemStack> onTake, Runnable changed) {
        this.getter = getter;
        this.setter = setter;
        this.onTake = onTake;
        this.changed = changed;
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return getter.get().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return getter.get(); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return removeItemNoUpdate(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getter.get();
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = onTake.apply(current);
        setter.accept(ItemStack.EMPTY);
        changed.run();
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setter.accept(stack);
        changed.run();
    }

    @Override public int getMaxStackSize() { return 1; }
    @Override public void setChanged() { changed.run(); }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { setter.accept(ItemStack.EMPTY); }
}
