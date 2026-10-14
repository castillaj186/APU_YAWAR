package pe.apuyawar.systems.client.judgment;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import pe.apuyawar.systems.judgment.IntiPresentationTimeline;
import pe.apuyawar.systems.network.IntiFinishedPayload;

/** Local, frame-rate independent state; it never changes a server challenge directly. */
public final class IntiClientState {
    private static boolean running;
    private static boolean previewOnly;
    private static int elapsed;
    private static long sequence;
    private IntiClientState() {}
    public static void begin(long sequenceId, boolean preview) {
        cancel();
        sequence = sequenceId;
        previewOnly = preview;
        elapsed = 0;
        running = true;
        IntiSoundController.begin();
    }
    public static void tick(MinecraftClient client) {
        if (!running) return;
        if (client.world == null) { cancel(); return; }
        if (client.isPaused()) return;
        elapsed++;
        IntiSoundController.advance(elapsed);
        if (elapsed >= IntiPresentationTimeline.DURATION_TICKS) {
            boolean standalone = previewOnly;
            long completed = sequence;
            // Keep the final impact audible while the server title reaches the client.
            running = false;
            if (!standalone && ClientPlayNetworking.canSend(IntiFinishedPayload.ID)) {
                ClientPlayNetworking.send(new IntiFinishedPayload(completed));
            }
        }
    }
    public static void cancel() { running = false; IntiSoundController.stop(); }
    public static boolean isRunning() { return running; }
    public static float time(float partial) { return Math.min(IntiPresentationTimeline.DURATION_TICKS, elapsed + partial); }
    public static int frame(float partial) {
        return Math.min(IntiPresentationTimeline.FRAME_COUNT - 1,
            (int) (time(partial) * IntiPresentationTimeline.FRAME_COUNT / IntiPresentationTimeline.DURATION_TICKS));
    }
}
