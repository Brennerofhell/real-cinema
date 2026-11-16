package ch.realcinema.client.screen;

import ch.realcinema.film.FilmData;
import ch.realcinema.menu.FilmCutterMenu;
import ch.realcinema.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class FilmCutterScreen extends CinemaScreen<FilmCutterMenu> {
    private Button cut25, cut50, cut75, join;

    public FilmCutterScreen(FilmCutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        cut25 = menuButton(Component.literal("25%"), 54, 30, 34, FilmCutterMenu.BTN_CUT_25);
        cut50 = menuButton(Component.literal("50%"), 90, 30, 34, FilmCutterMenu.BTN_CUT_50);
        cut75 = menuButton(Component.literal("75%"), 126, 30, 34, FilmCutterMenu.BTN_CUT_75);
        join = menuButton(Component.translatable("gui.realcinema.join"), 54, 54, 106, FilmCutterMenu.BTN_JOIN);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        FilmData a = data(menu.slotA()), b = data(menu.slotB());
        boolean cutOk = a != null && a.lengthTicks() >= FilmData.MIN_CUTTABLE_TICKS;
        cut25.active = cut50.active = cut75.active = cutOk;
        join.active = a != null && b != null && a.isContiguousWith(b);
    }

    private static FilmData data(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(ModDataComponents.FILM.get());
    }

    @Override
    protected void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        text(g, "A", 12, 38);
        text(g, "B", 12, 62);
        FilmData a = data(menu.slotA()), b = data(menu.slotB());
        text(g, describe(a), 54, 78);
        text(g, describe(b), 54, 86);
    }

    private static String describe(FilmData d) {
        return d == null ? "-" : d.reference() + "  " + d.range();
    }
}
