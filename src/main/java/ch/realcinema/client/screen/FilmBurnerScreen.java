package ch.realcinema.client.screen;

import ch.realcinema.menu.FilmBurnerMenu;
import ch.realcinema.network.SetReferencePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class FilmBurnerScreen extends CinemaScreen<FilmBurnerMenu> {
    private EditBox reference;
    private Button burn;

    public FilmBurnerScreen(FilmBurnerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        reference = new EditBox(font, leftPos + 54, topPos + 34, 112, 16, Component.translatable("gui.realcinema.reference"));
        reference.setMaxLength(FilmBurnerMenu.MAX_REFERENCE);
        reference.setHint(Component.translatable("gui.realcinema.reference_hint"));
        reference.setValue(menu.initialReference);
        reference.setResponder(text -> PacketDistributor.sendToServer(new SetReferencePayload(menu.containerId, text)));
        addRenderableWidget(reference);
        burn = menuButton(Component.translatable("gui.realcinema.burn"), 54, 56, 70, FilmBurnerMenu.BTN_BURN);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        burn.active = menu.state() == FilmBurnerMenu.STATE_IDLE && menu.getSlot(0).hasItem();
        reference.setEditable(menu.state() != FilmBurnerMenu.STATE_BURNING);
    }

    @Override
    protected void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        text(g, Component.translatable("gui.realcinema.reference"), 54, 22);
        bar(g, 54, 82, 112, 6, menu.progress(), 0xFFE08A1E);
        Component status = switch (menu.state()) {
            case FilmBurnerMenu.STATE_BURNING -> Component.translatable("gui.realcinema.status_burning");
            case FilmBurnerMenu.STATE_DONE -> Component.translatable("gui.realcinema.status_done");
            default -> menu.getSlot(0).hasItem() ? Component.translatable("gui.realcinema.status_ready")
                    : Component.translatable("gui.realcinema.status_need_roll");
        };
        text(g, status, 128, 62);
    }

    // Beim Tippen darf "E" das Inventar nicht schliessen (wie beim Amboss).
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return reference.keyPressed(keyCode, scanCode, modifiers) || reference.canConsumeInput()
                || super.keyPressed(keyCode, scanCode, modifiers);
    }
}
