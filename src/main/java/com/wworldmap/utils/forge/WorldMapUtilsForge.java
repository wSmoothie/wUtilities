//? if forge {
/*package com.wworldmap.utils.forge;

import com.wworldmap.utils.policy.PolicyConfig;
import com.wworldmap.utils.policy.UtilityPolicy;
//? if <1.20.5
import com.wworldmap.utils.protocol.LegacyPolicyPacket;
//? if >=1.20.5
import com.wworldmap.utils.protocol.ModPolicyPayload;
//? if >=1.20.5
import com.wworldmap.utils.protocol.WaypointsPolicyPayload;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;
//? if <1.21.11
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
//? if >=1.20.5
import net.minecraftforge.network.Channel;
//? if >=1.20.5
import net.minecraftforge.network.ChannelBuilder;
//? if >=1.20.5
import net.minecraft.network.protocol.PacketFlow;
//? if >=1.20.5
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.20.5
import net.minecraftforge.network.NetworkProtocol;
//? if <1.21.11
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("wutilities")
public final class WorldMapUtilsForge {
	private static final Logger LOGGER = LoggerFactory.getLogger("wutilities");
	private final UtilityPolicy policy;
	//? if >=1.20.5
	private final Channel<CustomPacketPayload> channel;
	//? if >=1.20.5
	private final Channel<CustomPacketPayload> waypointsChannel;

	//? if <1.20.5 {
	/^public WorldMapUtilsForge() {
		this(FMLJavaModLoadingContext.get().getModEventBus());
	}
	^///?} else {
	public WorldMapUtilsForge(FMLJavaModLoadingContext context) {
		this(
			//? if <1.21.11
			context.getModEventBus()
		);
	}
	//?}

	private WorldMapUtilsForge(
		//? if <1.21.11
		IEventBus ignored
	) {
		try {
			Path configDirectory = Path.of("config");
			policy = PolicyConfig.load(configDirectory.resolve(PolicyConfig.FILE_NAME),
				configDirectory.resolve(PolicyConfig.LEGACY_FILE_NAME), LOGGER::warn);
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load wUtilities policy", exception);
		}
		//? if >=1.20.5 {
		channel = ChannelBuilder.named(ModPolicyPayload.TYPE.id())
			.networkProtocolVersion(2).optional().payloadChannel()
			.protocol(NetworkProtocol.PLAY).flow(PacketFlow.CLIENTBOUND)
			.add(ModPolicyPayload.TYPE, ModPolicyPayload.CODEC, (payload, context) -> {})
			.build();
		waypointsChannel = ChannelBuilder.named(WaypointsPolicyPayload.TYPE.id())
			.networkProtocolVersion(1).optional().payloadChannel()
			.protocol(NetworkProtocol.PLAY).flow(PacketFlow.CLIENTBOUND)
			.add(WaypointsPolicyPayload.TYPE, WaypointsPolicyPayload.CODEC, (payload, context) -> {})
			.build();
		//?}
		//? if <1.21.11
		MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
		//? if >=1.21.11
		//PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::onPlayerLoggedIn);
		LOGGER.info("wUtilities enabled on Forge; disabledWorldMapFeatures={}; disabledWaypointsFeatures={}",
			policy.worldMap().disabledFeatureIds(), policy.waypoints().disabledFeatureIds());
	}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		//? if <1.20.5
		LegacyPolicyPacket.sendWorldMap(player, policy.worldMap().disabledMask());
		//? if <1.20.5
		LegacyPolicyPacket.sendWaypoints(player, policy.waypoints().disabledMask());
		//? if >=1.20.5 {
		if (channel.isRemotePresent(player.connection.getConnection())) {
			channel.send(new ModPolicyPayload(policy.worldMap().disabledMask()), player.connection.getConnection());
		}
		if (waypointsChannel.isRemotePresent(player.connection.getConnection())) {
			waypointsChannel.send(new WaypointsPolicyPayload(policy.waypoints().disabledMask()),
				player.connection.getConnection());
		}
		//?}
	}
}
*///?}
