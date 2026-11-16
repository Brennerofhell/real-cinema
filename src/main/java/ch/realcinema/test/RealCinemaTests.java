package ch.realcinema.test;

import ch.realcinema.RealCinema;
import ch.realcinema.block.DeviceBehaviors;
import ch.realcinema.block.DeviceBlock;
import ch.realcinema.block.DeviceBlockEntity;
import ch.realcinema.block.DeviceType;
import ch.realcinema.block.ProjectorBlock;
import ch.realcinema.block.ProjectorBlockEntity;
import ch.realcinema.block.ProjectorState;
import ch.realcinema.block.ScreenHelper;
import ch.realcinema.film.FilmData;
import ch.realcinema.item.FilmCanisterItem;
import ch.realcinema.registry.ModBlocks;
import ch.realcinema.registry.ModDataComponents;
import ch.realcinema.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Serverseitige Tests der Spiellogik (ohne Fenster). Ausfuehren mit {@code ./gradlew runGameTestServer}.
 * Sie pruefen Zustandsautomaten, Interaktionen, Datenkomponenten, Speichern/Laden und die Leinwandlogik; das Zeichnen von Bild
 * und Ton ist Client-Sache und wird hier nicht getestet.
 */
@GameTestHolder(RealCinema.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RealCinemaTests {
    private static final String T = "empty"; // Namespace kommt von @GameTestHolder: realcinema:empty

    private RealCinemaTests() {}

    // ---- Hilfen -----------------------------------------------------------------------------------------------

    static DeviceBlockEntity device(GameTestHelper h, DeviceType type, BlockPos pos) {
        h.setBlock(pos, ModBlocks.DEVICES.get(type).get().defaultBlockState());
        if (!(h.getBlockEntity(pos) instanceof DeviceBlockEntity be)) {
            h.fail("Keine Block-Entity fuer " + type);
            throw new IllegalStateException();
        }
        return be;
    }

    static ProjectorBlockEntity projector(GameTestHelper h, BlockPos pos, Direction facing) {
        h.setBlock(pos, ModBlocks.PROJECTOR.get().defaultBlockState().setValue(ProjectorBlock.FACING, facing));
        if (!(h.getBlockEntity(pos) instanceof ProjectorBlockEntity be)) {
            h.fail("Keine Projektor-Block-Entity");
            throw new IllegalStateException();
        }
        return be;
    }

    static ItemStack written(String ref, int length, int start) {
        ItemStack s = new ItemStack(ModItems.FILM_ROLL_WRITTEN.get());
        s.set(ModDataComponents.FILM.get(), new FilmData(ref, length, start));
        return s;
    }

    static int count(Player p, ItemStack like) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (ItemStack.isSameItem(s, like)) n += s.getCount();
        return n;
    }

    static void check(GameTestHelper h, boolean ok, String message) {
        if (!ok) h.fail(message);
    }

    // ---- Registrierung ----------------------------------------------------------------------------------------

    @GameTest(template = T)
    public static void registry_complete(GameTestHelper h) {
        for (DeviceType t : DeviceType.values()) {
            check(h, ModBlocks.DEVICES.get(t) != null && ModBlocks.DEVICES.get(t).isBound(), "Block fehlt: " + t);
            check(h, ModItems.DEVICES.get(t) != null && ModItems.DEVICES.get(t).isBound(), "Item fehlt: " + t);
        }
        check(h, ModItems.FILM_ROLL_WRITTEN.isBound() && ModItems.FILM_CANISTER.isBound() && ModItems.CLAPPERBOARD.isBound(), "Items fehlen");
        h.succeed();
    }

    @GameTest(template = T)
    public static void device_state_machines_valid(GameTestHelper h) {
        for (DeviceType t : DeviceType.values()) {
            int n = t.states.length;
            for (int i = 0; i < n; i++) {
                DeviceType.State s = t.states[i];
                check(h, s.click() >= 0 && s.click() < n, t + "/" + s.name() + ": click zeigt ins Leere");
                check(h, s.sneakClick() >= 0 && s.sneakClick() < n, t + "/" + s.name() + ": sneakClick zeigt ins Leere");
                if (s.autoTicks() > 0) check(h, s.autoNext() >= 0 && s.autoNext() < n, t + "/" + s.name() + ": autoNext ungueltig");
                check(h, t.mainAnimation(i) != null, t + "/" + s.name() + ": keine Animation");
            }
        }
        h.succeed();
    }

    // ---- Generischer Zustandswechsel + Auto-Timer -------------------------------------------------------------

    @GameTest(template = T, timeoutTicks = 100)
    public static void speaker_toggles(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.SPEAKER_WALL, new BlockPos(2, 1, 2));
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        check(h, be.stateIndex() == 0, "Start nicht aus");
        be.interact(p, false);
        check(h, be.stateIndex() == 1, "Klick schaltet nicht ein");
        be.interact(p, false);
        check(h, be.stateIndex() == 0, "Klick schaltet nicht aus");
        h.succeed();
    }

    @GameTest(template = T, timeoutTicks = 100)
    public static void cutter_returns_to_idle_by_itself(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.FILM_CUTTER, new BlockPos(2, 1, 2));
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        be.interact(p, false);
        check(h, be.stateIndex() == 1, "Schnittzustand nicht erreicht");
        h.runAfterDelay(30, () -> {
            check(h, be.stateIndex() == 0, "Schneider kehrt nicht selbst zurueck (Zustand " + be.stateIndex() + ")");
            h.succeed();
        });
    }

    // ---- Filmbrenner ------------------------------------------------------------------------------------------

    @GameTest(template = T, timeoutTicks = 200)
    public static void burner_full_flow(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.FILM_BURNER, new BlockPos(2, 1, 2));
        Player p = h.makeMockPlayer(GameType.SURVIVAL);

        be.interact(p, false);
        check(h, be.stateIndex() == 0, "Start ohne Rolle und Referenz muss abgelehnt werden");

        ItemStack empty = new ItemStack(ModItems.FILM_ROLL_EMPTY.get(), 2);
        check(h, DeviceBehaviors.useItem(be, p, InteractionHand.MAIN_HAND, empty), "leere Rolle nicht angenommen");
        check(h, empty.getCount() == 1 && !be.film().isEmpty(), "Rolle nicht eingelegt/verbraucht");

        be.interact(p, false);
        check(h, be.stateIndex() == 0, "Start ohne Referenz muss abgelehnt werden");

        ItemStack named = new ItemStack(net.minecraft.world.item.Items.PAPER);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("urlaub.mp4"));
        check(h, DeviceBehaviors.useItem(be, p, InteractionHand.MAIN_HAND, named), "benannter Gegenstand nicht angenommen");
        check(h, "urlaub.mp4".equals(be.reference()), "Referenz falsch: " + be.reference());

        be.interact(p, false);
        check(h, be.stateIndex() == 1, "Brennen nicht gestartet");
        h.runAfterDelay(110, () -> {
            check(h, be.stateIndex() == 2, "Brennen nicht fertig (Zustand " + be.stateIndex() + ")");
            FilmData d = be.film().get(ModDataComponents.FILM.get());
            check(h, d != null && d.reference().equals("urlaub.mp4") && d.lengthTicks() == FilmData.DEFAULT_LENGTH_TICKS && d.startTicks() == 0,
                    "Filmdaten falsch: " + d);
            be.interact(p, false);
            check(h, be.stateIndex() == 0 && be.film().isEmpty(), "Entnahme fehlgeschlagen");
            ItemStack got = null;
            for (ItemStack s : p.getInventory().items) if (s.has(ModDataComponents.FILM.get())) got = s;
            check(h, got != null && got.is(ModItems.FILM_ROLL_WRITTEN.get()), "Spieler hat keine beschriebene Rolle bekommen");
            h.succeed();
        });
    }

    // ---- Filmschneider ----------------------------------------------------------------------------------------

    @GameTest(template = T)
    public static void cutter_cut_and_join(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.FILM_CUTTER, new BlockPos(2, 1, 2));
        Player p = h.makeMockPlayer(GameType.SURVIVAL);

        ItemStack roll = written("film.mp4", 1200, 0);
        p.setItemInHand(InteractionHand.MAIN_HAND, roll);
        check(h, DeviceBehaviors.useItem(be, p, InteractionHand.MAIN_HAND, roll), "Schnitt nicht verarbeitet");

        FilmData a = null, b = null;
        for (ItemStack s : p.getInventory().items) {
            FilmData d = s.get(ModDataComponents.FILM.get());
            if (d == null) continue;
            if (d.startTicks() == 0) a = d; else b = d;
        }
        check(h, a != null && b != null, "Es sind nicht zwei Rollen entstanden");
        check(h, a.lengthTicks() == 600 && b.lengthTicks() == 600 && b.startTicks() == 600, "Abschnitte falsch: " + a + " / " + b);
        check(h, a.isContiguousWith(b), "Abschnitte muessen aneinanderschliessen");

        // Zusammenfuegen: beide Rollen in die Haende, schleichen
        ItemStack ra = written("film.mp4", 600, 0), rb = written("film.mp4", 600, 600);
        p.setItemInHand(InteractionHand.MAIN_HAND, rb);
        p.setItemInHand(InteractionHand.OFF_HAND, ra);
        p.setShiftKeyDown(true);
        check(h, DeviceBehaviors.useItem(be, p, InteractionHand.MAIN_HAND, rb), "Zusammenfuegen nicht verarbeitet");
        boolean found = false;
        for (ItemStack s : p.getInventory().items) {
            FilmData d = s.get(ModDataComponents.FILM.get());
            if (d != null && d.lengthTicks() == 1200 && d.startTicks() == 0) found = true;
        }
        check(h, found, "zusammengefuegte Rolle (1200 Ticks, Start 0) fehlt");
        h.succeed();
    }

    @GameTest(template = T)
    public static void cutter_rejects_unrelated_join_and_short_film(GameTestHelper h) {
        DeviceBlockEntity be = device(h, DeviceType.FILM_CUTTER, new BlockPos(2, 1, 2));
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack a = written("a.mp4", 600, 0), b = written("b.mp4", 600, 600);
        p.setItemInHand(InteractionHand.MAIN_HAND, a);
        p.setItemInHand(InteractionHand.OFF_HAND, b);
        p.setShiftKeyDown(true);
        DeviceBehaviors.useItem(be, p, InteractionHand.MAIN_HAND, a);
        check(h, !a.isEmpty() && !b.isEmpty(), "andere Videos duerfen nicht zusammengefuegt werden");

        p.setShiftKeyDown(false);
        ItemStack tiny = written("a.mp4", 20, 0);
        p.setItemInHand(InteractionHand.MAIN_HAND, tiny);
        DeviceBehaviors.useItem(be, p, InteractionHand.MAIN_HAND, tiny);
        check(h, !tiny.isEmpty(), "zu kurzer Film darf nicht geschnitten werden");
        h.succeed();
    }

    // ---- Projektor --------------------------------------------------------------------------------------------

    @GameTest(template = T, timeoutTicks = 300)
    public static void projector_needs_film_and_runs(GameTestHelper h) {
        ProjectorBlockEntity be = projector(h, new BlockPos(2, 1, 2), Direction.NORTH);
        check(h, !be.requestStart() && be.getState() == ProjectorState.OFF, "Start ohne Film muss scheitern");

        ItemStack roll = written("film.mp4", 80, 0); // 4 s
        check(h, be.insertFilm(roll), "Einlegen fehlgeschlagen");
        check(h, be.filmData() != null && be.hasFilm(), "Film nicht erkannt");
        check(h, be.requestStart() && be.getState() == ProjectorState.STARTING, "Start mit Film muss STARTING geben");
        check(h, h.getBlockState(new BlockPos(2, 1, 2)).getValue(ProjectorBlock.LIT), "Lampe muss leuchten (LIT)");
        check(h, !be.insertFilm(written("x", 10, 0)), "Einlegen waehrend des Laufs darf nicht gehen");

        h.runAfterDelay(ProjectorBlockEntity.STARTUP_TICKS + 5, () -> {
            check(h, be.getState() == ProjectorState.RUNNING, "nicht RUNNING nach Hochlauf: " + be.getState());
            h.runAfterDelay(100, () -> {
                // Film (80 Ticks) ist zu Ende: Projektor stoppt selbst
                check(h, be.getState() == ProjectorState.STOPPING || be.getState() == ProjectorState.OFF,
                        "Projektor stoppt am Filmende nicht: " + be.getState());
                h.runAfterDelay(ProjectorBlockEntity.SPINDOWN_TICKS + 5, () -> {
                    check(h, be.getState() == ProjectorState.OFF, "nicht OFF nach Auslauf: " + be.getState());
                    check(h, !h.getBlockState(new BlockPos(2, 1, 2)).getValue(ProjectorBlock.LIT), "Lampe muss aus sein");
                    ItemStack out = be.ejectFilm();
                    check(h, out.is(ModItems.FILM_ROLL_WRITTEN.get()), "ausgeworfene Rolle muss 'beschrieben' sein");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = T)
    public static void projector_inserted_roll_is_loaded_variant(GameTestHelper h) {
        ProjectorBlockEntity be = projector(h, new BlockPos(2, 1, 2), Direction.NORTH);
        ItemStack roll = written("film.mp4", 200, 100);
        be.insertFilm(roll);
        CompoundTag tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
        ItemStack inside = ItemStack.parse(h.getLevel().registryAccess(), tag.getCompound("Film")).orElse(ItemStack.EMPTY);
        check(h, inside.is(ModItems.FILM_ROLL_LOADED.get()), "eingelegte Rolle muss die 'eingelegt'-Variante sein");
        FilmData d = inside.get(ModDataComponents.FILM.get());
        check(h, d != null && d.startTicks() == 100 && d.lengthTicks() == 200, "Filmdaten gehen beim Einlegen verloren: " + d);
        check(h, be.ejectFilm().is(ModItems.FILM_ROLL_WRITTEN.get()), "Auswurf muss wieder 'beschrieben' sein");
        h.succeed();
    }

    // ---- Bedienpult / Klappe ----------------------------------------------------------------------------------

    @GameTest(template = T, timeoutTicks = 200)
    public static void panel_controls_projector_and_speakers(GameTestHelper h) {
        DeviceBlockEntity panel = device(h, DeviceType.CONTROL_PANEL, new BlockPos(2, 1, 2));
        DeviceBlockEntity speaker = device(h, DeviceType.SPEAKER_WALL, new BlockPos(4, 1, 2));
        ProjectorBlockEntity pr = projector(h, new BlockPos(2, 1, 5), Direction.NORTH);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);

        panel.interact(p, false);
        check(h, panel.stateIndex() == 0, "Ohne Film im Projektor darf das Pult nicht starten");

        pr.insertFilm(written("film.mp4", 1200, 0));
        panel.interact(p, false);
        check(h, panel.stateIndex() == 1, "Pult nicht auf PLAYING");
        check(h, pr.getState() == ProjectorState.STARTING, "Projektor nicht gestartet: " + pr.getState());
        check(h, speaker.stateIndex() == 1, "Lautsprecher nicht eingeschaltet");

        panel.interact(p, false);
        check(h, panel.stateIndex() == 2, "Pult nicht auf PAUSED");
        check(h, pr.getState() == ProjectorState.STOPPING, "Projektor nicht gestoppt bei Pause: " + pr.getState());
        check(h, speaker.stateIndex() == 1, "Lautsprecher bleibt bei Pause an");

        panel.interact(p, true);
        check(h, panel.stateIndex() == 0, "Schleichen muss stoppen");
        check(h, speaker.stateIndex() == 0, "Lautsprecher bei Stopp nicht aus");
        h.succeed();
    }

    @GameTest(template = T, timeoutTicks = 400)
    public static void panel_resets_when_film_ends(GameTestHelper h) {
        DeviceBlockEntity panel = device(h, DeviceType.CONTROL_PANEL, new BlockPos(2, 1, 2));
        ProjectorBlockEntity pr = projector(h, new BlockPos(2, 1, 5), Direction.NORTH);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        pr.insertFilm(written("kurz.mp4", 60, 0)); // 3 s
        panel.interact(p, false);
        check(h, panel.stateIndex() == 1, "Pult startet nicht");
        h.runAfterDelay(ProjectorBlockEntity.STARTUP_TICKS + 60 + 20, () -> {
            check(h, panel.stateIndex() == 0, "Pult springt am Filmende nicht auf Stopp (Zustand " + panel.stateIndex() + ")");
            h.succeed();
        });
    }

    @GameTest(template = T)
    public static void clapperboard_starts_nearby_panel(GameTestHelper h) {
        DeviceBlockEntity panel = device(h, DeviceType.CONTROL_PANEL, new BlockPos(3, 1, 3));
        ProjectorBlockEntity pr = projector(h, new BlockPos(3, 1, 6), Direction.NORTH);
        pr.insertFilm(written("film.mp4", 1200, 0));
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(h.absolutePos(new BlockPos(3, 1, 4)).getCenter());
        DeviceBehaviors.clap(h.getLevel(), p);
        check(h, panel.stateIndex() == 1 && pr.getState() == ProjectorState.STARTING, "Klappe startet das Pult nicht");
        p.setShiftKeyDown(true);
        DeviceBehaviors.clap(h.getLevel(), p);
        check(h, panel.stateIndex() == 0, "Klappe + Schleichen stoppt nicht");
        h.succeed();
    }

    // ---- Leinwand ---------------------------------------------------------------------------------------------

    private static void screen(GameTestHelper h, BlockPos pos, Direction facing) {
        h.setBlock(pos, ModBlocks.DEVICES.get(DeviceType.CINEMA_SCREEN).get().defaultBlockState().setValue(DeviceBlock.FACING, facing));
    }

    @GameTest(template = T)
    public static void screen_surface_and_master(GameTestHelper h) {
        Direction f = Direction.NORTH;
        // 3 breit, 2 hoch. Aus Sicht des Betrachters (steht im Norden, schaut nach Ssueden) ist "links" = Osten (+x).
        for (int dx = 0; dx < 3; dx++) for (int dy = 0; dy < 2; dy++) screen(h, new BlockPos(2 + dx, 1 + dy, 8), f);
        var level = h.getLevel();
        BlockPos any = h.absolutePos(new BlockPos(3, 2, 8));
        BlockPos master = ScreenHelper.findMaster(level, any, f);
        check(h, master.equals(h.absolutePos(new BlockPos(4, 1, 8))),
                "Master muss unten links (aus Betrachtersicht, also hoechstes x) liegen, ist " + h.relativePos(master));
        ScreenHelper.Surface s = ScreenHelper.surfaceOf(level, master, f);
        check(h, s.width() == 3 && s.height() == 2, "Abmessungen " + s.width() + "x" + s.height() + " statt 3x2");
        check(h, ScreenHelper.isMaster(level, master, f), "isMaster muss true sein");
        check(h, !ScreenHelper.isMaster(level, h.absolutePos(new BlockPos(3, 1, 8)), f), "mittlerer Block ist kein Master");
        check(h, ScreenHelper.connected(level, master, f).size() == 6, "zusammenhaengend muessen 6 Bloecke sein");
        h.succeed();
    }

    @GameTest(template = T)
    public static void screen_click_toggles_all_connected(GameTestHelper h) {
        Direction f = Direction.NORTH;
        for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++) screen(h, new BlockPos(2 + dx, 1 + dy, 8), f);
        screen(h, new BlockPos(10, 1, 8), f); // getrennt, darf nicht mitschalten
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        DeviceBlockEntity one = (DeviceBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 8));
        one.interact(p, false);
        for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++) {
            DeviceBlockEntity be = (DeviceBlockEntity) h.getBlockEntity(new BlockPos(2 + dx, 1 + dy, 8));
            check(h, be.stateIndex() == 1, "Block " + dx + "," + dy + " schaltet nicht mit");
        }
        check(h, ((DeviceBlockEntity) h.getBlockEntity(new BlockPos(10, 1, 8))).stateIndex() == 0, "getrennter Block darf nicht mitschalten");
        h.succeed();
    }

    @GameTest(template = T)
    public static void screen_only_links_facing_projector(GameTestHelper h) {
        // Projektor schaut nach Sueden (+z), Leinwand liegt davor und zeigt nach Norden (zum Projektor).
        BlockPos pr = h.absolutePos(new BlockPos(5, 2, 2));
        screen(h, new BlockPos(5, 2, 12), Direction.NORTH);
        BlockPos found = ScreenHelper.findScreenFor(h.getLevel(), pr, Direction.SOUTH);
        check(h, found != null && found.equals(h.absolutePos(new BlockPos(5, 2, 12))), "Leinwand vor dem Projektor nicht gefunden: " + found);
        // Leinwand mit der Rueckseite zum Projektor wird ignoriert.
        h.setBlock(new BlockPos(5, 2, 12), net.minecraft.world.level.block.Blocks.AIR);
        screen(h, new BlockPos(5, 2, 12), Direction.SOUTH);
        check(h, ScreenHelper.findScreenFor(h.getLevel(), pr, Direction.SOUTH) == null, "falsch herum stehende Leinwand darf nicht gefunden werden");
        h.succeed();
    }

    @GameTest(template = T)
    public static void projector_links_screen_on_start(GameTestHelper h) {
        ProjectorBlockEntity pr = projector(h, new BlockPos(5, 2, 2), Direction.SOUTH);
        screen(h, new BlockPos(5, 2, 12), Direction.NORTH);
        pr.insertFilm(written("film.mp4", 1200, 0));
        check(h, pr.requestStart(), "Start fehlgeschlagen");
        check(h, h.absolutePos(new BlockPos(5, 2, 12)).equals(pr.screenPos()), "Projektor hat die Leinwand nicht zugeordnet: " + pr.screenPos());
        CompoundTag update = pr.getUpdateTag(h.getLevel().registryAccess());
        check(h, update.contains("Screen") && "film.mp4".equals(update.getString("Ref")) && update.getString("State").equals("STARTING"),
                "Update-Paket unvollstaendig: " + update);
        h.succeed();
    }

    // ---- Speichern / Laden / Netzwerk-Tag ---------------------------------------------------------------------

    @GameTest(template = T)
    public static void persistence_roundtrip(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        // Brenner mit Rolle und Referenz
        DeviceBlockEntity burner = device(h, DeviceType.FILM_BURNER, new BlockPos(2, 1, 2));
        burner.setReference("a.mp4");
        burner.setStateIndex(2);                    // DONE schreibt die Rolle neu, deshalb erst danach die Testrolle setzen
        burner.setFilm(written("a.mp4", 300, 40));
        CompoundTag tag = burner.saveWithoutMetadata(access);
        DeviceBlockEntity copy = device(h, DeviceType.FILM_BURNER, new BlockPos(4, 1, 2));
        copy.loadWithComponents(tag, access);
        check(h, copy.stateIndex() == 2 && "a.mp4".equals(copy.reference()), "Zustand/Referenz nach dem Laden falsch");
        FilmData d = copy.film().get(ModDataComponents.FILM.get());
        check(h, d != null && d.lengthTicks() == 300 && d.startTicks() == 40, "Film nach dem Laden falsch: " + d);

        // Projektor mitten im Hochlauf: Zustand muss unveraendert bleiben (Client sieht ihn)
        ProjectorBlockEntity pr = projector(h, new BlockPos(2, 1, 6), Direction.NORTH);
        pr.insertFilm(written("b.mp4", 100, 0));
        pr.requestStart();
        CompoundTag ptag = pr.saveWithoutMetadata(access);
        ProjectorBlockEntity pcopy = projector(h, new BlockPos(5, 1, 6), Direction.NORTH);
        pcopy.loadWithComponents(ptag, access);
        check(h, pcopy.getState() == ProjectorState.STARTING, "Zustand STARTING geht beim Laden verloren: " + pcopy.getState());
        check(h, pcopy.hasFilm() && pcopy.clientReference().equals("b.mp4"), "Film/Referenz beim Laden verloren");
        h.succeed();
    }

    // ---- Items ------------------------------------------------------------------------------------------------

    @GameTest(template = T)
    public static void canister_stores_and_returns_rolls(GameTestHelper h) {
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack canister = new ItemStack(ModItems.FILM_CANISTER.get());
        ItemStack r1 = written("a", 100, 0), r2 = new ItemStack(ModItems.FILM_ROLL_EMPTY.get(), 3);
        check(h, FilmCanisterItem.tryInsert(canister, r1, p), "beschriebene Rolle nicht aufgenommen");
        check(h, FilmCanisterItem.tryInsert(canister, r2, p) && r2.getCount() == 2, "leere Rolle nicht aufgenommen/verbraucht");
        check(h, FilmCanisterItem.contents(canister).size() == 2, "Inhalt nicht 2");
        check(h, !FilmCanisterItem.tryInsert(canister, new ItemStack(net.minecraft.world.item.Items.STICK), p), "Fremdgegenstand darf nicht rein");
        ItemStack out = FilmCanisterItem.takeLast(canister, p);
        check(h, out.is(ModItems.FILM_ROLL_EMPTY.get()), "letzte Rolle (leer) muss zuerst raus");
        ItemStack out2 = FilmCanisterItem.takeLast(canister, p);
        FilmData d = out2.get(ModDataComponents.FILM.get());
        check(h, d != null && d.reference().equals("a"), "Filmdaten gehen in der Dose verloren");
        check(h, FilmCanisterItem.takeLast(canister, p).isEmpty(), "leere Dose gibt etwas her");
        for (int i = 0; i < FilmCanisterItem.CAPACITY; i++) FilmCanisterItem.tryInsert(canister, new ItemStack(ModItems.FILM_ROLL_EMPTY.get()), p);
        check(h, !FilmCanisterItem.tryInsert(canister, new ItemStack(ModItems.FILM_ROLL_EMPTY.get()), p), "Kapazitaet wird nicht begrenzt");
        h.succeed();
    }

    @GameTest(template = T)
    public static void film_data_math(GameTestHelper h) {
        FilmData a = new FilmData("x", 600, 0), b = new FilmData("x", 600, 600), c = new FilmData("y", 600, 600);
        check(h, a.isContiguousWith(b) && b.isContiguousWith(a), "aneinanderschliessend wird nicht erkannt");
        check(h, !a.isContiguousWith(c), "andere Referenz darf nicht zusammenpassen");
        check(h, a.join(b).equals(new FilmData("x", 1200, 0)) && b.join(a).equals(new FilmData("x", 1200, 0)), "join falsch");
        check(h, a.slice(100, 200).equals(new FilmData("x", 200, 100)), "slice falsch");
        check(h, new FilmData("x", 1300, 0).range().equals("0:00-1:05"), "range falsch: " + new FilmData("x", 1300, 0).range());
        h.succeed();
    }
    // ---- Echter Block-Pfad (Rechtsklick auf den Block) ----------------------------------------------------------

    @GameTest(template = T)
    public static void projector_right_click_inserts_starts_and_ejects(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        ProjectorBlockEntity be = projector(h, pos, Direction.NORTH);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);

        h.useBlock(pos, p); // leere Hand, kein Film
        check(h, be.getState() == ProjectorState.OFF, "ohne Film darf Rechtsklick nicht starten");

        p.setItemInHand(InteractionHand.MAIN_HAND, written("film.mp4", 200, 0));
        h.useBlock(pos, p);
        check(h, be.hasFilm(), "Rechtsklick mit Rolle legt nicht ein");
        check(h, p.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Rolle wurde nicht verbraucht");

        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.useBlock(pos, p);
        check(h, be.getState() == ProjectorState.STARTING, "Rechtsklick mit leerer Hand startet nicht");
        h.useBlock(pos, p);
        check(h, be.getState() == ProjectorState.STOPPING, "zweiter Rechtsklick stoppt nicht");

        p.setShiftKeyDown(true);
        h.useBlock(pos, p);
        check(h, be.hasFilm(), "Auswurf waehrend Auslauf darf nicht gehen");
        h.succeed();
    }

    @GameTest(template = T)
    public static void breaking_device_drops_film(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 1, 2);
        projector(h, pos, Direction.NORTH).insertFilm(written("film.mp4", 200, 0));
        DeviceBlockEntity burner = device(h, DeviceType.FILM_BURNER, new BlockPos(5, 1, 2));
        burner.setFilm(new ItemStack(ModItems.FILM_ROLL_EMPTY.get()));
        h.destroyBlock(pos);
        h.destroyBlock(new BlockPos(5, 1, 2));
        h.assertItemEntityPresent(ModItems.FILM_ROLL_WRITTEN.get(), pos, 3.0);
        h.assertItemEntityPresent(ModItems.FILM_ROLL_EMPTY.get(), new BlockPos(5, 1, 2), 3.0);
        h.succeed();
    }

    @GameTest(template = T)
    public static void devices_ignore_unrelated_items(GameTestHelper h) {
        ItemStack stick = new ItemStack(net.minecraft.world.item.Items.STICK);
        ItemStack block = new ItemStack(net.minecraft.world.item.Items.STONE);
        for (DeviceType t : DeviceType.values()) {
            check(h, !DeviceBehaviors.handles(t, stick) && !DeviceBehaviors.handles(t, block),
                    t + " beansprucht Gegenstaende, die es nicht braucht (verhindert Blockplatzieren)");
        }
        check(h, DeviceBehaviors.handles(DeviceType.FILM_BURNER, new ItemStack(ModItems.FILM_ROLL_EMPTY.get())), "Brenner nimmt leere Rolle nicht an");
        check(h, DeviceBehaviors.handles(DeviceType.FILM_CUTTER, written("a", 100, 0)), "Schneider nimmt beschriebene Rolle nicht an");
        h.succeed();
    }

    @GameTest(template = T)
    public static void device_hitboxes_rotate_with_facing(GameTestHelper h) {
        for (DeviceType t : DeviceType.values()) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                check(h, t.shape(d) != null && !t.shape(d).isEmpty(), t + " hat keine Hitbox fuer " + d);
            }
        }
        // Die Leinwand ist flach (8 tief, 16 breit): gedreht muss sie 16 tief und 8 breit sein.
        var north = DeviceType.CINEMA_SCREEN.shape(Direction.NORTH).bounds();
        var east = DeviceType.CINEMA_SCREEN.shape(Direction.EAST).bounds();
        check(h, Math.abs(north.getXsize() - east.getZsize()) < 1e-6 && Math.abs(north.getZsize() - east.getXsize()) < 1e-6,
                "Hitbox dreht nicht mit der Ausrichtung");
        h.succeed();
    }
    // ---- Ressourcen (haetten den Absturz im Kreativ-Inventar verhindert) ---------------------------------------

    static java.io.InputStream res(String path) {
        return RealCinemaTests.class.getResourceAsStream("/assets/" + RealCinema.MOD_ID + "/" + path);
    }

    @GameTest(template = T)
    public static void device_assets_are_consistent(GameTestHelper h) {
        for (DeviceType t : DeviceType.values()) {
            String id = t.id;
            check(h, res("geo/block/" + id + ".geo.json") != null, id + ": Geo-Modell fehlt");
            check(h, res("textures/block/" + id + ".png") != null, id + ": Textur fehlt");
            try (var in = res("animations/block/" + id + ".animation.json")) {
                check(h, in != null, id + ": Animationsdatei fehlt");
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var anims = json.getAsJsonObject("animations");
                for (int i = 0; i < t.states.length; i++) {
                    String main = "animation." + id + "." + t.states[i].anim();
                    check(h, anims.has(main), id + ": Animation fehlt in der Datei: " + main);
                    if (t.states[i].aux() != null) {
                        String aux = "animation." + id + "." + t.states[i].aux();
                        check(h, anims.has(aux), id + ": Zusatzanimation fehlt in der Datei: " + aux);
                    }
                }
            } catch (java.io.IOException e) {
                h.fail(id + ": Animationsdatei nicht lesbar: " + e);
            }
            // Glowmaske: genau dann vorhanden, wenn glow() gilt, und dann mit mindestens einem sichtbaren Pixel.
            boolean hasMask = res("textures/block/" + id + "_glowmask.png") != null;
            check(h, hasMask == t.glow(), id + ": glow()=" + t.glow() + ", Glowmaske vorhanden=" + hasMask);
            if (hasMask) {
                try (var in = res("textures/block/" + id + "_glowmask.png")) {
                    var img = javax.imageio.ImageIO.read(in);
                    boolean visible = false;
                    for (int y = 0; y < img.getHeight() && !visible; y++)
                        for (int x = 0; x < img.getWidth(); x++)
                            if ((img.getRGB(x, y) >>> 24) != 0) { visible = true; break; }
                    check(h, visible, id + ": Glowmaske ist komplett transparent (GeckoLib stuerzt damit ab)");
                } catch (java.io.IOException e) {
                    h.fail(id + ": Glowmaske nicht lesbar: " + e);
                }
            }
        }
        // Projektor und Item-Modelle
        for (String name : new String[]{"film_roll", "film_canister", "clapperboard"}) {
            check(h, res("geo/item/" + name + ".geo.json") != null && res("animations/item/" + name + ".animation.json") != null, name + ": Item-Dateien fehlen");
        }
        for (String tex : new String[]{"film_roll_empty", "film_roll_written", "film_roll_loaded", "film_canister", "clapperboard"}) {
            check(h, res("textures/item/" + tex + ".png") != null, "Item-Textur fehlt: " + tex);
        }
        for (String name : new String[]{"projector_glowmask"}) {
            check(h, res("textures/block/" + name + ".png") != null, name + " fehlt");
        }
        h.succeed();
    }
    // ---- Bild- und Tondekodierung mit einer echten Datei -------------------------------------------------------

    private static java.nio.file.Path sample() throws java.io.IOException {
        try (var in = RealCinemaTests.class.getResourceAsStream("/data/" + RealCinema.MOD_ID + "/testdata/sample.mp4")) {
            if (in == null) throw new java.io.IOException("Testdatei fehlt: testdata/sample.mp4");
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("realcinema-sample", ".mp4");
            java.nio.file.Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();
            return tmp;
        }
    }

    private static double freq(byte[] pcm, int frames, int ch, int channels, int rate) {
        int crossings = 0, prev = 0;
        for (int i = 0; i < frames; i++) {
            int idx = (i * channels + ch) * 2;
            int v = (short) ((pcm[idx] & 0xFF) | (pcm[idx + 1] << 8));
            if (i > 0 && ((prev < 0) != (v < 0))) crossings++;
            prev = v;
        }
        return crossings / 2.0 / (frames / (double) rate);
    }

    @GameTest(template = T)
    public static void media_video_decodes(GameTestHelper h) {
        try (var dec = new ch.realcinema.video.VideoDecoder(sample())) {
            check(h, Math.abs(dec.fps() - 25.0) < 0.01, "fps " + dec.fps());
            // gemeldet wird die auf 16 Pixel aufgerundete Kodiergroesse (180 -> 192); das Bild selbst wird auf 320x180 zugeschnitten
            check(h, dec.width() == 320 && dec.height() >= 180 && dec.height() <= 192, "Groesse " + dec.width() + "x" + dec.height());
            int[] out = new int[640 * 360];
            int n = 0;
            while (dec.next(out, 640, 360)) n++;
            check(h, n == 25, "Bilder: " + n + " statt 25");
            // 320x180 ist 16:9 wie die Zielflaeche: Es darf KEIN schwarzer Rand und kein Kodier-Padding im Bild sein.
            int edgeL = out[180 * 640 + 2], edgeR = out[180 * 640 + 637], bottom = out[359 * 640 + 150], top = out[0 * 640 + 150];
            check(h, ((edgeL >> 16) & 0xFF) > 150 && ((edgeR >> 8) & 0xFF) > 150, "schwarzer Rand links/rechts (Padding nicht abgeschnitten?)");
            check(h, ((bottom >> 16) & 0xFF) > 150 && ((top >> 16) & 0xFF) > 150, "Rand oben/unten (Padding nicht abgeschnitten?)");
            int left = out[180 * 640 + 150], right = out[180 * 640 + 500];
            check(h, ((left >> 16) & 0xFF) > 150 && ((left >> 8) & 0xFF) < 80, "linke Haelfte nicht rot: " + Integer.toHexString(left));
            check(h, ((right >> 8) & 0xFF) > 150 && ((right >> 16) & 0xFF) < 80, "rechte Haelfte nicht gruen: " + Integer.toHexString(right));
            dec.seek(0.5);
            check(h, dec.next(out, 640, 360) && Math.abs(dec.position() - 0.52) < 0.05, "Seek auf 0,5 s ungenau: " + dec.position());
        } catch (Exception e) {
            h.fail("Videodekodierung: " + e);
        }
        h.succeed();
    }

    /** Regressionstest: ffmpegs Standard-AAC (mit PNS) muss Ton ergeben (JAAD lieferte Stille). */
    @GameTest(template = T)
    public static void media_aac_with_pns_decodes_correctly(GameTestHelper h) {
        try (var r = ch.realcinema.video.AudioTrackReader.open(sample())) {
            check(h, r != null, "keine AAC-Tonspur gefunden");
            long frames = 0;
            int chunks = 0, rate = 0, ch = 0;
            ch.realcinema.video.AudioTrackReader.Chunk mid = null, c;
            double sumSq = 0;
            long samples = 0;
            while ((c = r.read(4096)) != null) {
                frames += c.frames();
                rate = c.sampleRate();
                ch = c.channels();
                if (++chunks == 3) mid = c;
                for (int i = 0; i < c.frames(); i++) {
                    int idx = i * ch * 2;
                    double v = ((short) ((c.pcm()[idx] & 0xFF) | (c.pcm()[idx + 1] << 8))) / 32768.0;
                    sumSq += v * v;
                    samples++;
                }
            }
            check(h, rate == 44100 && ch == 2, "Format " + rate + " Hz, " + ch + " Kanaele");
            check(h, Math.abs(frames / (double) rate - 1.0) < 0.1, "Dauer " + frames / (double) rate + " s statt ~1 s");
            check(h, r.failedPackets() == 0, "defekte Pakete: " + r.failedPackets());
            check(h, !r.suspectUnsupportedFeatures(), "Erkennung 'vermutlich still' schlaegt an");
            double rms = Math.sqrt(sumSq / samples);
            check(h, Math.abs(rms - 0.354) < 0.05, "Pegel (RMS) " + rms + " statt ~0,354: Ton stumm oder verzerrt");
            double fl = freq(mid.pcm(), mid.frames(), 0, ch, rate), fr = freq(mid.pcm(), mid.frames(), 1, ch, rate);
            check(h, Math.abs(fl - 440) < 25, "linker Kanal " + fl + " Hz statt 440");
            check(h, Math.abs(fr - 880) < 40, "rechter Kanal " + fr + " Hz statt 880");
        } catch (Exception e) {
            h.fail("Tondekodierung: " + e);
        }
        h.succeed();
    }
}
