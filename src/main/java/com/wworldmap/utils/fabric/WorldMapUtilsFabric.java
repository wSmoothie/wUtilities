package com.wworldmap.utils.fabric;

import com.wworldmap.utils.policy.FeaturePolicy;
import com.wworldmap.utils.policy.PolicyConfig;
import java.io.IOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldMapUtilsFabric implements ModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("wworldmap_utils");

	@Override
	public void onInitialize() {
		FeaturePolicy policy;
		try {
			policy = PolicyConfig.load(
				FabricLoader.getInstance().getConfigDir().resolve(PolicyConfig.FILE_NAME),
				LOGGER::warn);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load wWorldMap Utils policy", exception);
		}

		PayloadTypeRegistry.playS2C().register(PolicyPayload.TYPE, PolicyPayload.CODEC);
		FeaturePolicy loadedPolicy = policy;
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, PolicyPayload.TYPE)) {
				ServerPlayNetworking.send(handler.player, new PolicyPayload(loadedPolicy.disabledMask()));
			}
		});
		LOGGER.info("wWorldMap Utils enabled on Fabric; disabledFeatures={}",
			policy.disabledFeatureIds());
	}
}
