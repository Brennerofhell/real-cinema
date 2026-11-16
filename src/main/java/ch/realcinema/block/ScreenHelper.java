package ch.realcinema.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Mehrblock-Logik der Kinoleinwand. Mehrere {@link DeviceType#CINEMA_SCREEN}-Bloecke mit gleicher Ausrichtung in einer
 * senkrechten Ebene bilden eine Flaeche. Es gibt keine eigene Struktur im Speicher: alles wird aus den Nachbarn berechnet.
 *
 * Konvention: Der Betrachter steht vor der Leinwand (auf der Seite, zu der die Vorderseite zeigt). Aus seiner Sicht liegt
 * "links" bei {@code facing.getClockWise()} (= +X im Modell), "rechts" bei {@code facing.getCounterClockWise()}.
 * Der Master-Block ist der unterste linke der Flaeche; er zeichnet das Video fuer die ganze Flaeche.
 */
public final class ScreenHelper {
    public static final int MAX_SIZE = 16;
    public static final int FLOOD_LIMIT = 256;

    private ScreenHelper() {}

    public record Surface(BlockPos master, int width, int height, Direction facing) {}

    public static boolean isScreen(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof DeviceBlock db && db.type == DeviceType.CINEMA_SCREEN
                && s.getValue(DeviceBlock.FACING) == facing;
    }

    public static Direction left(Direction facing) { return facing.getClockWise(); }
    public static Direction right(Direction facing) { return facing.getCounterClockWise(); }

    public static boolean isMaster(BlockGetter level, BlockPos pos, Direction facing) {
        return !isScreen(level, pos.relative(left(facing)), facing) && !isScreen(level, pos.below(), facing);
    }

    /** Unterster linker Block der Flaeche, zu der pos gehoert. */
    public static BlockPos findMaster(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos p = pos;
        for (int i = 0; i < MAX_SIZE && isScreen(level, p.relative(left(facing)), facing); i++) p = p.relative(left(facing));
        for (int i = 0; i < MAX_SIZE && isScreen(level, p.below(), facing); i++) p = p.below();
        // Nach dem Absenken kann links wieder etwas sein (L-Formen): noch einmal nach links.
        for (int i = 0; i < MAX_SIZE && isScreen(level, p.relative(left(facing)), facing); i++) p = p.relative(left(facing));
        return p;
    }

    /** Abmessungen der rechteckigen Flaeche ab dem Master (nach rechts und nach oben). */
    public static Surface surfaceOf(BlockGetter level, BlockPos master, Direction facing) {
        int w = 1, h = 1;
        while (w < MAX_SIZE && isScreen(level, master.relative(right(facing), w), facing)) w++;
        while (h < MAX_SIZE && isScreen(level, master.above(h), facing)) h++;
        return new Surface(master, w, h, facing);
    }

    /** Alle zusammenhaengenden Leinwandbloecke (4er-Nachbarschaft in der Ebene), begrenzt. */
    public static Set<BlockPos> connected(BlockGetter level, BlockPos start, Direction facing) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        seen.add(start);
        while (!todo.isEmpty() && seen.size() < FLOOD_LIMIT) {
            BlockPos p = todo.poll();
            for (BlockPos n : new BlockPos[]{p.relative(left(facing)), p.relative(right(facing)), p.above(), p.below()}) {
                if (!seen.contains(n) && isScreen(level, n, facing)) {
                    seen.add(n);
                    todo.add(n);
                }
            }
        }
        return seen;
    }

    /**
     * Sucht die Leinwand vor einem Projektor: entlang seiner Blickrichtung bis 32 Bloecke, seitlich +-6, vertikal -4..+8.
     * Die Leinwand muss mit der Vorderseite zum Projektor zeigen. @return Master-Block der Flaeche oder null.
     */
    @Nullable
    public static BlockPos findScreenFor(BlockGetter level, BlockPos projector, Direction projectorFacing) {
        Direction screenFacing = projectorFacing.getOpposite();
        Direction lateral = projectorFacing.getClockWise();
        for (int d = 1; d <= 32; d++) {
            BlockPos base = projector.relative(projectorFacing, d);
            for (int dy = -4; dy <= 8; dy++) {
                for (int l = -6; l <= 6; l++) {
                    BlockPos p = base.above(dy).relative(lateral, l);
                    if (isScreen(level, p, screenFacing)) return findMaster(level, p, screenFacing);
                }
            }
        }
        return null;
    }
}
