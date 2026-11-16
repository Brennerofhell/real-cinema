package ch.realcinema.test;

import ch.realcinema.RealCinema;
import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.DeviceType;
import ch.realcinema.block.ProjectorBlockEntity;
import ch.realcinema.block.ProjectorState;
import ch.realcinema.film.FilmData;
import ch.realcinema.menu.ControlPanelMenu;
import ch.realcinema.menu.FilmBurnerMenu;
import ch.realcinema.menu.FilmCutterMenu;
import ch.realcinema.menu.ProjectorMenu;
import ch.realcinema.registry.ModDataComponents;
import ch.realcinema.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static ch.realcinema.test.RealCinemaTests.check;
import static ch.realcinema.test.RealCinemaTests.count;
import static ch.realcinema.test.RealCinemaTests.device;
import static ch.realcinema.test.RealCinemaTests.projector;
import static ch.realcinema.test.RealCinemaTests.res;
import static ch.realcinema.test.RealCinemaTests.written;

/**
 * Tests der Oberflaechen-Logik (Menues) mit einem echten Server-Spieler. Die Bildschirme selbst (Zeichnen, Knoepfe, Textfeld)
 * sind Client-Code und werden hier nicht getestet.
 */
@GameTestHolder(RealCinema.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuiTests {
    private static final String T = "empty";

    private GuiTests() {}

    private static ServerPlayer serverPlayer(GameTestHelper h, BlockPos near) {
        ServerPlayer sp = h.makeMockServerPlayerInLevel();
        sp.setPos(h.absolutePos(near).getCenter());
        return sp;
    }

    @GameTest(template = T, timeoutTicks = 300)
    public static void gui_burner_flow(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.FILM_BURNER, new BlockPos(2, 1, 2));
        ServerPlayer sp = serverPlayer(h, new BlockPos(2, 1, 4));
        FilmBurnerMenu menu = new FilmBurnerMenu(1, sp.getInventory(), be);
        var slot = menu.getSlot(0);
        check(h, menu.stillValid(sp), "Menue nicht gueltig in Reichweite");

        check(h, !slot.mayPlace(written("a", 100, 0)), "beschriebene Rolle darf nicht ins Brennerfach");
        check(h, !slot.mayPlace(new ItemStack(Items.STICK)), "Fremdgegenstand darf nicht ins Brennerfach");
        check(h, slot.mayPlace(new ItemStack(ModItems.FILM_ROLL_EMPTY.get())), "leere Rolle muss ins Brennerfach duerfen");

        check(h, !menu.clickMenuButton(sp, FilmBurnerMenu.BTN_BURN) && be.stateIndex() == 0, "Brennen ohne Rolle muss scheitern");
        slot.set(new ItemStack(ModItems.FILM_ROLL_EMPTY.get()));
        check(h, !menu.clickMenuButton(sp, FilmBurnerMenu.BTN_BURN) && be.stateIndex() == 0, "Brennen ohne Referenz muss scheitern");

        // Referenz wird bereinigt: Steuerzeichen raus, auf 200 Zeichen gekuerzt
        menu.setReference("  film\u0001.mp4\n  ");
        check(h, "film.mp4".equals(be.reference()), "Referenz nicht bereinigt: [" + be.reference() + "]");
        menu.setReference("x".repeat(500));
        check(h, be.reference().length() == FilmBurnerMenu.MAX_REFERENCE, "Referenz nicht gekuerzt: " + be.reference().length());
        menu.setReference("urlaub.mp4");

        check(h, menu.clickMenuButton(sp, FilmBurnerMenu.BTN_BURN), "Brennen startet nicht");
        check(h, be.stateIndex() == 1, "nicht im Zustand BRENNT");
        check(h, !slot.mayPickup(sp), "waehrend des Brennens darf die Rolle nicht entnommen werden");
        menu.setReference("anders.mp4");
        check(h, "urlaub.mp4".equals(be.reference()), "Referenz darf waehrend des Brennens nicht geaendert werden");

        h.runAfterDelay(110, () -> {
            check(h, be.stateIndex() == 2, "nicht fertig: " + be.stateIndex());
            check(h, slot.mayPickup(sp), "fertige Rolle muss entnehmbar sein");
            ItemStack out = slot.remove(1);
            slot.onTake(sp, out);
            FilmData d = out.get(ModDataComponents.FILM.get());
            check(h, d != null && d.reference().equals("urlaub.mp4"), "entnommene Rolle ohne Daten: " + d);
            check(h, be.stateIndex() == 0 && be.film().isEmpty(), "Brenner nicht wieder bereit");
            h.succeed();
        });
    }

    @GameTest(template = T, timeoutTicks = 400)
    public static void gui_projector_flow(GameTestHelper h) {
        ProjectorBlockEntity be = projector(h, new BlockPos(2, 1, 2), Direction.NORTH);
        ServerPlayer sp = serverPlayer(h, new BlockPos(2, 1, 4));
        ProjectorMenu menu = new ProjectorMenu(1, sp.getInventory(), be);
        var slot = menu.getSlot(0);

        check(h, !menu.clickMenuButton(sp, ProjectorMenu.BTN_PLAY), "Play ohne Rolle muss scheitern");
        check(h, !slot.mayPlace(new ItemStack(ModItems.FILM_ROLL_EMPTY.get())), "leere Rolle darf nicht in den Projektor");
        check(h, slot.mayPlace(written("film.mp4", 400, 0)), "beschriebene Rolle muss ins Rollenfach duerfen");
        slot.set(written("film.mp4", 400, 0));
        check(h, be.hasFilm() && be.filmStack().is(ModItems.FILM_ROLL_LOADED.get()), "Rolle muss als 'eingelegt' im Projektor liegen");
        check(h, menu.getSlot(0).getItem().is(ModItems.FILM_ROLL_LOADED.get()), "Rollenfach zeigt nicht die eingelegte Rolle");

        check(h, menu.clickMenuButton(sp, ProjectorMenu.BTN_PLAY) && be.getState() == ProjectorState.STARTING, "Play startet nicht");
        check(h, !slot.mayPickup(sp) && !slot.mayPlace(written("x", 100, 0)), "waehrend des Laufs darf das Fach nicht bedient werden");

        h.runAfterDelay(ProjectorBlockEntity.STARTUP_TICKS + 30, () -> {
            check(h, be.getState() == ProjectorState.RUNNING && be.playTicks() > 0, "laeuft nicht / keine Position: " + be.getState() + " " + be.playTicks());
            int pausedAt = be.playTicks();
            check(h, menu.clickMenuButton(sp, ProjectorMenu.BTN_PAUSE), "Pause nicht angenommen");
            h.runAfterDelay(ProjectorBlockEntity.SPINDOWN_TICKS + 10, () -> {
                check(h, be.getState() == ProjectorState.OFF, "nach Pause nicht aus: " + be.getState());
                check(h, be.playTicks() >= pausedAt && be.playTicks() > 0, "Pause darf die Position nicht zuruecksetzen: " + be.playTicks());
                check(h, slot.mayPickup(sp), "bei ausgeschaltetem Projektor muss die Rolle entnehmbar sein");
                menu.clickMenuButton(sp, ProjectorMenu.BTN_STOP); // Stopp = zurueckspulen
                check(h, be.playTicks() == 0, "Stopp spult nicht zurueck: " + be.playTicks());
                ItemStack out = slot.remove(1);
                check(h, out.is(ModItems.FILM_ROLL_WRITTEN.get()) && !be.hasFilm(), "entnommene Rolle muss 'beschrieben' sein");
                h.succeed();
            });
        });
    }

    @GameTest(template = T)
    public static void gui_panel_buttons(GameTestHelper h) {
        DeviceBlockEntity panel = device(h, DeviceType.CONTROL_PANEL, new BlockPos(2, 1, 2));
        DeviceBlockEntity speaker = device(h, DeviceType.SPEAKER_FLOOR, new BlockPos(5, 1, 2));
        ProjectorBlockEntity pr = projector(h, new BlockPos(2, 1, 6), Direction.NORTH);
        ServerPlayer sp = serverPlayer(h, new BlockPos(2, 1, 4));
        ControlPanelMenu menu = new ControlPanelMenu(1, sp.getInventory(), panel);

        check(h, !menu.clickMenuButton(sp, ControlPanelMenu.BTN_PLAY) && panel.stateIndex() == 0, "Play ohne Projektor-Rolle muss scheitern");
        pr.insertFilm(written("film.mp4", 1200, 0));
        check(h, menu.clickMenuButton(sp, ControlPanelMenu.BTN_PLAY), "Play nicht angenommen");
        check(h, panel.stateIndex() == 1 && pr.getState() == ProjectorState.STARTING && speaker.stateIndex() == 1,
                "Play wirkt nicht auf Projektor/Lautsprecher");
        check(h, menu.clickMenuButton(sp, ControlPanelMenu.BTN_PAUSE) && panel.stateIndex() == 2 && pr.getState() == ProjectorState.STOPPING, "Pause wirkt nicht");
        check(h, menu.clickMenuButton(sp, ControlPanelMenu.BTN_STOP) && panel.stateIndex() == 0 && speaker.stateIndex() == 0, "Stopp wirkt nicht");
        check(h, !menu.clickMenuButton(sp, 99), "unbekannter Button muss abgelehnt werden");
        h.succeed();
    }

    @GameTest(template = T)
    public static void gui_cutter_cut_join_and_return(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.FILM_CUTTER, new BlockPos(2, 1, 2));
        ServerPlayer sp = serverPlayer(h, new BlockPos(2, 1, 4));
        FilmCutterMenu menu = new FilmCutterMenu(1, sp.getInventory(), be);

        check(h, !menu.clickMenuButton(sp, FilmCutterMenu.BTN_CUT_50), "Schneiden ohne Rolle muss scheitern");
        check(h, !menu.getSlot(0).mayPlace(new ItemStack(ModItems.FILM_ROLL_EMPTY.get())), "leere Rolle darf nicht ins Schneiderfach");
        menu.getSlot(0).set(written("film.mp4", 1200, 0));
        check(h, menu.clickMenuButton(sp, FilmCutterMenu.BTN_CUT_25), "Schnitt bei 25 % nicht angenommen");
        check(h, menu.getSlot(0).getItem().isEmpty(), "geschnittene Rolle muss aus dem Fach verschwinden");
        FilmData first = null, second = null;
        for (ItemStack st : sp.getInventory().items) {
            FilmData d = st.get(ModDataComponents.FILM.get());
            if (d == null) continue;
            if (d.startTicks() == 0) first = d; else second = d;
        }
        check(h, first != null && second != null && first.lengthTicks() == 300 && second.startTicks() == 300 && second.lengthTicks() == 900,
                "25-%-Schnitt falsch: " + first + " / " + second);

        menu.getSlot(0).set(written("film.mp4", 300, 0));
        menu.getSlot(1).set(written("film.mp4", 900, 300));
        check(h, menu.clickMenuButton(sp, FilmCutterMenu.BTN_JOIN), "Zusammenfuegen nicht angenommen");
        check(h, menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "Faecher nach dem Zusammenfuegen nicht leer");
        boolean joined = false;
        for (ItemStack st : sp.getInventory().items) {
            FilmData d = st.get(ModDataComponents.FILM.get());
            if (d != null && d.lengthTicks() == 1200 && d.startTicks() == 0) joined = true;
        }
        check(h, joined, "zusammengefuegte Rolle fehlt");

        // Unpassende Rollen werden nicht verbunden und gehen beim Schliessen zurueck ins Inventar
        menu.getSlot(0).set(written("a.mp4", 300, 0));
        menu.getSlot(1).set(written("b.mp4", 300, 300));
        check(h, !menu.clickMenuButton(sp, FilmCutterMenu.BTN_JOIN), "fremde Videos duerfen nicht verbunden werden");
        int before = count(sp, new ItemStack(ModItems.FILM_ROLL_WRITTEN.get()));
        menu.removed(sp);
        check(h, count(sp, new ItemStack(ModItems.FILM_ROLL_WRITTEN.get())) == before + 2, "Rollen aus den Faechern gehen beim Schliessen verloren");
        h.succeed();
    }

    @GameTest(template = T)
    public static void gui_shift_click_moves_rolls(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.FILM_BURNER, new BlockPos(2, 1, 2));
        ServerPlayer sp = serverPlayer(h, new BlockPos(2, 1, 4));
        FilmBurnerMenu menu = new FilmBurnerMenu(1, sp.getInventory(), be);
        sp.getInventory().setItem(9, new ItemStack(ModItems.FILM_ROLL_EMPTY.get(), 5));   // erster Inventar-Slot = Menue-Slot 1
        menu.quickMoveStack(sp, 1);
        check(h, be.film().is(ModItems.FILM_ROLL_EMPTY.get()), "Shift-Klick legt die Rolle nicht ins Fach");
        check(h, sp.getInventory().getItem(9).getCount() == 4, "genau eine Rolle muss verbraucht werden: " + sp.getInventory().getItem(9).getCount());
        menu.quickMoveStack(sp, 0);
        check(h, be.film().isEmpty() && count(sp, new ItemStack(ModItems.FILM_ROLL_EMPTY.get())) == 5, "Shift-Klick auf das Fach gibt die Rolle nicht zurueck");
        sp.getInventory().setItem(10, new ItemStack(Items.STICK));
        menu.quickMoveStack(sp, 2);
        check(h, be.film().isEmpty(), "Fremdgegenstand landet im Rollenfach");
        h.succeed();
    }

    @GameTest(template = T)
    public static void gui_menu_closes_when_far_away_or_broken(GameTestHelper h) {
        device(h, DeviceType.CONTROL_PANEL, new BlockPos(2, 1, 2));
        DeviceBlockEntity be = (DeviceBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 2));
        ServerPlayer sp = serverPlayer(h, new BlockPos(2, 1, 4));
        ControlPanelMenu menu = new ControlPanelMenu(1, sp.getInventory(), be);
        check(h, menu.stillValid(sp), "in Reichweite muss das Menue gueltig sein");
        sp.setPos(h.absolutePos(new BlockPos(2, 1, 20)).getCenter());
        check(h, !menu.stillValid(sp), "weit weg muss das Menue ungueltig sein");
        sp.setPos(h.absolutePos(new BlockPos(2, 1, 4)).getCenter());
        h.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
        check(h, !menu.stillValid(sp), "nach dem Abbau muss das Menue ungueltig sein");
        h.succeed();
    }

    @GameTest(template = T)
    public static void gui_translations_exist(GameTestHelper h) {
        for (String lang : new String[]{"en_us", "de_de"}) {
            try (var in = res("lang/" + lang + ".json")) {
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : new String[]{"container.realcinema.film_burner", "container.realcinema.film_cutter", "container.realcinema.control_panel",
                        "container.realcinema.projector", "gui.realcinema.burn", "gui.realcinema.play", "gui.realcinema.pause", "gui.realcinema.stop",
                        "gui.realcinema.join", "gui.realcinema.reference", "gui.realcinema.reference_hint", "gui.realcinema.panel_state.0",
                        "gui.realcinema.projector_state.running", "gui.realcinema.status_done", "gui.realcinema.screen_linked"}) {
                    check(h, json.has(key), lang + ": Uebersetzung fehlt: " + key);
                }
            } catch (Exception e) {
                h.fail(lang + ": " + e);
            }
        }
        h.succeed();
    }
}
