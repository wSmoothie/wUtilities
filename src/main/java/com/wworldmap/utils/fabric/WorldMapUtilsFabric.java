//? if fabric {
package com.wworldmap.utils.fabric;

import com.wworldmap.utils.policy.PolicyConfig;
import com.wworldmap.utils.policy.UtilityPolicy;
import com.wworldmap.utils.protocol.PolicyProtocol;
//? if >=1.20.5
import com.wworldmap.utils.protocol.ModPolicyPayload;
//? if >=1.20.5
import com.wworldmap.utils.protocol.WaypointsPolicyPayload;
import java.io.IOException;
import net.fabricmc.api.ModInitializer;
//? if >=1.20.5
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//? if <1.20.5
//import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//? if <1.20.5
//import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldMapUtilsFabric implements ModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("wutilities");
	//? if <1.20.5
	//private static final ResourceLocation WORLD_MAP_CHANNEL = new ResourceLocation("wworldmap", "policy");
	//? if <1.20.5
	//private static final ResourceLocation WAYPOINTS_CHANNEL = new ResourceLocation("wwaypoints", "policy");

	@Override
	public void onInitialize() {
		UtilityPolicy policy;
		try {
			var configDirectory = FabricLoader.getInstance().getConfigDir();
			policy = PolicyConfig.load(configDirectory.resolve(PolicyConfig.FILE_NAME),
				configDirectory.resolve(PolicyConfig.LEGACY_FILE_NAME), LOGGER::warn);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load wUtilities policy", exception);
		}

		//? if >=1.20.5 {
		//? if <26.1
		PayloadTypeRegistry.playS2C().register(ModPolicyPayload.TYPE, ModPolicyPayload.CODEC);
		//? if <26.1
		PayloadTypeRegistry.playS2C().register(WaypointsPolicyPayload.TYPE, WaypointsPolicyPayload.CODEC);
		//? if >=26.1
		//PayloadTypeRegistry.clientboundPlay().register(ModPolicyPayload.TYPE, ModPolicyPayload.CODEC);
		//? if >=26.1
		//PayloadTypeRegistry.clientboundPlay().register(WaypointsPolicyPayload.TYPE, WaypointsPolicyPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, ModPolicyPayload.TYPE)) {
				ServerPlayNetworking.send(handler.player, new ModPolicyPayload(policy.worldMap().disabledMask()));
			}
			if (ServerPlayNetworking.canSend(handler, WaypointsPolicyPayload.TYPE)) {
				ServerPlayNetworking.send(handler.player, new WaypointsPolicyPayload(policy.waypoints().disabledMask()));
			}
		});
		//?} else {
		/*ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, WORLD_MAP_CHANNEL)) {
				var buffer = PacketByteBufs.create();
				buffer.writeByte(PolicyProtocol.WORLD_MAP_VERSION);
				buffer.writeVarInt(policy.worldMap().disabledMask());
				ServerPlayNetworking.send(handler.player, WORLD_MAP_CHANNEL, buffer);
			}
			if (ServerPlayNetworking.canSend(handler, WAYPOINTS_CHANNEL)) {
				var buffer = PacketByteBufs.create();
				buffer.writeByte(PolicyProtocol.WAYPOINTS_VERSION);
				buffer.writeVarInt(policy.waypoints().disabledMask());
				ServerPlayNetworking.send(handler.player, WAYPOINTS_CHANNEL, buffer);
			}
		});
		*///?}
		LOGGER.info("wUtilities enabled on Fabric/Quilt; disabledWorldMapFeatures={}; disabledWaypointsFeatures={}",
			policy.worldMap().disabledFeatureIds(), policy.waypoints().disabledFeatureIds());
	}
}
//?}
