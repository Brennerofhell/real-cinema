package ch.realcinema.registry;

import ch.realcinema.RealCinema;
import ch.realcinema.block.DeviceBlock;
import ch.realcinema.block.DeviceType;
import ch.realcinema.block.ProjectorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealCinema.MOD_ID);

    public static final DeferredBlock<ProjectorBlock> PROJECTOR = BLOCKS.register("projector",
            () -> new ProjectorBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(ProjectorBlock.LIT) ? 10 : 0)));

    /** Alle datengetriebenen Geraete, ein Block je {@link DeviceType}. */
    public static final Map<DeviceType, DeferredBlock<DeviceBlock>> DEVICES = new EnumMap<>(DeviceType.class);

    static {
        for (DeviceType type : DeviceType.values()) {
            boolean soft = type == DeviceType.CINEMA_SCREEN || type == DeviceType.CINEMA_SEAT;
            DEVICES.put(type, BLOCKS.register(type.id, () -> new DeviceBlock(type, BlockBehaviour.Properties.of()
                    .strength(2.0f, 5.0f)
                    .sound(soft ? SoundType.WOOL : SoundType.METAL)
                    .noOcclusion())));
        }
    }

    private ModBlocks() {}
}
