package pe.apuyawar.systems.client;

import pe.apuyawar.systems.roulette.RouletteTimeline;
import pe.apuyawar.systems.roulette.RouletteEvents;
import net.minecraft.client.MinecraftClient;

/** Reloj local. Los cambios de ganador provienen del paquete del servidor. */
public final class RouletteClientState {
    private static boolean running = false;
    private static int elapsed = 0;
    private static int winner = 0;
    private static long sequence;
    private static final RouletteEvents events = new RouletteEvents();
    private RouletteClientState() {}
    public static void begin(int colorIndex, long sequenceId) {
        if (colorIndex < 0 || colorIndex > 7) return;
        cancel();
        events.reset();
        sequence = sequenceId;
        winner = colorIndex;
        elapsed = 0;
        running = true;
    }
    public static void cancel() {
        running = false;
        RouletteSoundController.stop();
        RouletteParticles.clear();
    }
    public static void tick(MinecraftClient client) {
        if (client.world == null) { if(running)cancel(); return; }
        if (!running || client.isPaused()) return;
        ++elapsed;
        RouletteParticles.prune(elapsed);
        events.advance(elapsed,winner,event->{
            RouletteSoundController.play(event);
            RouletteParticles.emit(event,winner);
        });
        if(elapsed>=RouletteTimeline.DURATION){
            if(net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(pe.apuyawar.systems.network.RouletteFinishedPayload.ID))
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new pe.apuyawar.systems.network.RouletteFinishedPayload(sequence));
            cancel();
        }
    }
    public static boolean isRunning() { return running; }
    public static int winner() { return winner; }
    public static float time(float partial) { return Math.min(RouletteTimeline.DURATION, elapsed + partial); }
}
