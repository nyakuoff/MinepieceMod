package com.piecemod.hud;

import com.piecemod.gui.BossTimerScreen;
import com.piecemod.config.ModConfig;
import com.piecemod.data.BossTimerManager;
import com.piecemod.data.TimerEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders the boss timer overlay onto the in-game HUD.
 *
 * <p>The overlay is hidden whenever any screen is open (e.g., inventory, chat).
 * It draws a translucent background box sized to fit all active timer lines.
 */
public class BossTimerHud {

    private static final int TEXT_HEIGHT   = 9;
    private static final int LINE_SPACING  = 2;
    private static final int PADDING       = 4;
    private static final float HUD_SCALE   = 0.85f;

    /**
     * Called every frame by the registered {@link net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement}.
     *
     * @param context   the draw context for this frame
     * @param deltaTick partial tick for smooth animation (unused here but available)
     */
    public static void render(DrawContext context, float deltaTick) {
        MinecraftClient mc = MinecraftClient.getInstance();

        // Hide the HUD while any screen is open (except our own panel)
        if (mc.currentScreen != null && !(mc.currentScreen instanceof BossTimerScreen)) return;

        ModConfig config = ModConfig.getInstance();
        if (!config.isHudVisible()) return;

        List<String> lines = buildLines(config);
        if (lines.isEmpty()) return;

        int boxWidth  = getMaxTextWidth(mc, lines) + PADDING * 2;
        int boxHeight = lines.size() * (TEXT_HEIGHT + LINE_SPACING) - LINE_SPACING + PADDING * 2;

        final int margin = 5;
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        // Compute anchor for the scaled box so it stays in the chosen corner
        int scaledW = (int) (boxWidth  * HUD_SCALE);
        int scaledH = (int) (boxHeight * HUD_SCALE);
        int ox, oy;
        switch (config.getHudPosition()) {
            case TOP_RIGHT    -> { ox = sw - scaledW - margin; oy = margin; }
            case BOTTOM_LEFT  -> { ox = margin;                oy = sh - scaledH - margin; }
            case BOTTOM_RIGHT -> { ox = sw - scaledW - margin; oy = sh - scaledH - margin; }
            default           -> { ox = margin;                oy = margin; } // TOP_LEFT
        }

        // Save matrix state (JOML Matrix3x2f public fields) and apply transform
        var m = context.getMatrices();
        float s00=m.m00, s01=m.m01, s10=m.m10, s11=m.m11, s20=m.m20, s21=m.m21;
        m.translate(ox, oy);
        m.scale(HUD_SCALE, HUD_SCALE);

        // Draw background
        context.fill(0, 0, boxWidth, boxHeight, config.getBackgroundColor());

        // Draw each timer line
        int textY = PADDING;
        for (String line : lines) {
            context.drawTextWithShadow(mc.textRenderer, line, PADDING, textY, 0xFFFFFFFF);
            textY += TEXT_HEIGHT + LINE_SPACING;
        }

        // Restore matrix state
        m.m00=s00; m.m01=s01; m.m10=s10; m.m11=s11; m.m20=s20; m.m21=s21;
    }

    // -------------------------------------------------------------------------

    private static List<String> buildLines(ModConfig config) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, TimerEntry> e : BossTimerManager.getInstance().getAll().entrySet()) {
            if (!config.isBossEnabled(e.getKey())) continue;
            lines.add(e.getKey() + ": " + formatTimer(e.getValue()));
        }
        return lines;
    }

    private static String formatTimer(TimerEntry timer) {
        if (timer.isSpawning()) return "SPAWNING";
        long secs = timer.getLiveSecondsRemaining();
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    private static int getMaxTextWidth(MinecraftClient mc, List<String> lines) {
        int max = 0;
        for (String line : lines) {
            int w = mc.textRenderer.getWidth(line);
            if (w > max) max = w;
        }
        return max;
    }
}
