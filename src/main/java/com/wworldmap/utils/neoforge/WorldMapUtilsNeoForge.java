//? if neoforge {
/*package com.wworldmap.utils.neoforge;

import com.wworldmap.utils.policy.FeaturePolicy;
import com.wworldmap.utils.policy.PolicyConfig;
//? if <1.20.5
import com.wworldmap.utils.protocol.LegacyPolicyPacket;
//? if >=1.20.5
import com.wworldmap.utils.protocol.ModPolicyPayload;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//? if <1.20.5
//import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//? if >=1.20.5
import net.neoforged.neoforge.network.PacketDistributor;
//? if >=1.20.5
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
//? if >=1.20.5
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("wworldmap_utils")
public final class WorldMapUtilsNeoForge {
	private static final Logger LOGGER = LoggerFactory.getLogger("wworldmap_utils");
	private final FeaturePolicy policy;

	//? if >=1.20.5
	public WorldMapUtilsNeoForge(IEventBus modBus) { this(modBus, true); }
	//? if <1.20.5
	//public WorldMapUtilsNeoForge() { this(FMLJavaModLoadingContext.get().getModEventBus(), true); }

	private WorldMapUtilsNeoForge(IEventBus modBus, boolean ignored) {
		try {
			policy = PolicyConfig.load(Path.of("config").resolve(PolicyConfig.FILE_NAME), LOGGER::warn);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load wWorldMap Utils policy", exception);
		}
		//? if >=1.20.5
		modBus.addListener(this::registerPayload);
		NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
		LOGGER.info("wWorldMap Utils enabled on NeoForge; disabledFeatures={}", policy.disabledFeatureIds());
	}

	//? if >=1.20.5 {
	private void registerPayload(RegisterPayloadHandlersEvent event) {
		event.registrar("2").optional().playToClient(
			ModPolicyPayload.TYPE, ModPolicyPayload.CODEC, (payload, context) -> {});
	}
	//?}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		//? if <1.20.5
		LegacyPolicyPacket.send(player, policy.disabledMask());
		//? if >=1.20.5 {
		if (NetworkRegistry.hasChannel(player.connection, ModPolicyPayload.TYPE.id())) {
			PacketDistributor.sendToPlayer(player, new ModPolicyPayload(policy.disabledMask()));
		}
		//?}
	}
}
*///?}
