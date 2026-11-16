package ch.realcinema.block;

import ch.realcinema.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import ch.realcinema.film.FilmData;
import ch.realcinema.film.FilmItems;
import ch.realcinema.registry.ModDataComponents;
import net.minecraft.world.Containers;
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
 * Multiplayer-Konzept:
 * - Der Server haelt den einzigen Zustand ({@link ProjectorState}) und schaltet STARTING/STOPPING nach festen Tick-Zeiten weiter.
 * - Der Zustand wird per Block-Entity-Update an alle Clients gesendet. Es werden keine Animationsdaten uebertragen.
 * - Jeder Client rampt die Mechanik-Geschwindigkeit deterministisch (feste Beschleunigung pro Tick) auf den Zielwert des
 *   Zustands. Alle Spieler sehen so denselben Hoch-/Auslauf, und die Spulen drehen nie ruckartig zurueck.
 */
public class ProjectorBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int STARTUP_TICKS = 40;
    public static final int SPINDOWN_TICKS = 60;

    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("animation.projector.mechanism_spin");
    private static final RawAnimation LAMP_OFF = RawAnimation.begin().thenPlayAndHold("animation.projector.lamp_off");
    private static final RawAnimation LAMP_WARMUP = RawAnimation.begin().thenPlayAndHold("animation.projector.lamp_warmup");
    private static final RawAnimation LAMP_RUN = RawAnimation.begin().thenLoop("animation.projector.lamp_run");
    private static final RawAnimation LAMP_COOLDOWN = RawAnimation.begin().thenPlayAndHold("animation.projector.lamp_cooldown");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private ProjectorState state = ProjectorState.OFF;
    private int ticksInState = 0;

    // Eingelegte Filmrolle (nur serverseitig relevant) und bisherige Abspielzeit in Ticks.
    private ItemStack film = ItemStack.EMPTY;
    private int playTicks = 0;
    private boolean rewindWhenOff = false;

    // Leinwand, auf die dieser Projektor gerade projiziert (Master-Block), vom Server beim Start gewaehlt und synchronisiert.
    private BlockPos screenPos = null;

    // Nur Client: aus dem Update-Paket. Daraus wird die Videoposition geschaetzt.
    private String clientReference = "";
    private int clientStartTicks = 0;
    private int syncPlayTicks = 0;
    private long syncGameTime = 0;

    // Nur clientseitig relevant: Geschwindigkeit 0..1 der Mechanik-Animation.
    private float spinSpeed = 0f;
    private float prevSpinSpeed = 0f;

    public ProjectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PROJECTOR.get(), pos, blockState);
    }

    public ProjectorState getState() {
        return state;
    }

    // ---- Film ---------------------------------------------------------------------------------------------------

    public boolean hasFilm() {
        return !film.isEmpty();
    }

    /** Daten der eingelegten Rolle oder null. */
    public FilmData filmData() {
        return film.isEmpty() ? null : film.get(ModDataComponents.FILM.get());
    }

    public int playTicks() {
        return playTicks;
    }

    /** Die eingelegte Rolle (in der "eingelegt"-Variante) fuer das GUI-Rollenfach. */
    public ItemStack filmStack() {
        return film;
    }

    /** GUI: Rolle setzen oder entfernen. Die Zulaessigkeit (nur bei ausgeschaltetem Projektor) prueft das Rollenfach. */
    public void setFilmStack(ItemStack stack) {
        film = stack.isEmpty() ? ItemStack.EMPTY : FilmItems.toLoaded(stack);
        playTicks = 0;
        setChanged();
    }

    /** Stoppen und an den Anfang zurueckspulen (nach dem Auslauf, wenn noch am Laufen). */
    public void stopAndRewind() {
        if (state == ProjectorState.OFF) {
            playTicks = 0;
            setChanged();
        } else {
            rewindWhenOff = true;
            requestStop();
        }
    }

    public BlockPos screenPos() {
        return screenPos;
    }

    /** Client: Videoreferenz der eingelegten Rolle (leer, wenn keine). */
    public String clientReference() {
        return clientReference;
    }

    /** Client: Projektor zeigt gerade Bild (startet, laeuft oder laeuft aus/pausiert). */
    public boolean isShowingPicture() {
        return state != ProjectorState.OFF && !clientReference.isEmpty();
    }

    /** Client: laeuft die Wiedergabezeit gerade weiter? */
    public boolean isAdvancing() {
        return state == ProjectorState.RUNNING;
    }

    /** Client: geschaetzte Wiedergabeposition in Sekunden (Serverstand + seit dem Update vergangene Ticks). */
    public double playbackSeconds() {
        long elapsed = (isAdvancing() && level != null) ? Math.max(0, level.getGameTime() - syncGameTime) : 0;
        return (clientStartTicks + syncPlayTicks + elapsed) / 20.0;
    }

    /** Legt eine beschriebene Rolle ein. Nur bei ausgeschaltetem Projektor und leerem Fach. */
    public boolean insertFilm(ItemStack stack) {
        if (hasFilm() || state != ProjectorState.OFF || !stack.has(ModDataComponents.FILM.get())) return false;
        film = FilmItems.toLoaded(stack); // eingelegt: Variante mit "eingelegt"-Textur
        playTicks = 0;
        setChanged();
        return true;
    }

    /** Entnimmt die Rolle (nur bei ausgeschaltetem Projektor). */
    public ItemStack ejectFilm() {
        if (!hasFilm() || state != ProjectorState.OFF) return ItemStack.EMPTY;
        ItemStack out = FilmItems.toWritten(film);
        film = ItemStack.EMPTY;
        playTicks = 0;
        setChanged();
        return out;
    }

    public void dropContents() {
        if (level != null && !level.isClientSide && hasFilm()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, FilmItems.toWritten(film));
            film = ItemStack.EMPTY;
        }
    }

    // ---- Serverlogik --------------------------------------------------------------------------------------------

    /** @return false, wenn ein Start mangels Film nicht moeglich war. */
    public boolean toggle() {
        if (state == ProjectorState.OFF || state == ProjectorState.STOPPING) {
            return requestStart();
        }
        requestStop();
        return true;
    }

    /** @return true, wenn der Projektor jetzt laeuft oder hochfaehrt (ohne Film nicht moeglich). */
    public boolean requestStart() {
        if (!hasFilm()) return false;
        rewindWhenOff = false; // wer wieder startet, will nicht zurueckspulen
        if (state == ProjectorState.OFF || state == ProjectorState.STOPPING) {
            setState(ProjectorState.STARTING);
        }
        return state == ProjectorState.STARTING || state == ProjectorState.RUNNING;
    }

    public void requestStop() {
        if (state == ProjectorState.STARTING || state == ProjectorState.RUNNING) {
            setState(ProjectorState.STOPPING);
        }
    }

    private void setState(ProjectorState newState) {
        this.state = newState;
        this.ticksInState = 0;
        if (level != null && !level.isClientSide) {
            if (newState == ProjectorState.STARTING) {
                screenPos = ScreenHelper.findScreenFor(level, worldPosition, getBlockState().getValue(ProjectorBlock.FACING));
            } else if (newState == ProjectorState.OFF) {
                screenPos = null;
                if (rewindWhenOff) { playTicks = 0; rewindWhenOff = false; }
            }
        }
        setChanged();
        if (level != null) {
            BlockState bs = getBlockState();
            boolean lit = newState.lampOn;
            if (bs.getValue(ProjectorBlock.LIT) != lit) {
                level.setBlock(worldPosition, bs.setValue(ProjectorBlock.LIT, lit), Block.UPDATE_ALL);
            }
            // Immer senden: setBlock allein synchronisiert den Block-Entity-Zustand nicht.
            level.sendBlockUpdated(worldPosition, bs, getBlockState(), Block.UPDATE_ALL);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState bs, ProjectorBlockEntity be) {
        be.ticksInState++;
        if (level.isClientSide) {
            ch.realcinema.client.ClientHooks.projectorTick(be);
            be.prevSpinSpeed = be.spinSpeed;
            float target = be.state.targetSpeed();
            if (target > be.spinSpeed) {
                be.spinSpeed = Math.min(target, be.spinSpeed + 1f / STARTUP_TICKS);
            } else if (target < be.spinSpeed) {
                be.spinSpeed = Math.max(target, be.spinSpeed - 1f / SPINDOWN_TICKS);
            }
            return;
        }
        // Alle 5 s die Wiedergabeposition an die Clients schicken, damit lange Filme nicht auseinanderlaufen.
        if (be.state == ProjectorState.RUNNING && be.ticksInState % 100 == 0) {
            level.sendBlockUpdated(pos, bs, bs, Block.UPDATE_ALL);
        }
        if (be.state == ProjectorState.RUNNING && be.hasFilm()) {
            be.playTicks++;
            FilmData data = be.filmData();
            if (data != null && be.playTicks >= data.lengthTicks()) {
                be.playTicks = 0;
                be.requestStop();
                DeviceBehaviors.notifyFilmFinished(level, pos);
            }
        }
        if (be.state == ProjectorState.STARTING && be.ticksInState >= STARTUP_TICKS) {
            be.setState(ProjectorState.RUNNING);
        } else if (be.state == ProjectorState.STOPPING && be.ticksInState >= SPINDOWN_TICKS) {
            be.setState(ProjectorState.OFF);
        }
    }

    // ---- Persistenz & Sync --------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("State", state.name());
        if (!film.isEmpty()) tag.put("Film", film.save(registries));
        tag.putInt("PlayTicks", playTicks);
        if (screenPos != null) tag.putLong("Screen", screenPos.asLong());
        FilmData data = filmData();
        tag.putString("Ref", data == null ? "" : data.reference());
        tag.putInt("Start", data == null ? 0 : data.startTicks());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            state = ProjectorState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            state = ProjectorState.OFF;
        }
        film = tag.contains("Film") ? ItemStack.parse(registries, tag.getCompound("Film")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        playTicks = tag.getInt("PlayTicks");
        // Gilt fuer Laden von der Platte UND fuer Client-Updates. Uebergangszustaende werden deshalb nicht umgewandelt:
        // Auf dem Server laufen sie nach dem Laden ueber tick() zu Ende, auf dem Client sollen sie sichtbar sein.
        screenPos = tag.contains("Screen") ? BlockPos.of(tag.getLong("Screen")) : null;
        clientReference = tag.getString("Ref");
        clientStartTicks = tag.getInt("Start");
        syncPlayTicks = playTicks;
        syncGameTime = level != null ? level.getGameTime() : 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("State", state.name());
        tag.putInt("PlayTicks", playTicks);
        if (screenPos != null) tag.putLong("Screen", screenPos.asLong());
        FilmData data = filmData();
        tag.putString("Ref", data == null ? "" : data.reference());
        tag.putInt("Start", data == null ? 0 : data.startTicks());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) ch.realcinema.client.ClientHooks.projectorRemoved(this);
    }

    // ---- GeckoLib -----------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Die Mechanik laeuft immer, nur die Abspielgeschwindigkeit aendert sich. Dadurch bleibt die Spulenphase beim
        // Hoch-/Auslaufen stetig, und im Stillstand wird die Pose eingefroren statt zurueckgesetzt.
        controllers.add(new AnimationController<>(this, "mechanism", 0, this::mechanismPredicate));
        controllers.add(new AnimationController<>(this, "lamp", 4, this::lampPredicate));
    }

    private PlayState mechanismPredicate(AnimationState<ProjectorBlockEntity> animState) {
        float speed = prevSpinSpeed + (spinSpeed - prevSpinSpeed) * animState.getPartialTick();
        animState.getController().setAnimationSpeed(Math.max(speed, 0.0f));
        return animState.setAndContinue(SPIN);
    }

    private PlayState lampPredicate(AnimationState<ProjectorBlockEntity> animState) {
        return animState.setAndContinue(switch (state) {
            case OFF -> LAMP_OFF;
            case STARTING -> LAMP_WARMUP;
            case RUNNING -> LAMP_RUN;
            case STOPPING -> LAMP_COOLDOWN;
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
