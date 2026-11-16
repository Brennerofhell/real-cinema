package ch.realcinema.block;

/** Betriebszustand des Projektors. Der Server ist autoritativ, Clients leiten daraus die Animation ab. */
public enum ProjectorState {
    OFF(false),
    STARTING(true),
    RUNNING(true),
    STOPPING(false);

    /** Lampe an (Licht und Leuchtanimation). */
    public final boolean lampOn;

    ProjectorState(boolean lampOn) {
        this.lampOn = lampOn;
    }

    /** Zielgeschwindigkeit der Mechanik (0..1), gegen die der Client sanft beschleunigt/bremst. */
    public float targetSpeed() {
        return (this == STARTING || this == RUNNING) ? 1.0f : 0.0f;
    }
}
