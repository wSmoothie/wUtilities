//? if fabric {
package com.wworldmap.utils.fabric;

import com.wworldmap.utils.policy.FeaturePolicy;
import com.wworldmap.utils.policy.PolicyConfig;
import com.wworldmap.utils.protocol.PolicyProtocol;
//? if >=1.20.5
import com.wworldmap.utils.protocol.ModPolicyPayload;
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
	private static final Logger LOGGER = LoggerFactory.getLogger("wworldmap_utils");
	//? if <1.20.5
	//private static final ResourceLocation CHANNEL = new ResourceLocation("wworldmap", "policy");

	@Override
	public void onInitialize() {
		FeaturePolicy policy;
		try {
			policy = PolicyConfig.load(
				FabricLoader.getInstance().getConfigDir().resolve(PolicyConfig.FILE_NAME), LOGGER::warn);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load wWorldMap Utils policy", exception);
		}

		//? if >=1.20.5 {
		//? if <26.1
		PayloadTypeRegistry.playS2C().register(ModPolicyPayload.TYPE, ModPolicyPayload.CODEC);
		//? if >=26.1
		//PayloadTypeRegistry.clientboundPlay().register(ModPolicyPayload.TYPE, ModPolicyPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (ServerPlayNetworking.canSend(handler, ModPolicyPayload.TYPE)) {
				ServerPlayNetworking.send(handler.player, new ModPolicyPayload(policy.disabledMask()));
			}
		});
		//?} else {
		/*ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (!ServerPlayNetworking.canSend(handler, CHANNEL)) return;
			var buffer = PacketByteBufs.create();
			buffer.writeByte(PolicyProtocol.VERSION);
			buffer.writeVarInt(policy.disabledMask());
			ServerPlayNetworking.send(handler.player, CHANNEL, buffer);
		});
		*///?}
		LOGGER.info("wWorldMap Utils enabled on Fabric/Quilt; disabledFeatures={}",
			policy.disabledFeatureIds());
	}
}
//?}
