package com.example.witherforcefield;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

public class ForcefieldClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ForcefieldConfig.load();
		ForcefieldCommand.register();

		// Fires for every living-entity renderer, including both the default and slim player renderers.
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
			if (entityRenderer instanceof AvatarRenderer<?> avatarRenderer) {
				helper.register(new ForcefieldLayer(avatarRenderer));
			}
		});
	}
}
