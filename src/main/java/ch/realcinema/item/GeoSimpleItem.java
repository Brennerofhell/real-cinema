package ch.realcinema.item;

import ch.realcinema.client.SimpleItemRenderer;
import ch.realcinema.film.FilmData;
import ch.realcinema.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Einfaches Geo-Item (Filmrollen, Filmdose, Klappe).
 * Dateien: geo/item/{model}.geo.json, animations/item/{model}.animation.json, textures/item/{texture}.png.
 * Optional gibt es eine Einmal-Animation {@code triggerAnim}, die bei Rechtsklick fuer alle Spieler ausgeloest wird.
 */
public class GeoSimpleItem extends Item implements GeoItem {
    public final String model;
    public final String texture;
    @Nullable
    private final String triggerAnim;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GeoSimpleItem(Properties properties, String model, String texture, @Nullable String triggerAnim) {
        super(properties);
        this.model = model;
        this.texture = texture;
        this.triggerAnim = triggerAnim;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SimpleItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new SimpleItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<GeoSimpleItem> controller = new AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation." + model + ".idle")));
        if (triggerAnim != null) {
            controller.triggerableAnim(triggerAnim, RawAnimation.begin().thenPlay("animation." + model + "." + triggerAnim));
        }
        controllers.add(controller);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (triggerAnim != null && level instanceof ServerLevel serverLevel) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "main", triggerAnim);
        }
        return triggerAnim != null ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FilmData film = stack.get(ModDataComponents.FILM.get());
        if (film != null) {
            tooltip.add(Component.translatable("tooltip.realcinema.film_reference", film.reference()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.realcinema.film_length", film.seconds()).withStyle(ChatFormatting.DARK_GRAY));
            if (film.startTicks() > 0) {
                tooltip.add(Component.translatable("tooltip.realcinema.film_segment", film.range()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    /** Eine eingelegte Rolle schimmert, damit man sie von einer beschriebenen unterscheidet. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.FILM.get()) && stack.is(ch.realcinema.registry.ModItems.FILM_ROLL_LOADED.get());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
