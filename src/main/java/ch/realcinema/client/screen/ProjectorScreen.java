package ch.realcinema.client.screen;

import ch.realcinema.block.ProjectorState;
import ch.realcinema.menu.ProjectorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ProjectorScreen extends CinemaScreen<ProjectorMenu> {
    private Button play, pause, stop;

    public ProjectorScreen(ProjectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        play = menuButton(Component.translatable("gui.realcinema.play"), 54, 32, 36, ProjectorMenu.BTN_PLAY);
        pause = menuButton(Component.translatable("gui.realcinema.pause"), 92, 32, 36, ProjectorMenu.BTN_PAUSE);
        stop = menuButton(Component.translatable("gui.realcinema.stop"), 130, 32, 36, ProjectorMenu.BTN_STOP);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ProjectorState s = menu.state();
        boolean running = s == ProjectorState.STARTING || s == ProjectorState.RUNNING;
        play.active = menu.hasFilm() && !running;
        pause.active = running;
        stop.active = menu.hasFilm() && (s != ProjectorState.OFF || menu.playTicks() > 0);
    }

    @Override
    protected void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        bar(g, 54, 60, 112, 8, menu.progress(), 0xFF3FA34D);
        text(g, clock(menu.playTicks()) + " / " + clock(menu.lengthTicks()), 54, 71);
        Component state = Component.translatable("gui.realcinema.projector_state." + menu.state().name().toLowerCase());
        text(g, state, 54, 82);
        Component screen = Component.translatable(menu.screenLinked() ? "gui.realcinema.screen_linked" : "gui.realcinema.screen_unknown");
        text(g, screen, 110, 82);
    }
}
