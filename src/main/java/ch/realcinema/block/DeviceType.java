package ch.realcinema.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.EnumMap;
import java.util.Map;

/**
 * Beschreibt jedes datengetriebene Geraet: Modellname, Hitbox und seine Zustaende.
 * Ein Zustand legt fest, welche Animation (aus der Blockbench-Animationsdatei) gespielt wird, wohin ein Rechtsklick
 * (normal / Schleichen) fuehrt und ob der Server nach einer Zeit selbst weiterschaltet.
 *
 * Alle Animationen sind stationaere Posen oder Schleifen; GeckoLib blendet zwischen ihnen ({@link #transitionTicks}).
 * Dadurch gibt es keine Einmal-Animationen, die beim Laden einer Welt faelschlich abgespielt wuerden.
 */
public enum DeviceType implements StringRepresentable {
    FILM_CUTTER("film_cutter", 14, 16, 14, 4,
            new State("IDLE", "idle", null, 0, -1, 1, 2),
            new State("CUT", "cut", null, 24, 0, 0, 0),
            new State("CRANK", "crank_turn", null, 0, -1, 0, 0)),
    FILM_BURNER("film_burner", 14, 13, 14, 4,
            new State("IDLE", "idle", null, 0, -1, 1, 1),
            new State("BURNING", "burning", null, 100, 2, 2, 2),
            new State("DONE", "done", null, 0, -1, 0, 0)),
    CINEMA_SCREEN("cinema_screen", 16, 16, 8, 30,
            new State("CLOSED", "curtain_closed", null, 0, -1, 1, 1),
            new State("OPEN", "curtain_opened", null, 0, -1, 0, 0)),
    SPEAKER_WALL("speaker_wall", 10, 14, 8, 4,
            new State("OFF", "idle", null, 0, -1, 1, 1),
            new State("ON", "pulse", null, 0, -1, 0, 0)),
    SPEAKER_FLOOR("speaker_floor", 10, 16, 8, 4,
            new State("OFF", "idle", null, 0, -1, 1, 1),
            new State("ON", "pulse", null, 0, -1, 0, 0)),
    CONTROL_PANEL("control_panel", 16, 11, 10, 4,
            new State("STOPPED", "status_stopped", "meter_off", 0, -1, 1, 0),
            new State("PLAYING", "status_playing", "meter_active", 0, -1, 2, 0),
            new State("PAUSED", "status_paused", "meter_off", 0, -1, 1, 0)),
    CINEMA_SEAT("cinema_seat", 16, 16, 12, 10,
            new State("DOWN", "seat_down", null, 0, -1, 1, 1),
            new State("UP", "seat_up", null, 0, -1, 0, 0)),
    POPCORN_MACHINE("popcorn_machine", 14, 16, 12, 6,
            new State("OFF", "idle", null, 0, -1, 1, 1),
            new State("POPPING", "popping", null, 0, -1, 0, 0));

    /**
     * @param name       Zustandsname (Debug/NBT-Lesbarkeit)
     * @param anim       Hauptanimation (ohne Praefix "animation.<modell>.")
     * @param aux        zweite, unabhaengige Animation (z. B. Pegelanzeige) oder null
     * @param autoTicks  Server schaltet nach so vielen Ticks selbst weiter (0 = nie)
     * @param autoNext   Zielzustand dafuer
     * @param click      Zielzustand bei Rechtsklick
     * @param sneakClick Zielzustand bei Schleichen + Rechtsklick
     */
    public record State(String name, String anim, String aux, int autoTicks, int autoNext, int click, int sneakClick) {}

    public final String id;
    public final int widthPx, heightPx, depthPx;
    public final int transitionTicks;
    public final State[] states;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
    private final RawAnimation[] mainAnims;
    private final RawAnimation[] auxAnims;

    DeviceType(String id, int w, int h, int d, int transitionTicks, State... states) {
        this.id = id;
        this.widthPx = w;
        this.heightPx = h;
        this.depthPx = d;
        this.transitionTicks = transitionTicks;
        this.states = states;
        this.mainAnims = new RawAnimation[states.length];
        this.auxAnims = new RawAnimation[states.length];
        for (int i = 0; i < states.length; i++) {
            mainAnims[i] = RawAnimation.begin().thenLoop("animation." + id + "." + states[i].anim());
            if (states[i].aux() != null) {
                auxAnims[i] = RawAnimation.begin().thenLoop("animation." + id + "." + states[i].aux());
            }
        }
        // Hitbox: Geraet steht mittig auf dem Boden, Front nach NORTH; fuer die anderen Richtungen gedreht.
        double x1 = (16 - w) / 2.0, z1 = (16 - d) / 2.0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            double[] b = {x1, z1, x1 + w, z1 + d};
            int turns = switch (dir) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
            for (int k = 0; k < turns; k++) {
                // 90 Grad im Uhrzeigersinn: (x,z) -> (16 - z, x)
                b = new double[]{16 - b[3], b[0], 16 - b[1], b[2]};
            }
            shapes.put(dir, Block.box(b[0], 0, b[1], b[2], h, b[3]));
        }
    }

    /**
     * Hat das Modell leuchtende Teile und damit eine Glowmaske ({id}_glowmask.png)? GeckoLib stuerzt ab, wenn eine Maske
     * existiert, aber komplett transparent ist, und eine fehlende Maske braucht keinen Layer. Muss zum Modell passen
     * (wird von einem GameTest geprueft).
     */
    public boolean glow() {
        return switch (this) {
            case FILM_CUTTER, FILM_BURNER, CONTROL_PANEL, POPCORN_MACHINE -> true;
            default -> false;
        };
    }

    /** Oeffnet ein Rechtsklick (ohne Schleichen) eine Oberflaeche? */
    public boolean hasGui() {
        return this == FILM_BURNER || this == FILM_CUTTER || this == CONTROL_PANEL;
    }

    public VoxelShape shape(Direction facing) {
        return shapes.get(facing);
    }

    public State state(int index) {
        return states[Math.floorMod(index, states.length)];
    }

    public RawAnimation mainAnimation(int stateIndex) {
        return mainAnims[stateIndex];
    }

    public RawAnimation auxAnimation(int stateIndex) {
        return auxAnims[stateIndex];
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
