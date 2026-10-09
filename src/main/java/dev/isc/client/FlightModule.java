package dev.isc.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;

/**
 * Client-side flight. Uses the vanilla player abilities (the same flight as in creative mode),
 * so the speed multiplier is simply applied to the vanilla fly speed.
 */
public final class FlightModule {
	/** Vanilla fly speed (x1). */
	public static final float VANILLA_FLY_SPEED = 0.05f;
	public static final double MIN_SPEED = 1.0;
	public static final double MAX_SPEED = 20.0;

	/**
	 * Anti-kick timing. One tick = 0.05s, so 0.1s = 2 ticks of flight,
	 * then 1 tick (the shortest pause possible, 0.01s does not fit into a tick) without flight.
	 */
	private static final int ANTI_KICK_FLIGHT_TICKS = 2;

	private static boolean enabled = false;
	private static double speed = 1.0;
	private static boolean antiKick = false;

	private static int antiKickCounter = 0;
	private static boolean wasActive = false;

	private FlightModule() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
		antiKickCounter = 0;
	}

	public static double getSpeed() {
		return speed;
	}

	public static void setSpeed(double value) {
		speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, value));
	}

	public static boolean isAntiKick() {
		return antiKick;
	}

	public static void setAntiKick(boolean value) {
		antiKick = value;
		antiKickCounter = 0;
	}

	/** Called at the start of every client tick. */
	public static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			wasActive = false;
			antiKickCounter = 0;
			return;
		}

		if (!enabled) {
			if (wasActive) {
				restore(player);
				wasActive = false;
			}
			return;
		}
		wasActive = true;

		PlayerAbilities abilities = player.getAbilities();
		abilities.allowFlying = true;
		abilities.setFlySpeed((float) (VANILLA_FLY_SPEED * speed));

		// Spectator mode has its own flight
		if (player.isSpectator()) {
			return;
		}

		// Anti-kick: briefly cancel the flight, then let it work again
		if (antiKick) {
			antiKickCounter++;
			if (antiKickCounter > ANTI_KICK_FLIGHT_TICKS) {
				antiKickCounter = 0;
				abilities.flying = false;
				return;
			}
		} else {
			antiKickCounter = 0;
		}

		// In the air flight is always on; on the ground it starts with the jump key (like creative)
		if (!player.isOnGround() || client.options.jumpKey.isPressed()) {
			abilities.flying = true;
		}
	}

	private static void restore(ClientPlayerEntity player) {
		PlayerAbilities abilities = player.getAbilities();
		abilities.setFlySpeed(VANILLA_FLY_SPEED);
		if (!player.isCreative() && !player.isSpectator()) {
			abilities.allowFlying = false;
			abilities.flying = false;
		}
	}
}
