package ch.realcinema.client;

import ch.realcinema.RealCinema;
import ch.realcinema.item.ProjectorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Das Item nutzt dieselben Dateien wie der Block (eine Quelle, keine Duplikate). */
public class ProjectorItemModel extends GeoModel<ProjectorItem> {
    private static final ResourceLocation MODEL = rl("geo/block/projector.geo.json");
    private static final ResourceLocation TEXTURE = rl("textures/block/projector.png");
    private static final ResourceLocation ANIM = rl("animations/block/projector.animation.json");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, path);
    }

    @Override public ResourceLocation getModelResource(ProjectorItem animatable) { return MODEL; }
    @Override public ResourceLocation getTextureResource(ProjectorItem animatable) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(ProjectorItem animatable) { return ANIM; }
}
