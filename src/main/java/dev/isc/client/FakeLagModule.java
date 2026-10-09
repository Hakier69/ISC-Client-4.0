package dev.isc.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake Lag: it alternates between two phases.
 *  - normal phase: everything works as usual,
 *  - lag phase: our movement packets are held back, so other players see us freeze.
 * When the lag phase ends, all held packets are sent at once and we "jump" to the real position.
 * Locally nothing changes - we keep moving normally the whole time.
 *
 * "See server place" draws a second copy of us where the server thinks we are during the lag phase.
 */
public final class FakeLagModule {
	public static final int MIN_LAG_MS = 100;
	public static final int MAX_LAG_MS = 2000;
	public static final int MIN_INTERVAL_MS = 100;
	public static final int MAX_INTERVAL_MS = 3000;

	private static final int MS_PER_TICK = 50;

	// settings
	private static boolean enabled = false;
	private static boolean seeServerPlace = false;
	private static int lagMs = 300;       // how long packets are held
	private static int intervalMs = 600;  // how long everything is normal between two lags

	// state
	private static final List<PlayerMoveC2SPacket> held = new ArrayList<>();
	private static boolean lagging = false;
	private static boolean flushing = false;
	private static int phaseTicks = 0;
	private static ClientPlayerEntity trackedPlayer = null;

	// the last position the server really got from us
	private static boolean hasServerPos = false;
	private static double serverX, serverY, serverZ;
	private static float serverYaw, serverPitch;

	// "See server place"
	private static GhostPlayerEntity ghost = null;
	private static ClientWorld ghostWorld = null;
	private static int ghostTicks = 0;

	private FakeLagModule() {
	}

	// ---------- settings ----------

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		if (enabled == value) {
			return;
		}
		enabled = value;
		if (!value) {
			lagging = false;
			phaseTicks = 0;
			flush();
			removeGhost();
		}
	}

	public static boolean isSeeServerPlace() {
		return seeServerPlace;
	}

	public static void setSeeServerPlace(boolean value) {
		seeServerPlace = value;
		if (!value) {
			removeGhost();
		}
	}

	public static int getLagMs() {
		return lagMs;
	}

	public static void setLagMs(int value) {
		lagMs = Math.max(MIN_LAG_MS, Math.min(MAX_LAG_MS, value));
	}

	public static int getIntervalMs() {
		return intervalMs;
	}

	public static void setIntervalMs(int value) {
		intervalMs = Math.max(MIN_INTERVAL_MS, Math.min(MAX_INTERVAL_MS, value));
	}

	public static boolean isFlushing() {
		return flushing;
	}

	// ---------- packets ----------

	/** Called for every outgoing movement packet. Returns true when the packet is held back. */
	public static boolean intercept(PlayerMoveC2SPacket packet) {
		if (enabled && lagging && !flushing) {
			held.add(packet);
			return true;
		}
		recordSent(packet);
		return false;
	}

	private static void recordSent(PlayerMoveC2SPacket packet) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			return;
		}
		double cx = hasServerPos ? serverX : player.getX();
		double cy = hasServerPos ? serverY : player.getY();
		double cz = hasServerPos ? serverZ : player.getZ();
		float cyaw = hasServerPos ? serverYaw : player.getYaw();
		float cpitch = hasServerPos ? serverPitch : player.getPitch();

		serverX = packet.getX(cx);
		serverY = packet.getY(cy);
		serverZ = packet.getZ(cz);
		serverYaw = packet.getYaw(cyaw);
		serverPitch = packet.getPitch(cpitch);
		hasServerPos = true;
	}

	/** Sends everything that was held back, in the original order. */
	private static void flush() {
		if (held.isEmpty()) {
			return;
		}
		List<PlayerMoveC2SPacket> packets = new ArrayList<>(held);
		held.clear();

		ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
		if (handler == null) {
			return;
		}
		flushing = true;
		try {
			for (PlayerMoveC2SPacket packet : packets) {
				handler.sendPacket(packet);
			}
		} finally {
			flushing = false;
		}
	}

	/** The server moved us (teleport, rubber-band...) - held packets are outdated now. */
	public static void onServerTeleport() {
		held.clear();
		lagging = false;
		phaseTicks = 0;
		hasServerPos = false;
		removeGhost();
	}

	// ---------- tick ----------

	public static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			reset();
			return;
		}
		// New player object = respawn / dimension change / rejoin
		if (player != trackedPlayer) {
			reset();
			trackedPlayer = player;
		}
		if (!enabled) {
			return;
		}

		phaseTicks++;
		if (lagging) {
			if (phaseTicks >= toTicks(lagMs)) {
				flush();
				lagging = false;
				phaseTicks = 0;
			}
		} else if (phaseTicks >= toTicks(intervalMs)) {
			lagging = true;
			phaseTicks = 0;
			if (!hasServerPos) {
				serverX = player.getX();
				serverY = player.getY();
				serverZ = player.getZ();
				serverYaw = player.getYaw();
				serverPitch = player.getPitch();
				hasServerPos = true;
			}
		}

		updateGhost(client, player);
	}

	private static int toTicks(int ms) {
		return Math.max(1, Math.round(ms / (float) MS_PER_TICK));
	}

	private static void reset() {
		held.clear();
		lagging = false;
		phaseTicks = 0;
		hasServerPos = false;
		ghost = null;
		ghostWorld = null;
		trackedPlayer = null;
	}

	// ---------- "See server place" ----------

	private static void updateGhost(MinecraftClient client, ClientPlayerEntity player) {
		boolean want = enabled && seeServerPlace && lagging && hasServerPos;
		if (!want) {
			removeGhost();
			return;
		}

		ClientWorld world = client.world;
		if (ghost != null && ghostWorld != world) {
			ghost = null; // the old world is gone together with the ghost
			ghostWorld = null;
		}
		if (ghost == null) {
			ghost = new GhostPlayerEntity(world, player);
			ghostWorld = world;
			ghostTicks = 0;
			world.addEntity(ghost);
		}

		ghost.refreshPositionAndAngles(serverX, serverY, serverZ, serverYaw, serverPitch);
		ghost.setHeadYaw(serverYaw);
		ghost.setBodyYaw(serverYaw);
		ghost.setPose(player.getPose());
		ghost.setSneaking(player.isSneaking());

		// Armor and held items (not every tick)
		if (ghostTicks++ % 10 == 0) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ghost.equipStack(slot, player.getEquippedStack(slot).copy());
			}
		}
	}

	private static void removeGhost() {
		if (ghost != null) {
			ghost.discard();
		}
		ghost = null;
		ghostWorld = null;
	}
}
