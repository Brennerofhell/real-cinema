package ch.realcinema.client;

import ch.realcinema.RealCinema;
import ch.realcinema.item.DeviceBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Block-Items nutzen dieselben Dateien wie der Block. */
public class DeviceItemModel extends GeoModel<DeviceBlockItem> {
    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(DeviceBlockItem item) {
        return rl("geo/block/" + item.type.id + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DeviceBlockItem item) {
        return rl("textures/block/" + item.type.id + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DeviceBlockItem item) {
        return rl("animations/block/" + item.type.id + ".animation.json");
    }
}
