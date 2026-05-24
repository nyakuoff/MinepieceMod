package com.piecemod.mixin;

import com.piecemod.fetch.SilentFetcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts S2C screen packets during a silent fetch so the GUI is never
 * opened on the client -- zero flash, zero freeze.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onOpenScreen", at = @At("HEAD"), cancellable = true)
    private void piecemod$onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (SilentFetcher.onPacketOpenScreen(packet.getSyncId())) {
            ci.cancel();
        }
    }

    @Inject(method = "onInventory", at = @At("HEAD"), cancellable = true)
    private void piecemod$onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        if (SilentFetcher.onPacketInventory(
                packet.syncId(), packet.revision(), packet.contents(),
                MinecraftClient.getInstance())) {
            ci.cancel();
        }
    }

    @Inject(method = "onCloseScreen", at = @At("HEAD"), cancellable = true)
    private void piecemod$onCloseScreen(CloseScreenS2CPacket packet, CallbackInfo ci) {
        if (SilentFetcher.onPacketCloseScreen(packet.getSyncId())) {
            ci.cancel();
        }
    }
}
