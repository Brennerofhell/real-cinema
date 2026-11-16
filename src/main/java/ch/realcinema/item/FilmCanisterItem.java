package ch.realcinema.item;

import ch.realcinema.film.FilmData;
import ch.realcinema.film.FilmItems;
import ch.realcinema.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Filmdose: nimmt bis zu {@link #CAPACITY} Rollen auf (leer, beschrieben oder eingelegt, jede einzeln).
 * <ul>
 *   <li>Rechtsklick: letzte Rolle herausnehmen.</li>
 *   <li>Schleichen + Rechtsklick mit einer Rolle in der anderen Hand: hineinlegen.</li>
 *   <li>Im Inventar wie ein Beutel: Rolle mit dem Cursor auf die Dose klicken legt sie hinein, mit leerem Cursor auf die Dose
 *       klicken nimmt die letzte heraus.</li>
 * </ul>
 */
public class FilmCanisterItem extends GeoSimpleItem {
    public static final int CAPACITY = 8;

    public FilmCanisterItem(Properties properties, String model, String texture) {
        super(properties.stacksTo(1), model, texture, null);
    }

    // ---- Inhalt -------------------------------------------------------------------------------------------------

    public static List<ItemStack> contents(ItemStack canister) {
        List<ItemStack> list = new ArrayList<>();
        canister.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().forEach(s -> list.add(s.copy()));
        return list;
    }

    private static void store(ItemStack canister, List<ItemStack> list) {
        canister.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }

    /** @return true, wenn eine Rolle eingelegt wurde (verbraucht genau eine aus stack). */
    public static boolean tryInsert(ItemStack canister, ItemStack roll, Player player) {
        if (!FilmItems.isAnyRoll(roll)) return false;
        List<ItemStack> list = contents(canister);
        if (list.size() >= CAPACITY) {
            player.displayClientMessage(Component.translatable("message.realcinema.canister_full"), true);
            return false;
        }
        list.add(roll.copyWithCount(1));
        store(canister, list);
        if (!player.getAbilities().instabuild) roll.shrink(1);
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1.0f);
        return true;
    }

    /** Nimmt die letzte Rolle heraus oder gibt leer zurueck. */
    public static ItemStack takeLast(ItemStack canister, Player player) {
        List<ItemStack> list = contents(canister);
        if (list.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = list.remove(list.size() - 1);
        store(canister, list);
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1.0f);
        return out;
    }

    // ---- Benutzung in der Welt ----------------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack canister = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(canister);
        if (player.isShiftKeyDown()) {
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!tryInsert(canister, other, player)) {
                player.displayClientMessage(Component.translatable("message.realcinema.canister_need_roll"), true);
            }
        } else {
            ItemStack out = takeLast(canister, player);
            if (out.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.realcinema.canister_empty"), true);
            } else {
                player.getInventory().placeItemBackInInventory(out);
            }
        }
        return InteractionResultHolder.success(canister);
    }

    // ---- Beutel-Verhalten im Inventar ---------------------------------------------------------------------------

    /** Die Dose liegt auf dem Cursor und wird auf einen Slot geklickt: leerer Slot = Rolle herausgeben, Rolle im Slot = einlegen. */
    @Override
    public boolean overrideStackedOnOther(ItemStack canister, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (target.isEmpty()) {
            ItemStack out = takeLast(canister, player);
            if (out.isEmpty()) return false;
            ItemStack leftover = slot.safeInsert(out);
            if (!leftover.isEmpty()) tryInsert(canister, leftover, player); // passt nicht in den Slot: zurueck in die Dose
            return true;
        }
        return tryInsert(canister, target, player);
    }

    /** Die Dose liegt im Slot und der Cursor traegt etwas: einlegen; leerer Cursor = letzte Rolle auf den Cursor nehmen. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack canister, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (other.isEmpty()) {
            ItemStack out = takeLast(canister, player);
            if (out.isEmpty()) return false;
            access.set(out);
            return true;
        }
        return tryInsert(canister, other, player);
    }

    // ---- Anzeige ------------------------------------------------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> list = contents(stack);
        tooltip.add(Component.translatable("tooltip.realcinema.canister_count", list.size(), CAPACITY).withStyle(ChatFormatting.GRAY));
        int shown = 0;
        for (int i = list.size() - 1; i >= 0 && shown < 4; i--, shown++) {
            ItemStack roll = list.get(i);
            FilmData d = roll.get(ModDataComponents.FILM.get());
            Component line = d == null
                    ? roll.getHoverName()
                    : Component.literal(d.reference() + "  " + d.range());
            tooltip.add(Component.literal(" - ").append(line).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (list.size() > 4) tooltip.add(Component.literal(" ... +" + (list.size() - 4)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
