package ch.realcinema.client;

import ch.realcinema.block.DeviceBlock;
import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.ScreenHelper;
import ch.realcinema.client.video.VideoManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Zeichnet das Video fuer die ganze Leinwandflaeche ueber den Master-Block (unten links aus Sicht des Betrachters).
 * Das Bild hat immer 16:9 und wird in die Flaeche eingepasst; der Rest wird schwarz.
 *
 * Lokales Koordinatensystem nach der Drehung: Vorderseite zeigt nach -z, +x ist aus Sicht des Betrachters LINKS.
 * Die Leinwand liegt im Modell bei z = 6/16 hinter der Blockmitte, also lokal bei z = 0.375.
 */
public final class ScreenVideoRenderer {
    private static final float CANVAS_Z = 0.375f;
    private static final float X_LEFT = 7f / 16f;      // linke Kante der Leinwand im Master-Block (aus Betrachtersicht)
    private static final float Y_BOTTOM = 1f / 16f;
    private static final float EDGE = 7f / 16f;        // Rand zur Blockmitte
    private static final float ASPECT = 16f / 9f;

    private ScreenVideoRenderer() {}

    public static void render(DeviceBlockEntity be, PoseStack ps, MultiBufferSource buffers) {
        var level = be.getLevel();
        if (level == null) return;
        Direction facing = be.getBlockState().getValue(DeviceBlock.FACING);
        if (!ScreenHelper.isMaster(level, be.getBlockPos(), facing)) return;
        VideoManager.Entry entry = VideoManager.forScreen(be.getBlockPos());
        if (entry == null || !entry.ready()) return;
        entry.uploadIfNew();

        ScreenHelper.Surface surface = ScreenHelper.surfaceOf(level, be.getBlockPos(), facing);
        float x1 = -(EDGE + (surface.width() - 1));            // rechte Kante (aus Betrachtersicht)
        float y1 = 15f / 16f + (surface.height() - 1);
        float width = X_LEFT - x1, height = y1 - Y_BOTTOM;

        // 16:9 in die Flaeche einpassen
        float vw = width, vh = width / ASPECT;
        if (vh > height) { vh = height; vw = height * ASPECT; }
        float cx = (X_LEFT + x1) / 2f, cy = (Y_BOTTOM + y1) / 2f;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));

        quad(ps, buffers, VideoManager.black(), X_LEFT, Y_BOTTOM, x1, y1, CANVAS_Z - 0.003f);
        quad(ps, buffers, entry.textureId(), cx + vw / 2, cy - vh / 2, cx - vw / 2, cy + vh / 2, CANVAS_Z - 0.006f);
        ps.popPose();
    }

    /** Rechteck von (xa, ya) unten-links bis (xb, yb) oben-rechts aus Betrachtersicht, mit UV 0..1 (v=0 oben). */
    private static void quad(PoseStack ps, MultiBufferSource buffers, ResourceLocation tex,
                             float xa, float ya, float xb, float yb, float z) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        PoseStack.Pose pose = ps.last();
        int light = LightTexture.FULL_BRIGHT;
        vertex(vc, pose, xa, ya, z, 0, 1, light);
        vertex(vc, pose, xb, ya, z, 1, 1, light);
        vertex(vc, pose, xb, yb, z, 1, 0, light);
        vertex(vc, pose, xa, yb, z, 0, 0, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        vc.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, -1);
    }
}
