package com.piecemod.fetch;

import com.piecemod.config.ModConfig;
import com.piecemod.data.BossRegistry;
import com.piecemod.data.BossTimerManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Periodically fetches boss respawn timers by silently intercepting S2C packets.
 * The GUI is never opened on the client -- OpenScreenS2CPacket and InventoryS2CPacket
 * are cancelled while the state machine reads items and sends ClickSlotC2SPacket.
 */
public final class SilentFetcher {

    private static final Logger LOG = LoggerFactory.getLogger("piecemod");

    private static final int SCREEN_TIMEOUT_TICKS = 80;

    private static final Pattern RESPAWN_PATTERN =
            Pattern.compile("(?:Respawn(?:s| in)?|Available in|Dead for|Timer|Apparition)[:\\s]+(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(\\d+)s",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private enum State { IDLE, SENT_COMMAND, ON_NAV_SCREEN, SENT_NAV_CLICK, ON_BOSS_SCREEN, NEXT_CMD_DELAY }

    private static State state          = State.IDLE;
    private static int   activeSyncId   = -1;
    private static int   activeRevision = 0;
    private static int   waitTicks      = 0;
    private static int   fetchTimer     = 0;
    private static boolean startupFetch = true; // fire quickly on first join
    private static final List<BossRegistry.NavEntry> commandQueue = new ArrayList<>();
    private static int   commandIndex   = 0;
    private static int   totalFound     = 0;
    /** SyncIds for screens we cancelled -- their CloseScreenS2CPacket must also be cancelled. */
    private static final Set<Integer> silentSyncIds = new HashSet<>();
    private static final int INTER_CMD_DELAY_TICKS = 5;

    private SilentFetcher() {}

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SilentFetcher::tick);
    }

    // -------------------------------------------------------------------------
    // Tick handler
    // -------------------------------------------------------------------------

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            startupFetch = true; // reset so we re-fetch on next join
            return;
        }

        if (state == State.NEXT_CMD_DELAY) {
            if (++waitTicks >= INTER_CMD_DELAY_TICKS) {
                sendNextCommand(client);
            }
            return;
        }

        if (state != State.IDLE) {
            if (++waitTicks > SCREEN_TIMEOUT_TICKS) {
                String cmd = commandIndex < commandQueue.size() ? commandQueue.get(commandIndex).command() : "?";
                LOG.debug("[piecemod] Skipping {} — no response within {} ticks", cmd, SCREEN_TIMEOUT_TICKS);
                advanceToNext(client);
            }
            return;
        }

        if (client.currentScreen != null) { fetchTimer = 0; return; }
        if (BossRegistry.NAV_ENTRIES.isEmpty()) return;

        int intervalTicks = Math.max(20, ModConfig.getInstance().getAutoFetchIntervalSeconds() * 20);
        // On first join, jump the timer so the first fetch fires after ~5 seconds (100 ticks)
        if (startupFetch) {
            startupFetch = false;
            fetchTimer = intervalTicks - 100;
        }
        if (++fetchTimer >= intervalTicks) {
            fetchTimer = 0;
            startCycle(client);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Immediately triggers a fetch cycle (e.g., from a keybind or Refresh button). */
    public static void triggerNow(MinecraftClient client) {
        if (state != State.IDLE) return;
        fetchTimer = 0;
        startCycle(client);
    }

    // -------------------------------------------------------------------------
    // Packet callbacks (called from ClientPlayNetworkHandlerMixin)
    // -------------------------------------------------------------------------

    /**
     * Called when an OpenScreenS2CPacket arrives.
     *
     * @return true if the packet should be cancelled (we are handling it silently)
     */
    public static boolean onPacketOpenScreen(int syncId) {
        if (state == State.SENT_COMMAND) {
            activeSyncId = syncId;
            silentSyncIds.add(syncId);
            state = State.ON_NAV_SCREEN;
            waitTicks = 0;
            return true;
        }
        if (state == State.SENT_NAV_CLICK) {
            activeSyncId = syncId;
            silentSyncIds.add(syncId);
            state = State.ON_BOSS_SCREEN;
            waitTicks = 0;
            return true;
        }
        return false;
    }

    /**
     * Called when a CloseScreenS2CPacket arrives.
     *
     * @return true if the packet should be cancelled (we opened this screen silently)
     */
    public static boolean onPacketCloseScreen(int syncId) {
        return silentSyncIds.remove(syncId);
    }

    /**
     * Called when an InventoryS2CPacket arrives.
     *
     * @return true if the packet should be cancelled
     */
    public static boolean onPacketInventory(int syncId, int revision, List<ItemStack> contents,
                                            MinecraftClient client) {
        if (syncId != activeSyncId) return false;
        if (state == State.ON_NAV_SCREEN) {
            activeRevision = revision;
            return handleNavScreen(contents, client);
        }
        if (state == State.ON_BOSS_SCREEN) {
            activeRevision = revision;
            return handleBossScreen(contents, client);
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Private handlers
    // -------------------------------------------------------------------------

    private static boolean handleNavScreen(List<ItemStack> contents, MinecraftClient client) {
        int slot = commandQueue.get(commandIndex).subnavSlot();
        if (slot < 0) {
            // No intermediate nav -- treat this packet as the boss screen directly
            state = State.ON_BOSS_SCREEN;
            return handleBossScreen(contents, client);
        }
        if (slot < contents.size() && !contents.get(slot).isEmpty()) {
            sendClick(client, activeSyncId, activeRevision, slot);
            state = State.SENT_NAV_CLICK;
            waitTicks = 0;
            return true;
        }
        msg(client, "\u00a7c[PieceMod] Slot " + slot + " is empty in the nav screen for "
                + commandQueue.get(commandIndex).command()
                + " \u2014 check subnavSlot in BossRegistry.java.");
        advanceToNext(client);
        return true;
    }

    private static boolean handleBossScreen(List<ItemStack> contents, MinecraftClient client) {
        int found = 0;
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) continue;
            String name = strip(stack.getName().getString());
            if (name.isBlank()) continue;
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;
            for (Text line : lore.lines()) {
                String loreText = strip(line.getString());
                Matcher m = RESPAWN_PATTERN.matcher(loreText);
                if (m.find()) {
                    long hrs  = m.group(1) != null ? Long.parseLong(m.group(1)) : 0;
                    long mins = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
                    long secs = Long.parseLong(m.group(3));
                    long total = hrs * 3600L + mins * 60L + secs;
                    BossTimerManager.getInstance().update(name, total);
                    LOG.info("[piecemod] Captured timer for {}: {}h {}m {}s (lore: '{}')",
                            name, hrs, mins, secs, loreText);
                    found++;
                    break;
                }
            }
        }

        totalFound += found;
        advanceToNext(client);
        return true;
    }

    // -------------------------------------------------------------------------
    // Packet sending
    // -------------------------------------------------------------------------

    private static void sendClick(MinecraftClient client, int syncId, int revision, int slotIndex) {
        if (client.getNetworkHandler() == null) return;
        client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                syncId,
                revision,
                (short) slotIndex,
                (byte) 0,
                SlotActionType.PICKUP,
                new Int2ObjectOpenHashMap<>(),
                ItemStackHash.EMPTY
        ));
    }

    private static void sendClose(MinecraftClient client, int syncId) {
        if (client.getNetworkHandler() == null) return;
        client.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
    }

    /**
     * Closes any open silent screen, then moves to the next command in the queue.
     * Used on both success and failure so inaccessible commands are simply skipped.
     */
    private static void advanceToNext(MinecraftClient client) {
        if (activeSyncId != -1) sendClose(client, activeSyncId);
        silentSyncIds.clear();
        activeSyncId   = -1;
        activeRevision = 0;
        waitTicks      = 0;
        commandIndex++;
        if (commandIndex < commandQueue.size()) {
            state = State.NEXT_CMD_DELAY;
        } else {
            finalizeCycle(client);
        }
    }

    private static void finalizeCycle(MinecraftClient client) {
        if (totalFound > 0) {
            msg(client, "\u00a7a[PieceMod] Boss timers updated (" + totalFound + " captured).");
        } else {
            msg(client, "\u00a7c[PieceMod] No boss timers found \u2014 open a boss chest and press the Debug Dump key.");
        }
        reset();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static void startCycle(MinecraftClient client) {
        commandQueue.clear();
        commandQueue.addAll(BossRegistry.NAV_ENTRIES);
        for (String cmd : ModConfig.getInstance().getCustomCommands()) {
            commandQueue.add(new BossRegistry.NavEntry(cmd, 16));
        }
        if (commandQueue.isEmpty()) return;
        commandIndex = 0;
        totalFound   = 0;
        sendActionBar(client, "\u00a77[PieceMod] Fetching timers...");
        sendNextCommand(client);
    }

    private static void sendNextCommand(MinecraftClient client) {
        String raw = commandQueue.get(commandIndex).command();
        String cmd = raw.startsWith("/") ? raw.substring(1) : raw;
        state = State.SENT_COMMAND;
        waitTicks = 0;
        LOG.debug("[piecemod] SilentFetcher: sending /{} ({}/{})", cmd, commandIndex + 1, commandQueue.size());
        client.getNetworkHandler().sendChatCommand(cmd);
    }

    private static void reset() {
        state          = State.IDLE;
        activeSyncId   = -1;
        activeRevision = 0;
        waitTicks      = 0;
        commandQueue.clear();
        commandIndex   = 0;
        totalFound     = 0;
        silentSyncIds.clear();
    }

    private static String strip(String s) {
        if (s == null) return "";
        String r = Formatting.strip(s);
        return r == null ? "" : r;
    }

    private static void msg(MinecraftClient client, String text) {
        // Schedule on the main/render thread: sendMessage touches the chat HUD
        // (font metrics, texture atlas) which requires the render thread in MC 1.21.9.
        client.execute(() -> {
            if (client.player != null) client.player.sendMessage(Text.literal(text), false);
        });
    }

    private static void sendActionBar(MinecraftClient client, String text) {
        if (client.player != null) client.player.sendMessage(Text.literal(text), true);
    }
}
