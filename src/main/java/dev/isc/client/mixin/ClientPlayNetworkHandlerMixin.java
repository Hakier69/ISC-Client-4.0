package dev.isc.client.mixin;

import dev.isc.client.FakeLagModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	/** The server teleported us - old held movement packets would only cause rubber-banding, so drop them. */
	@Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
	private void isc$onServerTeleport(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
		// This handler is first called on the network thread and then again on the game thread - only react once
		if (MinecraftClient.getInstance().isOnThread()) {
			FakeLagModule.onServerTeleport();
		}
	}
}
