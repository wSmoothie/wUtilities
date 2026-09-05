//? if <1.20.5 {
/*package com.wworldmap.utils.protocol;

//? if <1.20.2
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
//? if <1.20.2
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
//? if >=1.20.2
//import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
//? if >=1.20.2
//import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class LegacyPolicyPacket {
	private static final ResourceLocation WORLD_MAP_CHANNEL = new ResourceLocation("wworldmap", "policy");
	private static final ResourceLocation WAYPOINTS_CHANNEL = new ResourceLocation("wwaypoints", "policy");

	private LegacyPolicyPacket() {}

	public static void sendWorldMap(ServerPlayer player, int disabledMask) {
		send(player, WORLD_MAP_CHANNEL, PolicyProtocol.WORLD_MAP_VERSION, disabledMask);
	}

	public static void sendWaypoints(ServerPlayer player, int disabledMask) {
		send(player, WAYPOINTS_CHANNEL, PolicyProtocol.WAYPOINTS_VERSION, disabledMask);
	}

	private static void send(ServerPlayer player, ResourceLocation channel, int version, int disabledMask) {
		if (player == null) return;
		//? if <1.20.2 {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		buffer.writeByte(version);
		buffer.writeVarInt(disabledMask);
		player.connection.send(new ClientboundCustomPayloadPacket(channel, buffer));
		//?} else {
		//player.connection.send(new ClientboundCustomPayloadPacket(new Payload(channel, version, disabledMask)));
		//?}
	}

	//? if >=1.20.2 {
	/^private record Payload(ResourceLocation channel, int version, int disabledMask) implements CustomPacketPayload {
		@Override public void write(FriendlyByteBuf buffer) {
			buffer.writeByte(version);
			buffer.writeVarInt(disabledMask);
		}
		@Override public ResourceLocation id() { return channel; }
	}
	^///?}
}
*///?}
