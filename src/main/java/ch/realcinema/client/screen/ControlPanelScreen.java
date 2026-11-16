package ch.realcinema.client.screen;

import ch.realcinema.menu.ControlPanelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ControlPanelScreen extends CinemaScreen<ControlPanelMenu> {
    private Button play, pause, stop;

    public ControlPanelScreen(ControlPanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        play = menuButton(Component.translatable("gui.realcinema.play"), 18, 28, 46, ControlPanelMenu.BTN_PLAY);
        pause = menuButton(Component.translatable("gui.realcinema.pause"), 66, 28, 46, ControlPanelMenu.BTN_PAUSE);
        stop = menuButton(Component.translatable("gui.realcinema.stop"), 114, 28, 46, ControlPanelMenu.BTN_STOP);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int s = menu.panelState();
        play.active = s != 1;
        pause.active = s == 1;
        stop.active = s != 0;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        text(g, Component.translatable("gui.realcinema.panel_state." + menu.panelState()), 18, 56);
        text(g, Component.translatable("gui.realcinema.panel_projectors", menu.projectorsReady()), 18, 68);
        text(g, Component.translatable("gui.realcinema.panel_speakers", menu.speakers()), 18, 80);
    }
}
