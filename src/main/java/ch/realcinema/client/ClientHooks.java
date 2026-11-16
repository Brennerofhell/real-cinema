package ch.realcinema.client;

import ch.realcinema.block.ProjectorBlockEntity;
import ch.realcinema.client.video.VideoManager;

/**
 * Einziger Uebergang von gemeinsamem Code (Block-Entities) in Client-Klassen. Wird nur unter {@code level.isClientSide}
 * aufgerufen, damit ein dedizierter Server diese Klasse nie laedt.
 */
public final class ClientHooks {
    private ClientHooks() {}

    public static void projectorTick(ProjectorBlockEntity be) {
        VideoManager.tick(be);
    }

    public static void projectorRemoved(ProjectorBlockEntity be) {
        VideoManager.remove(be.getBlockPos());
    }
}
