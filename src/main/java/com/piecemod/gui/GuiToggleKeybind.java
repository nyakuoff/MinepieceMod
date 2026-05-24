package com.piecemod.gui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Toggles {@link BossTimerScreen} open/closed when Right Shift is pressed.
 * Uses direct GLFW key polling so it works regardless of in-game focus state.
 */
public final class GuiToggleKeybind {

    private static boolean prevDown = false;

    private GuiToggleKeybind() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GuiToggleKeybind::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) return;

        boolean down = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);

        if (down && !prevDown) {
            // Rising edge -- toggle the screen
            if (client.currentScreen instanceof BossTimerScreen) {
                client.setScreen(null);
            } else if (client.currentScreen == null) {
                client.setScreen(new BossTimerScreen());
            }
        }

        prevDown = down;
    }
}
