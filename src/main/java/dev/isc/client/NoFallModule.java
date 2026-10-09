package dev.isc.client;

import dev.isc.client.mixin.PlayerMoveC2SPacketAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * NoFall: the server calculates fall damage from the "on ground" flag in our movement packets.
 * While we are falling far enough to get hurt, we tell it that we are on the ground,
 * so the server keeps resetting our fall distance and never applies fall damage.
 */
public final class NoFallModule {
	/** Fall damage starts after 3 blocks - we start faking a little earlier. */
	private static final double FAKE_AFTER_FALL_DISTANCE = 2.0;

	private static boolean enabled = false;

	private NoFallModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	/** Called for every outgoing movement packet. */
	public static void apply(PlayerMoveC2SPacket packet) {
		if (!enabled) {
			return;
		}

		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null || player.isSpectator() || player.hasVehicle() || player.isGliding()) {
			return;
		}

		if (player.fallDistance > FAKE_AFTER_FALL_DISTANCE) {
			((PlayerMoveC2SPacketAccessor) packet).isc$setOnGround(true);
		}
	}
}
