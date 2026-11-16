package ch.realcinema.menu;

import ch.realcinema.block.ProjectorBlockEntity;
import ch.realcinema.block.ProjectorState;
import ch.realcinema.film.FilmData;
import ch.realcinema.film.FilmItems;
import ch.realcinema.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Projektor-Oberflaeche: Rollenfach (nur bei ausgeschaltetem Projektor), Play / Pause / Stopp, Fortschritt.
 * Buttons: 0 = Play, 1 = Pause, 2 = Stopp (zurueckspulen).
 * Datenfelder: 0 = Zustand (ProjectorState.ordinal), 1 = Wiedergabeposition in Ticks, 2 = Laenge in Ticks (0 ohne Film),
 * 3 = Leinwand zugeordnet, 4 = Rolle eingelegt.
 */
public final class ProjectorMenu extends CinemaMenu {
    public static final int BTN_PLAY = 0, BTN_PAUSE = 1, BTN_STOP = 2;

    public final ProjectorBlockEntity be;
    public final BlockPos pos;
    private final ContainerData data;

    /** Server. */
    public ProjectorMenu(int id, Inventory inv, ProjectorBlockEntity be) {
        this(id, inv, be, serverData(be));
    }

    /** Client. */
    public ProjectorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()), new SimpleContainerData(5));
    }

    private ProjectorMenu(int id, Inventory inv, ProjectorBlockEntity be, ContainerData data) {
        super(ModMenus.PROJECTOR.get(), id);
        this.be = be;
        this.pos = be.getBlockPos();
        this.data = data;
        addSlot(new Slot(new SlotContainer(be::filmStack, be::setFilmStack, FilmItems::toWritten, be::setChanged), 0, 26, 40) {
            @Override public boolean mayPlace(ItemStack stack) {
                return FilmItems.isWritten(stack) && stateOrdinal() == ProjectorState.OFF.ordinal() && !hasFilm();
            }
            @Override public boolean mayPickup(Player player) { return stateOrdinal() == ProjectorState.OFF.ordinal(); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addPlayerSlots(inv);
        addDataSlots(data);
    }

    private static ProjectorBlockEntity resolve(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof ProjectorBlockEntity p) return p;
        throw new IllegalStateException("Kein Projektor bei " + pos);
    }

    private static ContainerData serverData(ProjectorBlockEntity be) {
        return new ContainerData() {
            @Override public int get(int i) {
                FilmData d = be.filmData();
                return switch (i) {
                    case 0 -> be.getState().ordinal();
                    case 1 -> be.playTicks();
                    case 2 -> d == null ? 0 : d.lengthTicks();
                    case 3 -> be.screenPos() != null ? 1 : 0;
                    default -> be.hasFilm() ? 1 : 0;
                };
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 5; }
        };
    }

    public int stateOrdinal() { return data.get(0); }
    public ProjectorState state() {
        return ProjectorState.values()[Math.max(0, Math.min(ProjectorState.values().length - 1, data.get(0)))];
    }
    public int playTicks() { return data.get(1); }
    public int lengthTicks() { return data.get(2); }
    public boolean screenLinked() { return data.get(3) != 0; }
    public boolean hasFilm() { return data.get(4) != 0; }

    public float progress() {
        return lengthTicks() <= 0 ? 0f : Math.min(1f, playTicks() / (float) lengthTicks());
    }

    @Override
    protected boolean buttonPressed(Player player, int id) {
        switch (id) {
            case BTN_PLAY -> {
                if (!be.hasFilm()) { player.displayClientMessage(Component.translatable("message.realcinema.no_film"), true); return false; }
                return be.requestStart();
            }
            case BTN_PAUSE -> {
                be.requestStop();
                return true;
            }
            case BTN_STOP -> {
                be.stopAndRewind();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !be.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
