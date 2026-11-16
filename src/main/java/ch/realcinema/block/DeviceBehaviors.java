package ch.realcinema.block;

import ch.realcinema.film.FilmData;
import ch.realcinema.film.FilmItems;
import ch.realcinema.registry.ModDataComponents;
import ch.realcinema.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Spiellogik der Geraete, getrennt von Zustandsautomat ({@link DeviceType}) und Block-Entity.
 * Alles laeuft auf dem Server; der Client sieht nur den resultierenden Zustandsindex.
 *
 * <ul>
 *   <li>Filmbrenner: leere Rolle einlegen, Referenz per benanntem Gegenstand setzen, brennen, beschriebene Rolle entnehmen.</li>
 *   <li>Filmschneider: beschriebene Rolle in der Hand wird in zwei Rollen halber Laenge geschnitten.</li>
 *   <li>Bedienpult: steuert Projektoren (nur mit Film) und Lautsprecher im Umkreis von {@link #PANEL_RADIUS} Bloecken.</li>
 * </ul>
 */
public final class DeviceBehaviors {
    public static final int PANEL_RADIUS = 8;

    private DeviceBehaviors() {}

    private static void msg(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.realcinema." + key), true);
    }

    private static void msg(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.realcinema." + key, args), true);
    }

    private static void sound(Level level, BlockPos pos, net.minecraft.sounds.SoundEvent ev) {
        level.playSound(null, pos, ev, SoundSource.BLOCKS, 0.7f, 1.0f);
    }

    /** Nimmt das Geraet diesen Gegenstand an? (Client und Server nutzen dasselbe Praedikat.) */
    public static boolean handles(DeviceType type, ItemStack stack) {
        return switch (type) {
            case FILM_BURNER -> stack.is(ModItems.FILM_ROLL_EMPTY.get()) || stack.has(DataComponents.CUSTOM_NAME);
            case FILM_CUTTER -> stack.has(ModDataComponents.FILM.get());
            default -> false;
        };
    }

    // ---- Gegenstand auf Geraet benutzen ---------------------------------------------------------------------------

    /** @return true, wenn der Gegenstand verarbeitet wurde (dann keine weitere Standard-Interaktion). */
    public static boolean useItem(DeviceBlockEntity be, Player player, InteractionHand hand, ItemStack stack) {
        switch (be.type()) {
            case FILM_BURNER:
                if (stack.is(ModItems.FILM_ROLL_EMPTY.get())) {
                    if (be.stateIndex() != 0 || !be.film().isEmpty()) { msg(player, "burner_busy"); return true; }
                    be.setFilm(stack.copyWithCount(1));
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    sound(be.getLevel(), be.getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM);
                    msg(player, "roll_inserted");
                    return true;
                }
                if (stack.has(DataComponents.CUSTOM_NAME)) {
                    // Die Referenz ist der Name des Gegenstands (z. B. Papier im Amboss umbenannt).
                    String ref = stack.getHoverName().getString();
                    be.setReference(ref);
                    msg(player, "reference_set", ref);
                    return true;
                }
                return false;
            case FILM_CUTTER:
                return cutterUse(be, player, hand, stack);
            default:
                return false;
        }
    }

    /**
     * Filmschneider mit Rolle in der Hand:
     * - normal: schneidet in der Mitte in zwei Rollen (Abschnitte des Videos, siehe {@link FilmData#slice}),
     * - Schleichen mit einer zweiten Rolle in der anderen Hand: fuegt zwei aneinanderschliessende Abschnitte zusammen.
     */
    private static boolean cutterUse(DeviceBlockEntity be, Player player, InteractionHand hand, ItemStack stack) {
        FilmData data = stack.get(ModDataComponents.FILM.get());
        if (data == null) return false;
        if (player.isShiftKeyDown()) {
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!other.has(ModDataComponents.FILM.get())) { msg(player, "join_need_two"); return true; }
            ItemStack joined = FilmItems.join(stack, other);
            if (joined == null) { msg(player, "cannot_join"); return true; }
            if (!player.getAbilities().instabuild) { stack.shrink(1); other.shrink(1); }
            int seconds = joined.get(ModDataComponents.FILM.get()).seconds(); // vor dem Einsortieren lesen: danach ist der Stapel leer
            player.getInventory().placeItemBackInInventory(joined);
            be.setStateIndex(1);
            sound(be.getLevel(), be.getBlockPos(), SoundEvents.SHEEP_SHEAR);
            msg(player, "film_joined", seconds);
            return true;
        }
        ItemStack[] parts = FilmItems.cut(stack, 0.5);
        if (parts == null) { msg(player, "too_short"); return true; }
        int first = parts[0].get(ModDataComponents.FILM.get()).seconds();
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getInventory().placeItemBackInInventory(parts[0]);
        player.getInventory().placeItemBackInInventory(parts[1]);
        be.setStateIndex(1); // CUT
        sound(be.getLevel(), be.getBlockPos(), SoundEvents.SHEEP_SHEAR);
        msg(player, "film_cut", first);
        return true;
    }

    /** Schneider-Animation und Ton, wenn ueber die GUI geschnitten/zusammengefuegt wurde. */
    public static void cutterEffects(DeviceBlockEntity be) {
        be.setStateIndex(1);
        if (be.getLevel() != null) sound(be.getLevel(), be.getBlockPos(), SoundEvents.SHEEP_SHEAR);
    }

    /** Filmklappe: bedient das naechste Bedienpult im Umkreis (Schleichen = Stopp). */
    public static void clap(Level level, Player player) {
        BlockPos center = player.blockPosition();
        DeviceBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-PANEL_RADIUS, -PANEL_RADIUS, -PANEL_RADIUS),
                center.offset(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS))) {
            if (level.getBlockEntity(p) instanceof DeviceBlockEntity dev && dev.type() == DeviceType.CONTROL_PANEL) {
                double d = p.distSqr(center);
                if (d < bestDist) { bestDist = d; best = dev; }
            }
        }
        if (best == null) { msg(player, "no_panel_near"); return; }
        click(best, player, player.isShiftKeyDown());
    }

    // ---- Leere Hand ------------------------------------------------------------------------------------------------

    /** @return true, wenn die Interaktion hier vollstaendig behandelt wurde. */
    public static boolean click(DeviceBlockEntity be, Player player, boolean sneaking) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        switch (be.type()) {
            case FILM_BURNER -> {
                int s = be.stateIndex();
                if (s == 1) { msg(player, "burner_busy"); return true; }
                if (s == 2) { // DONE: beschriebene Rolle entnehmen
                    player.getInventory().placeItemBackInInventory(be.takeFilm());
                    be.setStateIndex(0);
                    msg(player, "roll_taken");
                    return true;
                }
                if (sneaking) { // IDLE: leere Rolle zurueck
                    if (!be.film().isEmpty()) player.getInventory().placeItemBackInInventory(be.takeFilm());
                    return true;
                }
                if (be.film().isEmpty()) { msg(player, "need_roll"); return true; }
                if (be.reference().isBlank()) { msg(player, "need_reference"); return true; }
                be.setStateIndex(1); // BURNING
                sound(level, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE);
                return true;
            }
            case CINEMA_SCREEN -> {
                int target = sneaking ? be.type().state(be.stateIndex()).sneakClick() : be.type().state(be.stateIndex()).click();
                Direction facing = be.getBlockState().getValue(DeviceBlock.FACING);
                for (BlockPos p : ScreenHelper.connected(level, pos, facing)) {
                    if (level.getBlockEntity(p) instanceof DeviceBlockEntity part) part.setStateIndex(target);
                }
                sound(level, pos, SoundEvents.WOOL_PLACE);
                return true;
            }
            case CONTROL_PANEL -> {
                int target = sneaking ? 0 : be.type().state(be.stateIndex()).click();
                if (target == 1 && countReadyProjectors(level, pos) == 0) { msg(player, "no_projector_ready"); return true; }
                be.setStateIndex(target);
                sound(level, pos, SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ---- Zustandswechsel (Server) ----------------------------------------------------------------------------------

    public static void entered(DeviceBlockEntity be, int newState) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide) return;
        switch (be.type()) {
            case FILM_BURNER -> {
                if (newState == 2 && !be.film().isEmpty()) { // DONE: Rolle wird beschriebene Rolle
                    ItemStack written = new ItemStack(ModItems.FILM_ROLL_WRITTEN.get());
                    written.set(ModDataComponents.FILM.get(), new FilmData(be.reference(), FilmData.DEFAULT_LENGTH_TICKS));
                    be.setFilm(written);
                    sound(level, be.getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP);
                }
            }
            case CONTROL_PANEL -> applyPanel(level, be.getBlockPos(), newState);
            default -> {}
        }
    }

    // ---- Bedienpult -------------------------------------------------------------------------------------------------

    private static int countReadyProjectors(Level level, BlockPos center) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-PANEL_RADIUS, -PANEL_RADIUS, -PANEL_RADIUS),
                center.offset(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS))) {
            if (level.getBlockEntity(p) instanceof ProjectorBlockEntity pr && pr.hasFilm()) n++;
        }
        return n;
    }

    /** 0 = gestoppt, 1 = spielt, 2 = pausiert. */
    private static void applyPanel(Level level, BlockPos center, int panelState) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-PANEL_RADIUS, -PANEL_RADIUS, -PANEL_RADIUS),
                center.offset(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS))) {
            BlockEntity other = level.getBlockEntity(p);
            if (other instanceof ProjectorBlockEntity projector) {
                if (panelState == 1) projector.requestStart(); else projector.requestStop();
            } else if (other instanceof DeviceBlockEntity dev
                    && (dev.type() == DeviceType.SPEAKER_WALL || dev.type() == DeviceType.SPEAKER_FLOOR)) {
                dev.setStateIndex(panelState == 0 ? 0 : 1);
            }
        }
    }

    // ---- GUIs -------------------------------------------------------------------------------------------------

    /** Oeffnet die Oberflaeche eines Geraets (Filmbrenner, Filmschneider, Bedienpult). */
    public static void openGui(DeviceBlockEntity be, net.minecraft.server.level.ServerPlayer player) {
        BlockPos pos = be.getBlockPos();
        net.minecraft.world.MenuProvider provider;
        switch (be.type()) {
            case FILM_BURNER -> {
                provider = new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new ch.realcinema.menu.FilmBurnerMenu(id, inv, be),
                        Component.translatable("container.realcinema.film_burner"));
                player.openMenu(provider, buf -> { buf.writeBlockPos(pos); buf.writeUtf(be.reference(), ch.realcinema.menu.FilmBurnerMenu.MAX_REFERENCE); });
            }
            case FILM_CUTTER -> {
                provider = new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new ch.realcinema.menu.FilmCutterMenu(id, inv, be),
                        Component.translatable("container.realcinema.film_cutter"));
                player.openMenu(provider, buf -> buf.writeBlockPos(pos));
            }
            case CONTROL_PANEL -> {
                provider = new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new ch.realcinema.menu.ControlPanelMenu(id, inv, be),
                        Component.translatable("container.realcinema.control_panel"));
                player.openMenu(provider, buf -> buf.writeBlockPos(pos));
            }
            default -> {}
        }
    }

    /** Oeffnet die Projektor-Oberflaeche. */
    public static void openProjectorGui(ProjectorBlockEntity be, net.minecraft.server.level.ServerPlayer player) {
        BlockPos pos = be.getBlockPos();
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new ch.realcinema.menu.ProjectorMenu(id, inv, be),
                Component.translatable("container.realcinema.projector")), buf -> buf.writeBlockPos(pos));
    }

    // ---- Bedienpult-Befehle (GUI) -------------------------------------------------------------------------------

    /** Zustand des Pultes setzen: 0 = Stopp, 1 = Wiedergabe, 2 = Pause. Wiedergabe nur mit bereitem Projektor. @return false, wenn abgelehnt */
    public static boolean panelCommand(DeviceBlockEntity panel, Player player, int target) {
        Level level = panel.getLevel();
        if (level == null || panel.type() != DeviceType.CONTROL_PANEL) return false;
        if (target == 1 && countReadyProjectors(level, panel.getBlockPos()) == 0) { msg(player, "no_projector_ready"); return false; }
        panel.setStateIndex(target);
        sound(level, panel.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value());
        return true;
    }

    public static int projectorsReady(Level level, BlockPos center) {
        return countReadyProjectors(level, center);
    }

    public static int speakersInRange(Level level, BlockPos center) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-PANEL_RADIUS, -PANEL_RADIUS, -PANEL_RADIUS),
                center.offset(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS))) {
            if (level.getBlockEntity(p) instanceof DeviceBlockEntity dev
                    && (dev.type() == DeviceType.SPEAKER_WALL || dev.type() == DeviceType.SPEAKER_FLOOR)) n++;
        }
        return n;
    }

    /** Ein Projektor ist am Filmende angekommen: Bedienpulte im Umkreis springen auf "gestoppt". */
    public static void notifyFilmFinished(Level level, BlockPos projectorPos) {
        for (BlockPos p : BlockPos.betweenClosed(projectorPos.offset(-PANEL_RADIUS, -PANEL_RADIUS, -PANEL_RADIUS),
                projectorPos.offset(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS))) {
            if (level.getBlockEntity(p) instanceof DeviceBlockEntity dev && dev.type() == DeviceType.CONTROL_PANEL) {
                dev.setStateIndex(0);
            }
        }
    }
}
