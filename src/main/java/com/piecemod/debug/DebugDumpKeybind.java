package com.piecemod.debug;

import com.piecemod.util.TextUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Press the debug keybind (default: unbound — set it in Controls > Key Binds)
 * while any chest/container GUI is open to dump every slot's name and lore
 * to the log AND to the chat. Useful for figuring out what the server
 * actually sends in custom menus.
 */
public final class DebugDumpKeybind {

    private static final Logger LOG = LoggerFactory.getLogger("piecemod-debug");

    private static KeyBinding key;

    private DebugDumpKeybind() {}

    public static void register() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.piecemod.dump_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // unbound by default
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.wasPressed()) {
                dump(client);
            }
        });
    }

    /** Returns true if the given key event matches the dump keybinding (works when a GUI is open). */
    public static boolean matchesKey(int keyCode, int scanCode) {
        if (key == null) return false;
        return key.matchesKey(new net.minecraft.client.input.KeyInput(keyCode, scanCode, 0));
    }

    /** Public alias used by the screen mixin so the dump fires while a GUI is open. */
    public static void performDump(MinecraftClient client) {
        dump(client);
    }

    private static void dump(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> hs)) {
            chat(client, "[piecemod] Open a chest GUI first.");
            return;
        }

        String rawTitle   = hs.getTitle().getString();
        String cleanTitle = TextUtil.strip(rawTitle);
        var handler = hs.getScreenHandler();
        int slotCount = handler.slots.size();

        String header = "[piecemod] === Screen dump ===";
        chat(client, header);
        LOG.info(header);
        log(client, "  class:  " + hs.getClass().getName());
        log(client, "  title:  raw='" + rawTitle + "' clean='" + cleanTitle + "'");
        log(client, "  slots:  " + slotCount);

        for (int i = 0; i < slotCount; i++) {
            Slot slot = handler.slots.get(i);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String name = TextUtil.strip(stack.getName().getString()).trim();
            log(client, "  [" + i + "] " + stack.getItem() + "  name='" + name + "'");

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                int li = 0;
                for (Text line : lore.lines()) {
                    String clean = TextUtil.strip(line.getString());
                    log(client, "        lore[" + (li++) + "] '" + clean + "'");
                }
            }
        }

        chat(client, "[piecemod] Dump complete — check logs/latest.log");
    }

    private static void log(MinecraftClient client, String msg) {
        LOG.info(msg);
        chat(client, msg);
    }

    private static void chat(MinecraftClient client, String msg) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(msg), false);
        }
    }
}
