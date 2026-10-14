package pe.apuyawar.systems.judgment;

/** Shared duration of the Inti presentation. It intentionally contains no client API. */
public final class IntiPresentationTimeline {
    public static final int FRAME_COUNT = 199;
    /** 13.5 s: deliberately shorter than the 15.5 s roulette presentation. */
    public static final int DURATION_TICKS = 270;
    public static final int COMPLETION_GRACE_TICKS = 200;
    private IntiPresentationTimeline() {}
}
