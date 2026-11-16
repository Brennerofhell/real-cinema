package ch.realcinema.client;

import ch.realcinema.item.DeviceBlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class DeviceItemRenderer extends GeoItemRenderer<DeviceBlockItem> {
    public DeviceItemRenderer() {
        super(new DeviceItemModel());
        // Wie beim Block: nur Geraete mit Glowmaske (sonst Absturz im Inventar, siehe DeviceType#glow).
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, DeviceBlockItem item, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (item.type.glow()) super.render(poseStack, item, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }
        });
    }
}
