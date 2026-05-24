package com.piecemod.mixin;

import com.piecemod.data.BossTimerManager;
import com.piecemod.debug.DebugDumpKeybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts HandledScreen renders to passively read boss respawn timers from
 * item lore when the player manually opens a container.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Unique
    private static final Logger PIECEMOD_LOG = LoggerFactory.getLogger("piecemod");

    @Shadow
    protected ScreenHandler handler;

    @Unique
    private static final Pattern RESPAWN_PATTERN =
            Pattern.compile("(?:Respawn(?:s| in)?|Available in|Dead for|Timer|Apparition)[:\\s]+(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(\\d+)s",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    @Unique private boolean piecemod$scanned = false;

    // -------------------------------------------------------------------------
    // Screen init
    // -------------------------------------------------------------------------

    @Inject(method = "init", at = @At("TAIL"))
    private void piecemod$onInit(CallbackInfo ci) {
        piecemod$scanned = false;
    }

    // -------------------------------------------------------------------------
    // Render hook -- passive timer scan
    // -------------------------------------------------------------------------

    @Inject(method = "render", at = @At("HEAD"))
    private void piecemod$onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (piecemod$scanned) return;

        // Wait until at least one slot is populated (items arrive asynchronously)
        boolean hasItems = false;
        for (Slot slot : this.handler.slots) {
            if (!slot.getStack().isEmpty()) { hasItems = true; break; }
        }
        if (!hasItems) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int captureCount = 0;
        for (Slot slot : this.handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            String cleanName = strip(stack.getName().getString()).trim();
            if (cleanName.isBlank()) continue;
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;
            for (Text line : lore.lines()) {
                String loreClean = strip(line.getString());
                Matcher matcher = RESPAWN_PATTERN.matcher(loreClean);
                if (matcher.find()) {
                    long hrs  = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
                    long mins = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : 0;
                    long secs = Long.parseLong(matcher.group(3));
                    long total = hrs * 3600L + mins * 60L + secs;
                    BossTimerManager.getInstance().update(cleanName, total);
                    PIECEMOD_LOG.info("[piecemod] Manual capture for {}: {}h {}m {}s (lore: '{}')",
                            cleanName, hrs, mins, secs, loreClean);
                    captureCount++;
                    break;
                }
            }
        }
        piecemod$scanned = true;
        if (captureCount > 0 && mc.player != null) {
            mc.player.sendMessage(
                    Text.literal("\u00a7a[PieceMod] Captured " + captureCount + " boss timer(s)."),
                    true);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    @Unique
    private static String strip(String s) {
        if (s == null) return "";
        String stripped = Formatting.strip(s);
        return stripped == null ? "" : stripped;
    }

    // -------------------------------------------------------------------------
    // Debug dump key -- fires even when this screen is open
    // -------------------------------------------------------------------------

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void piecemod$onKeyPressed(net.minecraft.client.input.KeyInput keyInput,
            CallbackInfoReturnable<Boolean> cir) {
        if (DebugDumpKeybind.matchesKey(keyInput.key(), keyInput.scancode())) {
            DebugDumpKeybind.performDump(MinecraftClient.getInstance());
            cir.setReturnValue(true);
        }
    }
}