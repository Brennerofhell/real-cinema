package ch.realcinema.item;

import ch.realcinema.block.DeviceBehaviors;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Filmklappe: Rechtsklick klappt (Animation fuer alle Spieler sichtbar, mit Ton) und bedient das naechste Bedienpult im
 * Umkreis ("Action!" startet, Schleichen + Klappe stoppt). Mit kurzer Abklingzeit.
 */
public class ClapperboardItem extends GeoSimpleItem {
    public static final int COOLDOWN_TICKS = 12;

    public ClapperboardItem(Properties properties, String model, String texture) {
        super(properties.stacksTo(1), model, texture, "clap");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand); // loest die Klapp-Animation aus
        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 1.0f, 1.5f);
            DeviceBehaviors.clap(level, player);
        }
        return result;
    }
}
