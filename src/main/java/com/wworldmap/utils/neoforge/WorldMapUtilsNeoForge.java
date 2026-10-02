//? if neoforge {
/*package com.wworldmap.utils.neoforge;

import com.wworldmap.utils.policy.PolicyConfig;
import com.wworldmap.utils.policy.UtilityPolicy;
//? if <1.20.5
import com.wworldmap.utils.protocol.LegacyPolicyPacket;
//? if >=1.20.5
import com.wworldmap.utils.protocol.ModPolicyPayload;
//? if >=1.20.5
import com.wworldmap.utils.protocol.PolicyProtocol;
//? if >=1.20.5
import com.wworldmap.utils.protocol.WaypointsPolicyPayload;
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

@Mod("wutilities")
public final class WorldMapUtilsNeoForge {
	private static final Logger LOGGER = LoggerFactory.getLogger("wutilities");
	private final UtilityPolicy policy;

	//? if >=1.20.5
	public WorldMapUtilsNeoForge(IEventBus modBus) { this(modBus, true); }
	//? if <1.20.5
	//public WorldMapUtilsNeoForge() { this(FMLJavaModLoadingContext.get().getModEventBus(), true); }

	private WorldMapUtilsNeoForge(IEventBus modBus, boolean ignored) {
		try {
			Path configDirectory = Path.of("config");
			policy = PolicyConfig.load(configDirectory.resolve(PolicyConfig.FILE_NAME),
				configDirectory.resolve(PolicyConfig.LEGACY_FILE_NAME), LOGGER::warn);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load wUtilities policy", exception);
		}
		//? if >=1.20.5
		modBus.addListener(this::registerPayload);
		NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
		LOGGER.info("wUtilities enabled on NeoForge; disabledWorldMapFeatures={}; disabledWaypointsFeatures={}",
			policy.worldMap().disabledFeatureIds(), policy.waypoints().disabledFeatureIds());
	}

	//? if >=1.20.5 {
	private void registerPayload(RegisterPayloadHandlersEvent event) {
		event.registrar(Integer.toString(PolicyProtocol.WORLD_MAP_VERSION)).optional().playToClient(
			ModPolicyPayload.TYPE, ModPolicyPayload.CODEC, (payload, context) -> {});
		event.registrar(Integer.toString(PolicyProtocol.WAYPOINTS_VERSION)).optional().playToClient(
			WaypointsPolicyPayload.TYPE, WaypointsPolicyPayload.CODEC, (payload, context) -> {});
	}
	//?}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		//? if <1.20.5
		LegacyPolicyPacket.sendWorldMap(player, policy.worldMap().disabledMask());
		//? if <1.20.5
		LegacyPolicyPacket.sendWaypoints(player, policy.waypoints().disabledMask());
		//? if >=1.20.5 {
		if (NetworkRegistry.hasChannel(player.connection, ModPolicyPayload.TYPE.id())) {
			PacketDistributor.sendToPlayer(player, new ModPolicyPayload(policy.worldMap().disabledMask()));
		}
		if (NetworkRegistry.hasChannel(player.connection, WaypointsPolicyPayload.TYPE.id())) {
			PacketDistributor.sendToPlayer(player, new WaypointsPolicyPayload(policy.waypoints().disabledMask()));
		}
		//?}
	}
}
*///?}
