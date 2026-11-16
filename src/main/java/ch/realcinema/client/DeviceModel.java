package ch.realcinema.client;

import ch.realcinema.RealCinema;
import ch.realcinema.block.DeviceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Waehlt Modell, Textur und Animationsdatei je nach Geraetetyp (geo/block/{id}.geo.json usw.). */
public class DeviceModel extends GeoModel<DeviceBlockEntity> {
    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, path);
    }

    @Override
    public ResourceLocation getModelResource(DeviceBlockEntity be) {
        return rl("geo/block/" + be.type().id + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DeviceBlockEntity be) {
        return rl("textures/block/" + be.type().id + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DeviceBlockEntity be) {
        return rl("animations/block/" + be.type().id + ".animation.json");
    }
}
