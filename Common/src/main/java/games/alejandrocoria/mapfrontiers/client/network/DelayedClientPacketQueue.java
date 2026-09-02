package games.alejandrocoria.mapfrontiers.client.network;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

@ParametersAreNonnullByDefault
final class DelayedClientPacketQueue {
    private record Entry(long dueTick, Runnable action) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();
    private long currentTick;
    private int delayTicks;
    private boolean holding;
    private boolean draining;

    public void submit(Runnable action) {
        entries.addLast(new Entry(currentTick + delayTicks, Objects.requireNonNull(action)));
        drainDueEntries();
    }

    public void tick() {
        ++currentTick;
        drainDueEntries();
    }

    public void setDelayTicks(int delayTicks) {
        if (delayTicks < 0) {
            throw new IllegalArgumentException("delayTicks cannot be negative");
        }

        this.delayTicks = delayTicks;
        drainDueEntries();
    }

    public void hold() {
        holding = true;
    }

    public void resume() {
        holding = false;
        drainDueEntries();
    }

    public boolean runNext() {
        Entry entry = entries.pollFirst();
        if (entry == null) {
            return false;
        }

        runEntry(entry);
        return true;
    }

    public int flush() {
        int executed = 0;
        while (runNext()) {
            ++executed;
        }
        return executed;
    }

    public int clear() {
        int cleared = entries.size();
        entries.clear();
        return cleared;
    }

    public void discardPendingPackets() {
        clear();
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public boolean isHolding() {
        return holding;
    }

    public int getPendingCount() {
        return entries.size();
    }

    private void drainDueEntries() {
        if (holding || draining) {
            return;
        }

        draining = true;
        try {
            while (true) {
                Entry entry = entries.peekFirst();
                if (entry == null || entry.dueTick() > currentTick) {
                    return;
                }

                entries.removeFirst();
                entry.action().run();
            }
        } finally {
            draining = false;
        }
    }

    private void runEntry(Entry entry) {
        boolean wasDraining = draining;
        draining = true;
        try {
            entry.action().run();
        } finally {
            draining = wasDraining;
        }
    }
}
