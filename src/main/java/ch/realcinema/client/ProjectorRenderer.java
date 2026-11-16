package ch.realcinema.client;

import ch.realcinema.block.ProjectorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ProjectorRenderer extends GeoBlockRenderer<ProjectorBlockEntity> {
    public ProjectorRenderer() {
        super(new ProjectorModel());
        // projector_glowmask.png: Linse und Lampe leuchten unabhaengig vom Umgebungslicht.
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** Projektoren weiter weg muessen nicht gerendert werden (Performance bei vielen Geraeten). */
    @Override
    public int getViewDistance() {
        return 48;
    }
}
