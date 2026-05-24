package com.piecemod.data;

/**
 * Snapshot of a boss timer captured when the FOOSHA GUI was last opened.
 * The timer counts down live using the system clock so the HUD stays current
 * without needing to re-open the GUI.
 */
public class TimerEntry {

    private final String bossName;
    private final long capturedSecondsRemaining;
    private final long captureTimestampMs;

    public TimerEntry(String bossName, long capturedSecondsRemaining) {
        this.bossName = bossName;
        this.capturedSecondsRemaining = capturedSecondsRemaining;
        this.captureTimestampMs = System.currentTimeMillis();
    }

    public String getBossName() {
        return bossName;
    }

    /**
     * Returns the time remaining in seconds, counting down from the moment
     * this entry was captured. Returns 0 once the timer has expired.
     */
    public long getLiveSecondsRemaining() {
        long elapsed = (System.currentTimeMillis() - captureTimestampMs) / 1000L;
        return Math.max(0L, capturedSecondsRemaining - elapsed);
    }

    public long getCaptureTimestampMs() {
        return captureTimestampMs;
    }

    /** True when the live countdown has reached zero (boss is spawning / has spawned). */
    public boolean isSpawning() {
        return getLiveSecondsRemaining() == 0;
    }

    /** Minutes elapsed since this entry was captured from the GUI. */
    public long minutesSinceCapture() {
        return (System.currentTimeMillis() - captureTimestampMs) / 60_000L;
    }
}
