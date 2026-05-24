package com.piecemod.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory configuration state for PieceMod.
 * Loaded from / saved to {@code config/piecemod.json} by {@link ConfigManager}.
 */
public class ModConfig {

    /** Corner of the screen where the boss timer HUD is anchored. */
    public enum HudPosition {
        TOP_LEFT("Top Left"),
        TOP_RIGHT("Top Right"),
        BOTTOM_LEFT("Bottom Left"),
        BOTTOM_RIGHT("Bottom Right");

        public final String label;

        HudPosition(String label) { this.label = label; }

        public HudPosition next() {
            HudPosition[] v = values();
            return v[(ordinal() + 1) % v.length];
        }
    }

    /** Visual theme for the overlay GUI. */
    public enum Theme {
        //          label       bg          header      headerText
        //          row         rowHover    sub         subHover
        //          action      actionHover textPrimary textDim
        //          colorOn     colorOff    colorTimer  colorAccent
        DARK  ("Dark",
                0xEE111111, 0xFF0D0D0D, 0xFFFFFFFF,
                0xFF1A1A1A, 0xFF252525, 0xFF141414, 0xFF1E1E1E,
                0xFF191919, 0xFF242424, 0xFFFFFFFF, 0xFF888888,
                0xFF55DD66, 0xFF555555, 0xFFFFFFFF, 0xFFAAAAAA),
        LIGHT ("Light",
                0xEEF2F2F2, 0xFFE0E0E0, 0xFF111111,
                0xFFEAEAEA, 0xFFDCDCDC, 0xFFE2E2E2, 0xFFD4D4D4,
                0xFFDDDDDD, 0xFFCCCCCC, 0xFF111111, 0xFF555555,
                0xFF117711, 0xFF888888, 0xFF222222, 0xFF333333),
        BLUE  ("Blue",
                0xEE04080F, 0xFF060C18, 0xFF88CCFF,
                0xFF08101E, 0xFF0F1C30, 0xFF050A14, 0xFF0C1828,
                0xFF071428, 0xFF0F2040, 0xFFCCEEFF, 0xFF6688AA,
                0xFF44DDFF, 0xFF2A4A66, 0xFF88DDFF, 0xFF55AAFF),
        GREEN ("Green",
                0xEE020902, 0xFF030C03, 0xFF33FF33,
                0xFF051005, 0xFF091809, 0xFF040A04, 0xFF071407,
                0xFF061506, 0xFF0A2A0A, 0xFF88FF88, 0xFF448844,
                0xFF33FF33, 0xFF1A4A1A, 0xFF88FF88, 0xFF33FF33),
        ROSE  ("Rose",
                0xEE140404, 0xFF1C0606, 0xFFFFAAAA,
                0xFF1A0808, 0xFF260E0E, 0xFF120404, 0xFF1C0A0A,
                0xFF1E0814, 0xFF2C1020, 0xFFFFCCCC, 0xFFCC8888,
                0xFFFF5566, 0xFF662233, 0xFFFFAAAA, 0xFFFF7788);

        public final String label;
        public final int bg, header, headerText;
        public final int row, rowHover, sub, subHover;
        public final int action, actionHover, textPrimary, textDim;
        public final int colorOn, colorOff, colorTimer, colorAccent;

        Theme(String label,
              int bg, int header, int headerText,
              int row, int rowHover, int sub, int subHover,
              int action, int actionHover, int textPrimary, int textDim,
              int colorOn, int colorOff, int colorTimer, int colorAccent) {
            this.label = label;
            this.bg = bg; this.header = header; this.headerText = headerText;
            this.row = row; this.rowHover = rowHover;
            this.sub = sub; this.subHover = subHover;
            this.action = action; this.actionHover = actionHover;
            this.textPrimary = textPrimary; this.textDim = textDim;
            this.colorOn = colorOn; this.colorOff = colorOff;
            this.colorTimer = colorTimer; this.colorAccent = colorAccent;
        }
    }

    private static final ModConfig INSTANCE = new ModConfig();

    // HUD position (top-left corner of the overlay box)
    private int hudX = 5;
    private int hudY = 5;

    // HUD visibility and position
    private boolean hudVisible  = true;
    private HudPosition hudPosition = HudPosition.TOP_LEFT;

    // Theme
    private Theme theme = Theme.DARK;

    // Background fill color in ARGB format (default: 53% opaque black)
    private int backgroundColor = 0x88000000;

    // Auto-fetch settings
    private boolean autoFetchEnabled = true;
    private int autoFetchIntervalSeconds = 60; // 1 minute

    // Per-boss visibility toggle (true = show on HUD)
    private final Map<String, Boolean> bossEnabled = new HashMap<>();

    // Custom commands added by the player in-game
    private final List<String> customCommands = new ArrayList<>();

    private ModConfig() {}

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    // --- HUD visibility / position ---

    public boolean isHudVisible() { return hudVisible; }
    public void setHudVisible(boolean v) { hudVisible = v; }

    public HudPosition getHudPosition() { return hudPosition; }
    public void setHudPosition(HudPosition v) { hudPosition = v; }

    // --- Theme ---

    public Theme getTheme() { return theme; }
    public void setTheme(Theme t) { theme = t; }

    // --- HUD position (legacy x/y kept for config serialisation) ---

    public int getHudX() { return hudX; }
    public void setHudX(int v) { hudX = v; }

    public int getHudY() { return hudY; }
    public void setHudY(int v) { hudY = v; }

    // --- Background color ---

    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int v) { backgroundColor = v; }

    // --- Auto-fetch ---

    public boolean isAutoFetchEnabled() { return autoFetchEnabled; }
    public void setAutoFetchEnabled(boolean v) { autoFetchEnabled = v; }

    public int getAutoFetchIntervalSeconds() { return autoFetchIntervalSeconds; }
    public void setAutoFetchIntervalSeconds(int v) { autoFetchIntervalSeconds = Math.max(30, v); }

    // --- Per-boss toggles ---

    public boolean isBossEnabled(String boss) {
        return bossEnabled.getOrDefault(boss, false);
    }

    public void setBossEnabled(String boss, boolean v) {
        bossEnabled.put(boss, v);
    }

    public Map<String, Boolean> getBossEnabled() {
        return bossEnabled;
    }

    // --- Custom commands ---

    public List<String> getCustomCommands() { return customCommands; }

    public void addCustomCommand(String cmd) {
        if (!customCommands.contains(cmd)) customCommands.add(cmd);
    }

    public void removeCustomCommand(String cmd) { customCommands.remove(cmd); }
}
