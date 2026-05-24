package com.piecemod.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Singleton store for all known boss timers.
 * Updated by the mixin whenever the FOOSHA GUI is opened and its items load.
 * Read by the HUD renderer every frame.
 */
public final class BossTimerManager {

    private static final BossTimerManager INSTANCE = new BossTimerManager();

    /** Insertion-ordered map so the HUD displays bosses in registry order. */
    private final Map<String, TimerEntry> timers = new LinkedHashMap<>();

    private BossTimerManager() {}

    public static BossTimerManager getInstance() {
        return INSTANCE;
    }

    /**
     * Records (or refreshes) the timer for {@code bossName}.
     *
     * @param bossName         the boss name as it appears in the FOOSHA GUI
     * @param secondsRemaining the value parsed from the item lore at capture time
     */
    public void update(String bossName, long secondsRemaining) {
        timers.put(bossName, new TimerEntry(bossName, secondsRemaining));
    }

    /** Returns the latest {@link TimerEntry} for the given boss, if any has been captured. */
    public Optional<TimerEntry> get(String bossName) {
        return Optional.ofNullable(timers.get(bossName));
    }

    /** Returns an unmodifiable view of all captured timers. */
    public Map<String, TimerEntry> getAll() {
        return Collections.unmodifiableMap(timers);
    }
}
