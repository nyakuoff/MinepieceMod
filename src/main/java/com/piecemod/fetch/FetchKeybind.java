package com.piecemod.fetch;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the "Refresh Timers" keybinding (unbound by default).
 * Pressing it immediately triggers a silent fetch cycle.
 */
public final class FetchKeybind {

    private static KeyBinding refreshKey;

    private FetchKeybind() {}

    public static void register() {
        refreshKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.piecemod.refresh_timers",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register((MinecraftClient client) -> {
            while (refreshKey.wasPressed()) {
                SilentFetcher.triggerNow(client);
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("\u00a7e[PieceMod] Refreshing boss timers..."), true);
                }
            }
        });
    }
}
