package com.piecemod;

import com.piecemod.config.ConfigManager;
import com.piecemod.debug.DebugDumpKeybind;
import com.piecemod.fetch.FetchKeybind;
import com.piecemod.fetch.SilentFetcher;
import com.piecemod.gui.GuiToggleKeybind;
import com.piecemod.hud.BossTimerHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

/**
 * Client-side entrypoint for PieceMod.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Load saved config on startup
 *   <li>Register the boss-timer HUD overlay after the vanilla boss bar layer
 * </ol>
 */
public class PieceModClient implements ClientModInitializer {

    public static final String MOD_ID = "piecemod";

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        DebugDumpKeybind.register();
        SilentFetcher.register();
        FetchKeybind.register();
        GuiToggleKeybind.register();

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                Identifier.of(MOD_ID, "boss_timer"),
                (context, tickCounter) ->
                        BossTimerHud.render(context, tickCounter.getTickProgress(false))
        );
    }
}
