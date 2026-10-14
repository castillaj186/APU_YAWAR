package pe.apuyawar.systems.events;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Server-side acknowledgement gate for a finite client presentation. */
public final class PresentationCompletionGate {
    public enum Status { WAITING, READY, TIMED_OUT }
    private final int minimumTicks;
    private final int graceTicks;
    private long sequence;
    private long start;
    private final Set<UUID> expected = new HashSet<>();
    private final Set<UUID> finished = new HashSet<>();

    public PresentationCompletionGate(int minimumTicks, int graceTicks) {
        this.minimumTicks = minimumTicks;
        this.graceTicks = graceTicks;
    }
    public void begin(long id, long tick, Collection<UUID> players) {
        sequence = id; start = tick;
        expected.clear(); expected.addAll(players); finished.clear();
    }
    public void acknowledge(UUID player, long id) {
        if (sequence == id && expected.contains(player)) finished.add(player);
    }
    public Status status(long now, Collection<UUID> online) {
        expected.retainAll(online);
        if (now - start < minimumTicks) return Status.WAITING;
        if (finished.containsAll(expected)) return Status.READY;
        return now - start > minimumTicks + graceTicks ? Status.TIMED_OUT : Status.WAITING;
    }
}
