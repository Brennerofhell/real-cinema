package ch.realcinema.client;

import ch.realcinema.item.ProjectorItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ProjectorItemRenderer extends GeoItemRenderer<ProjectorItem> {
    public ProjectorItemRenderer() {
        super(new ProjectorItemModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
