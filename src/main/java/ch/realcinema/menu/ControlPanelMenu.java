package ch.realcinema.menu;

import ch.realcinema.block.DeviceBehaviors;
import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.DeviceType;
import ch.realcinema.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * Bedienpult-Oberflaeche: Play / Pause / Stopp fuer alle Geraete im Umkreis.
 * Buttons: 0 = Play, 1 = Pause, 2 = Stopp. Datenfelder: 0 = Pultzustand (0 gestoppt, 1 spielt, 2 pausiert),
 * 1 = Projektoren mit Rolle in Reichweite, 2 = Lautsprecher in Reichweite.
 */
public final class ControlPanelMenu extends CinemaMenu {
    public static final int BTN_PLAY = 0, BTN_PAUSE = 1, BTN_STOP = 2;

    public final DeviceBlockEntity be;
    public final BlockPos pos;
    private final ContainerData data;

    /** Server. */
    public ControlPanelMenu(int id, Inventory inv, DeviceBlockEntity be) {
        this(id, inv, be, serverData(be));
    }

    /** Client. */
    public ControlPanelMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()), new SimpleContainerData(3));
    }

    private ControlPanelMenu(int id, Inventory inv, DeviceBlockEntity be, ContainerData data) {
        super(ModMenus.CONTROL_PANEL.get(), id);
        this.be = be;
        this.pos = be.getBlockPos();
        this.data = data;
        addPlayerSlots(inv);
        addDataSlots(data);
    }

    private static DeviceBlockEntity resolve(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof DeviceBlockEntity d && d.type() == DeviceType.CONTROL_PANEL) return d;
        throw new IllegalStateException("Kein Bedienpult bei " + pos);
    }

    private static ContainerData serverData(DeviceBlockEntity be) {
        // Die Umkreissuche ist teuer (17^3 Bloecke), deshalb nur alle 10 Ticks.
        return new ContainerData() {
            private long lastScan = Long.MIN_VALUE;
            private int projectors, speakers;

            private void scan() {
                var level = be.getLevel();
                if (level == null) return;
                long now = level.getGameTime();
                if (now - lastScan < 10) return;
                lastScan = now;
                projectors = DeviceBehaviors.projectorsReady(level, be.getBlockPos());
                speakers = DeviceBehaviors.speakersInRange(level, be.getBlockPos());
            }

            @Override public int get(int i) {
                if (i == 0) return be.stateIndex();
                scan();
                return i == 1 ? projectors : speakers;
            }
            @Override public void set(int i, int v) {}
            @Override public int getCount() { return 3; }
        };
    }

    public int panelState() { return data.get(0); }
    public int projectorsReady() { return data.get(1); }
    public int speakers() { return data.get(2); }

    @Override
    protected boolean buttonPressed(Player player, int id) {
        return switch (id) {
            case BTN_PLAY -> DeviceBehaviors.panelCommand(be, player, 1);
            case BTN_PAUSE -> DeviceBehaviors.panelCommand(be, player, 2);
            case BTN_STOP -> DeviceBehaviors.panelCommand(be, player, 0);
            default -> false;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return !be.isRemoved() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
