package net.inertiamc.anythinginabucket.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Tells a held right click from a new one. A held click repeats every 4 ticks (200 ms), which would release a mob right
 * after catching it, or catch it again right after releasing. After a catch or release, clicks arriving at that pace
 * are ignored until there is a gap.
 */
public final class Settle {

    /** A repeat's 200 ms plus a tick of lag, shorter than a release and re-click. */
    public static final long GAP = 300;

    private final LongSupplier clock;
    /** Last time a held click was seen, tracked only after a catch or release. */
    private final Map<UUID, Long> held = new ConcurrentHashMap<>();

    public Settle() {
        this(System::currentTimeMillis);
    }

    public Settle(LongSupplier clock) {
        this.clock = clock;
    }

    public void acted(UUID player) {
        held.put(player, clock.getAsLong());
    }

    /** True when this right click is the same held click still repeating. */
    public boolean repeat(UUID player) {
        Long last = held.get(player);
        if (last == null) return false;
        long now = clock.getAsLong();
        if (now - last < GAP) {
            held.put(player, now);
            return true;
        }
        held.remove(player, last);
        return false;
    }

    public void clear(UUID player) {
        held.remove(player);
    }

    public void clearAll() {
        held.clear();
    }
}
