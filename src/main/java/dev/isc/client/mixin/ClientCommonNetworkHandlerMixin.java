package dev.isc.client.mixin;

import dev.isc.client.FakeLagModule;
import dev.isc.client.NoFallModule;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every packet the client sends goes through sendPacket - this is where NoFall and Fake Lag hook in. */
@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
	@Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
	private void isc$onSendPacket(Packet<?> packet, CallbackInfo ci) {
		if (!(packet instanceof PlayerMoveC2SPacket move)) {
			return;
		}

		// 1) NoFall: fake the "on ground" flag (not for packets that are only being replayed by Fake Lag)
		if (!FakeLagModule.isFlushing()) {
			NoFallModule.apply(move);
		}

		// 2) Fake Lag: hold the packet back (it is sent later)
		if (FakeLagModule.intercept(move)) {
			ci.cancel();
		}
	}
}
