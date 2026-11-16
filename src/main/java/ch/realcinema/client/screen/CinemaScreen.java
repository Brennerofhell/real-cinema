package ch.realcinema.client.screen;

import ch.realcinema.menu.CinemaMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Gemeinsame Basis der Geraete-Oberflaechen. Zeichnet Hintergrund und Slotfelder selbst (ohne Texturdatei, im Stil der
 * Vanilla-Oberflaechen), damit es nichts zu pflegen gibt und alles zur Menue-Slotliste passt.
 */
public abstract class CinemaScreen<T extends CinemaMenu> extends AbstractContainerScreen<T> {
    protected static final int TEXT = 0xFF404040;

    protected CinemaScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.titleLabelY = 6;
        this.inventoryLabelY = 92;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) slotBox(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        drawContent(g, mouseX, mouseY);
    }

    /** Geraetespezifische Anzeigen (Balken, Texte). Koordinaten relativ zu leftPos/topPos selbst addieren. */
    protected abstract void drawContent(GuiGraphics g, int mouseX, int mouseY);

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    protected void text(GuiGraphics g, Component c, int x, int y) {
        g.drawString(font, c, leftPos + x, topPos + y, TEXT, false);
    }

    protected void text(GuiGraphics g, String s, int x, int y) {
        g.drawString(font, s, leftPos + x, topPos + y, TEXT, false);
    }

    /** Knopf, der beim Klick den Menue-Button {@code id} an den Server schickt. */
    protected Button menuButton(Component label, int x, int y, int w, int id) {
        Button b = Button.builder(label, btn -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + x, topPos + y, w, 20).build();
        return addRenderableWidget(b);
    }

    protected void bar(GuiGraphics g, int x, int y, int w, int h, float progress, int color) {
        int x0 = leftPos + x, y0 = topPos + y;
        g.fill(x0 - 1, y0 - 1, x0 + w + 1, y0 + h + 1, 0xFF373737);
        g.fill(x0, y0, x0 + w, y0 + h, 0xFF8B8B8B);
        int filled = Math.round(w * Math.max(0f, Math.min(1f, progress)));
        if (filled > 0) g.fill(x0, y0, x0 + filled, y0 + h, color);
    }

    protected static String clock(int ticks) {
        int s = Math.max(0, ticks / 20);
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    // ---- Vanilla-aehnliche Flaechen --------------------------------------------------------------------------

    protected static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);                             // Rand
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);             // Flaeche
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);                 // Licht oben
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);                 // Licht links
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);         // Schatten unten
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);         // Schatten rechts
    }

    protected static void slotBox(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);                           // dunkler Rand oben/links
        g.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);                   // heller Rand unten/rechts
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);                   // Innenflaeche
    }
}
