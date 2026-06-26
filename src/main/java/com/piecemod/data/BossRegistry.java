package com.piecemod.data;

import java.util.List;

/**
 * Hard-coded registry for the server commands that open boss menus.
 * Edit this file and rebuild -- no in-game setup required.
 */
public final class BossRegistry {

    /**
     * Pairs a slash-command with the slot to click on the nav screen.
     * subnavSlot: 0-based index of the "Chasseur/Boss" button on the first menu.
     * Use -1 if the command opens the boss list directly (no intermediate menu).
     */
    public record NavEntry(String command, int subnavSlot) {}

    /**
     * One entry per boss-menu command.  Order determines the fetch cycle order.
     * Dump any nav screen with the Debug key to find the correct slot number.
     * Both /fuchsia and /fishmen show the Chasseur category button at slot 16.
     */
    public static final List<NavEntry> NAV_ENTRIES = List.of(
            new NavEntry("/fuchsia",       16),  // Chasseur/Bandits
            new NavEntry("/drum",          16),  // verify slot
            new NavEntry("/alabasta",      16),  // verify slot
            new NavEntry("/thriller_bark", 16),  // verify slot
            new NavEntry("/sabaody",       16),  // verify slot
            new NavEntry("/fishmen",       16),  // Chasseur/Hommes-Poissons
            new NavEntry("/dressrosa",     16),  // verify slot
            new NavEntry("/komugi",        16),  // verify slot
            new NavEntry("/whole_cake",    16)  // verify slot
    );

    private BossRegistry() {}
}


