package ch.realcinema.registry;

import ch.realcinema.RealCinema;
import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.ProjectorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RealCinema.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProjectorBlockEntity>> PROJECTOR =
            BLOCK_ENTITIES.register("projector", () ->
                    BlockEntityType.Builder.of(ProjectorBlockEntity::new, ModBlocks.PROJECTOR.get()).build(null));

    /** Ein Typ fuer alle datengetriebenen Geraete. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeviceBlockEntity>> DEVICE =
            BLOCK_ENTITIES.register("device", () -> BlockEntityType.Builder.of(DeviceBlockEntity::new,
                    ModBlocks.DEVICES.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    private ModBlockEntities() {}
}
