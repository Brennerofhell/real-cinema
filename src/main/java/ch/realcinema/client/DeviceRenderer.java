package ch.realcinema.client;

import ch.realcinema.block.DeviceBlock;
import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.DeviceType;
import ch.realcinema.block.ScreenHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class DeviceRenderer extends GeoBlockRenderer<DeviceBlockEntity> {
    public DeviceRenderer() {
        super(new DeviceModel());
        // Nur Geraete mit Glowmaske: Der Layer wuerde sonst eine fehlende bzw. leere Maske laden (GeckoLib stuerzt dabei ab).
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, DeviceBlockEntity be, BakedGeoModel model, net.minecraft.client.renderer.RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (be.type().glow()) super.render(poseStack, be, model, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
            }
        });
    }

    /** Eine grosse Leinwand muss auch dann gezeichnet werden, wenn nur ein anderer Teil der Flaeche im Bild ist. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(DeviceBlockEntity be) {
        return be.type() == DeviceType.CINEMA_SCREEN
                ? new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(ScreenHelper.MAX_SIZE)
                : super.getRenderBoundingBox(be);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    /** Leinwand: Rahmenkanten zu Nachbarbloecken ausblenden, damit eine durchgehende Flaeche entsteht. */
    @Override
    public void preRender(PoseStack poseStack, DeviceBlockEntity be, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        super.preRender(poseStack, be, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        if (be.type() != DeviceType.CINEMA_SCREEN || be.getLevel() == null) return;
        Direction f = be.getBlockState().getValue(DeviceBlock.FACING);
        var level = be.getLevel();
        var pos = be.getBlockPos();
        // Die Bone-Namen beziehen sich auf die Modellachse: edge_left liegt bei -x = aus Sicht des Betrachters rechts.
        model.getBone("edge_left").ifPresent(b -> b.setHidden(ScreenHelper.isScreen(level, pos.relative(ScreenHelper.right(f)), f)));
        model.getBone("edge_right").ifPresent(b -> b.setHidden(ScreenHelper.isScreen(level, pos.relative(ScreenHelper.left(f)), f)));
        model.getBone("edge_top").ifPresent(b -> b.setHidden(ScreenHelper.isScreen(level, pos.above(), f)));
        model.getBone("edge_bottom").ifPresent(b -> b.setHidden(ScreenHelper.isScreen(level, pos.below(), f)));
    }

    @Override
    public void render(DeviceBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        super.render(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        if (be.type() == DeviceType.CINEMA_SCREEN) {
            ScreenVideoRenderer.render(be, poseStack, bufferSource);
        }
    }
}
