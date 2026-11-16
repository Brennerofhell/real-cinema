package ch.realcinema.client;

import ch.realcinema.RealCinema;
import ch.realcinema.item.GeoSimpleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SimpleItemModel extends GeoModel<GeoSimpleItem> {
    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(GeoSimpleItem item) {
        return rl("geo/item/" + item.model + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoSimpleItem item) {
        return rl("textures/item/" + item.texture + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeoSimpleItem item) {
        return rl("animations/item/" + item.model + ".animation.json");
    }
}
