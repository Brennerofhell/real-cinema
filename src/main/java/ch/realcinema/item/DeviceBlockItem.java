package ch.realcinema.item;

import ch.realcinema.block.DeviceType;
import ch.realcinema.client.DeviceItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/** Block-Item eines Geraets, im Inventar mit dem animierten Geo-Modell gerendert (Ruhepose). */
public class DeviceBlockItem extends BlockItem implements GeoItem {
    public final DeviceType type;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DeviceBlockItem(DeviceType type, Block block, Properties properties) {
        super(block, properties);
        this.type = type;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private DeviceItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new DeviceItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
