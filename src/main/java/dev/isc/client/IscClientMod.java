package dev.isc.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class IscClientMod implements ClientModInitializer {
	public static final String MOD_ID = "isc_client";

	/** Whether E + Z were both held during the previous tick (so the GUI opens once per press). */
	private boolean comboWasDown = false;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.START_CLIENT_TICK.register(FlightModule::tick);
		ClientTickEvents.START_CLIENT_TICK.register(FakeLagModule::tick);
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(MinecraftClient client) {
		// Only react while in a world
		if (client.player == null || client.world == null) {
			comboWasDown = false;
			return;
		}

		long window = client.getWindow().getHandle();
		boolean comboDown = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_E)
				&& InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_Z);

		if (comboDown && !comboWasDown && canOpenFrom(client.currentScreen)) {
			client.setScreen(new IscScreen());
		}

		comboWasDown = comboDown;
	}

	/**
	 * E opens the vanilla inventory (usually in the same tick as Z is pressed),
	 * so the inventory screens are allowed to be replaced by our GUI.
	 * Other screens (chat, signs, anvils, menus...) are left alone so typing is not hijacked.
	 */
	private static boolean canOpenFrom(Screen current) {
		return current == null
				|| current instanceof InventoryScreen
				|| current instanceof CreativeInventoryScreen;
	}
}
