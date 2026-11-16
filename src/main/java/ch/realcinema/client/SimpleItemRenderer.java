package ch.realcinema.client;

import ch.realcinema.item.GeoSimpleItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SimpleItemRenderer extends GeoItemRenderer<GeoSimpleItem> {
    public SimpleItemRenderer() {
        super(new SimpleItemModel());
    }
}
