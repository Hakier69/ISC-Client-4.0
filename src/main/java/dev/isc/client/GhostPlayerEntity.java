package dev.isc.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;

import java.util.UUID;

/**
 * A client-only copy of the player, drawn where the server thinks we are ("See server place").
 * It uses our skin, cannot be hit, pushed, or targeted.
 */
public class GhostPlayerEntity extends OtherClientPlayerEntity {
	private final ClientPlayerEntity owner;

	public GhostPlayerEntity(ClientWorld world, ClientPlayerEntity owner) {
		super(world, new GameProfile(UUID.randomUUID(), "Server"));
		this.owner = owner;
	}

	@Override
	public SkinTextures getSkinTextures() {
		return owner.getSkinTextures();
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean shouldRenderName() {
		return false;
	}
}
