package pe.apuyawar.systems.events;

import java.util.*;
import pe.apuyawar.systems.roulette.RouletteTimeline;

/** Espera servidor + acuses autorizados. Un acuse temprano nunca adelanta el juicio. */
public final class RouletteCompletionGate {
    public enum Status { WAITING, READY, TIMED_OUT }
    private long sequence,start;private final Set<UUID> expected=new HashSet<>(),finished=new HashSet<>();
    public void begin(long id,long tick,Collection<UUID> players){sequence=id;start=tick;expected.clear();expected.addAll(players);finished.clear();}
    public void acknowledge(UUID player,long id){if(sequence==id&&expected.contains(player))finished.add(player);}
    public Status status(long now,Collection<UUID> online){
        expected.retainAll(online);
        if(now-start<RouletteTimeline.DURATION+4)return Status.WAITING;
        if(finished.containsAll(expected))return Status.READY;
        return now-start>RouletteTimeline.DURATION+200?Status.TIMED_OUT:Status.WAITING;
    }
}
