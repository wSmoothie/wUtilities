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
	private static final ResourceLocation CHANNEL = new ResourceLocation("wworldmap", "policy");

	private LegacyPolicyPacket() {}

	public static void send(ServerPlayer player, int disabledMask) {
		if (player == null) return;
		//? if <1.20.2 {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		buffer.writeByte(PolicyProtocol.VERSION);
		buffer.writeVarInt(disabledMask);
		player.connection.send(new ClientboundCustomPayloadPacket(CHANNEL, buffer));
		//?} else {
		//player.connection.send(new ClientboundCustomPayloadPacket(new Payload(disabledMask)));
		//?}
	}

	//? if >=1.20.2 {
	/^private record Payload(int disabledMask) implements CustomPacketPayload {
		@Override public void write(FriendlyByteBuf buffer) {
			buffer.writeByte(PolicyProtocol.VERSION);
			buffer.writeVarInt(disabledMask);
		}
		@Override public ResourceLocation id() { return CHANNEL; }
	}
	^///?}
}
*///?}
