package pe.apuyawar.systems.client.judgment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** One-shot cues advance from the animation clock, never from HUD renders. */
final class IntiSoundController {
    private static final List<SoundInstance> PLAYING = new ArrayList<>();
    private static int cue;
    private static final int[] CUE_TICKS = {0, 32, 100, 180, 226, 266};
    private static final String[] CUE_NAMES = {"inti_arrival", "inti_glitch", "inti_materialize", "inti_reveal", "inti_fade", "inti_judgment"};
    private IntiSoundController() {}
    static void begin() { stop(); cue = 0; }
    static void advance(int tick) {
        while (cue < CUE_TICKS.length && tick >= CUE_TICKS[cue]) play(CUE_NAMES[cue++]);
    }
    static void stop() { var manager = MinecraftClient.getInstance().getSoundManager(); PLAYING.forEach(manager::stop); PLAYING.clear(); }
    private static void play(String name) {
        var manager = MinecraftClient.getInstance().getSoundManager();
        PLAYING.removeIf(sound -> !manager.isPlaying(sound));
        var sound = PositionedSoundInstance.master(SoundEvent.of(Identifier.of("apuyawar_systems", name)), 1.0f, .62f);
        PLAYING.add(sound); manager.play(sound);
    }
}
