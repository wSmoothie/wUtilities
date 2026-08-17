package com.wworldmap.utils.paper;

import com.wworldmap.utils.policy.FeaturePolicy;
import com.wworldmap.utils.policy.PolicyConfig;
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
	private FeaturePolicy policy;

	@Override
	public void onEnable() {
		try {
			policy = PolicyConfig.load(getDataFolder().toPath().resolve(PolicyConfig.FILE_NAME),
				message -> getLogger().warning(message));
		} catch (IOException exception) {
			getLogger().severe("Could not load policy: " + exception.getMessage());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		getServer().getMessenger().registerOutgoingPluginChannel(this, PolicyProtocol.CHANNEL);
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("Enabled on " + getServer().getName()
			+ "; disabledFeatures=" + policy.disabledFeatureIds());
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this, PolicyProtocol.CHANNEL);
		HandlerList.unregisterAll((Listener)this);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		sendPolicy(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChannelRegistered(PlayerRegisterChannelEvent event) {
		if (PolicyProtocol.CHANNEL.equals(event.getChannel())) {
			sendPolicy(event.getPlayer());
		}
	}

	private void sendPolicy(org.bukkit.entity.Player player) {
		FeaturePolicy currentPolicy = policy;
		if (player == null || currentPolicy == null || !player.isConnected()) return;
		player.sendPluginMessage(this, PolicyProtocol.CHANNEL,
			PolicyProtocol.encode(currentPolicy.disabledMask()));
	}
}
