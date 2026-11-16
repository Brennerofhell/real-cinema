package ch.realcinema.client;

import ch.realcinema.RealCinema;
import ch.realcinema.block.ProjectorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

public class ProjectorModel extends DefaultedBlockGeoModel<ProjectorBlockEntity> {
    public ProjectorModel() {
        // geo/block/projector.geo.json, animations/block/projector.animation.json, textures/block/projector.png
        super(ResourceLocation.fromNamespaceAndPath(RealCinema.MOD_ID, "projector"));
    }
}
