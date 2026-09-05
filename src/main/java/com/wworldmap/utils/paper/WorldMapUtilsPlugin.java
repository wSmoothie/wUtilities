package com.wworldmap.utils.paper;

import com.wworldmap.utils.policy.PolicyConfig;
import com.wworldmap.utils.policy.UtilityPolicy;
import com.wworldmap.utils.protocol.PolicyProtocol;
import java.io.IOException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldMapUtilsPlugin extends JavaPlugin implements Listener {
	private UtilityPolicy policy;

	@Override
	public void onEnable() {
		try {
			policy = PolicyConfig.load(getDataFolder().toPath().resolve(PolicyConfig.FILE_NAME),
				legacyConfigPath(),
				message -> getLogger().warning(message));
		} catch (IOException exception) {
			getLogger().severe("Could not load policy: " + exception.getMessage());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		getServer().getMessenger().registerOutgoingPluginChannel(this, PolicyProtocol.WORLD_MAP_CHANNEL);
		getServer().getMessenger().registerOutgoingPluginChannel(this, PolicyProtocol.WAYPOINTS_CHANNEL);
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("Enabled on " + getServer().getName()
			+ "; disabledWorldMapFeatures=" + policy.worldMap().disabledFeatureIds()
			+ "; disabledWaypointsFeatures=" + policy.waypoints().disabledFeatureIds());
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this, PolicyProtocol.WORLD_MAP_CHANNEL);
		getServer().getMessenger().unregisterOutgoingPluginChannel(this, PolicyProtocol.WAYPOINTS_CHANNEL);
		HandlerList.unregisterAll((Listener)this);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		sendPolicy(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChannelRegistered(PlayerRegisterChannelEvent event) {
		if (PolicyProtocol.WORLD_MAP_CHANNEL.equals(event.getChannel())
			|| PolicyProtocol.WAYPOINTS_CHANNEL.equals(event.getChannel())) {
			sendPolicy(event.getPlayer());
		}
	}

	private void sendPolicy(org.bukkit.entity.Player player) {
		UtilityPolicy currentPolicy = policy;
		if (player == null || currentPolicy == null || !player.isOnline()) return;
		player.sendPluginMessage(this, PolicyProtocol.WORLD_MAP_CHANNEL,
			PolicyProtocol.encodeWorldMap(currentPolicy.worldMap().disabledMask()));
		player.sendPluginMessage(this, PolicyProtocol.WAYPOINTS_CHANNEL,
			PolicyProtocol.encodeWaypoints(currentPolicy.waypoints().disabledMask()));
	}

	private java.nio.file.Path legacyConfigPath() {
		java.io.File pluginsDirectory = getDataFolder().getParentFile();
		if (pluginsDirectory == null) return null;
		return pluginsDirectory.toPath().resolve("wWorldMapUtils").resolve(PolicyConfig.LEGACY_FILE_NAME);
	}
}
