package ch.realcinema.block;

import ch.realcinema.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Generische Block-Entity fuer alle {@link DeviceType}-Geraete.
 * Multiplayer: Der Server haelt nur den Zustandsindex und schaltet ggf. nach fester Tick-Zeit weiter. Der Index wird per
 * Block-Entity-Update an alle Clients gesendet, die daraus die Animation waehlen (keine Animationsdaten im Netz).
 */
public class DeviceBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int state = 0;
    private int ticksInState = 0;

    // Inhalt (Filmbrenner): eingelegte Rolle und gewaehlte Videoreferenz. Nur serverseitig relevant.
    private ItemStack film = ItemStack.EMPTY;
    private String reference = "";

    public DeviceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEVICE.get(), pos, blockState);
    }

    public DeviceType type() {
        return ((DeviceBlock) getBlockState().getBlock()).type;
    }

    public int stateIndex() {
        return state;
    }

    // ---- Server -------------------------------------------------------------------------------------------------

    public ItemStack film() { return film; }
    public int ticksInState() { return ticksInState; }
    public String reference() { return reference; }

    public void setFilm(ItemStack stack) {
        film = stack;
        setChanged();
    }

    public void setReference(String ref) {
        reference = ref;
        setChanged();
    }

    public ItemStack takeFilm() {
        ItemStack out = film;
        film = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public void dropContents() {
        if (level != null && !level.isClientSide && !film.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, film);
            film = ItemStack.EMPTY;
        }
    }

    /** Leere Hand. Geraetespezifische Logik zuerst, sonst der generische Klick-Zustandswechsel. */
    public void interact(Player player, boolean sneaking) {
        if (DeviceBehaviors.click(this, player, sneaking)) return;
        DeviceType.State s = type().state(state);
        setStateIndex(sneaking ? s.sneakClick() : s.click());
    }

    public void setStateIndex(int index) {
        int clamped = Math.floorMod(index, type().states.length);
        if (clamped == state) return;
        state = clamped;
        ticksInState = 0;
        DeviceBehaviors.entered(this, clamped);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState bs, DeviceBlockEntity be) {
        if (level.isClientSide) return;
        be.ticksInState++;
        DeviceType.State s = be.type().state(be.state);
        if (s.autoTicks() > 0 && be.ticksInState >= s.autoTicks()) {
            be.setStateIndex(s.autoNext());
        }
    }

    // ---- Persistenz & Sync --------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("State", state);
        if (!film.isEmpty()) tag.put("Film", film.save(registries));
        tag.putString("Reference", reference);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Gilt fuer Laden von der Platte und fuer Client-Updates. Zustaende mit Auto-Timer laufen nach dem Laden auf dem
        // Server ueber tick() ordentlich zu Ende, deshalb wird hier nichts zurueckgesetzt.
        state = tag.getInt("State");
        film = tag.contains("Film") ? ItemStack.parse(registries, tag.getCompound("Film")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        reference = tag.getString("Reference");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("State", state);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- GeckoLib -----------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Transition-Laenge ist je Geraet verschieden (Vorhang langsam, Knopf schnell), daher aus dem Typ gelesen.
        controllers.add(new AnimationController<>(this, "main", 4, this::mainPredicate));
        controllers.add(new AnimationController<>(this, "aux", 4, this::auxPredicate));
    }

    private PlayState mainPredicate(AnimationState<DeviceBlockEntity> animState) {
        DeviceType type = type();
        animState.getController().transitionLength(type.transitionTicks);
        return animState.setAndContinue(type.mainAnimation(Math.floorMod(state, type.states.length)));
    }

    private PlayState auxPredicate(AnimationState<DeviceBlockEntity> animState) {
        DeviceType type = type();
        RawAnimation aux = type.auxAnimation(Math.floorMod(state, type.states.length));
        if (aux == null) return PlayState.STOP;
        return animState.setAndContinue(aux);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
