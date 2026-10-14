package pe.apuyawar.systems.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import pe.apuyawar.systems.roulette.RouletteEvents;

/** Sonidos de interfaz privados de esta animación, no audio del mundo. */
final class RouletteSoundController {
    private static final List<SoundInstance> PLAYING=new ArrayList<>();
    private RouletteSoundController() {}
    static void stop() {
        var manager=MinecraftClient.getInstance().getSoundManager();
        PLAYING.forEach(manager::stop);PLAYING.clear();
    }
    static void play(RouletteEvents.Event event) {
        var manager=MinecraftClient.getInstance().getSoundManager();
        PLAYING.removeIf(s->!manager.isPlaying(s));
        if(PLAYING.size()>=12)manager.stop(PLAYING.removeFirst());
        float pitch=event.name().equals("appear")||event.name().equals("fill")?1f+event.slot()*.025f:1f;
        float volume=event.name().equals("tick")?.22f:.55f;
        var sound=PositionedSoundInstance.master(SoundEvent.of(Identifier.of("apuyawar_systems","roulette_"+event.name())),pitch,volume);
        PLAYING.add(sound);manager.play(sound);
    }
}
