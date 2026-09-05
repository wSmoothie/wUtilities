//? if >=1.20.5 {
package com.wworldmap.utils.protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11
import net.minecraft.resources.Identifier;
//? if <1.21.11
//import net.minecraft.resources.ResourceLocation;

public record WaypointsPolicyPayload(int disabledMask) implements CustomPacketPayload {
	public static final Type<WaypointsPolicyPayload> TYPE = new Type<>(channel());
	public static final StreamCodec<RegistryFriendlyByteBuf, WaypointsPolicyPayload> CODEC = new StreamCodec<>() {
		@Override
		public WaypointsPolicyPayload decode(RegistryFriendlyByteBuf buffer) {
			int version = buffer.readUnsignedByte();
			int mask = buffer.readVarInt();
			return new WaypointsPolicyPayload(version == PolicyProtocol.WAYPOINTS_VERSION && mask >= 0 ? mask : 0);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, WaypointsPolicyPayload payload) {
			buffer.writeByte(PolicyProtocol.WAYPOINTS_VERSION);
			buffer.writeVarInt(payload.disabledMask);
		}
	};

	private static
	//? if >=1.21.11
	Identifier
	//? if <1.21.11
	//ResourceLocation
	channel() {
		//? if >=1.21.11
		return Identifier.fromNamespaceAndPath("wwaypoints", "policy");
		//? if >=1.21 && <1.21.11
		//return ResourceLocation.fromNamespaceAndPath("wwaypoints", "policy");
		//? if <1.21
		//return new ResourceLocation("wwaypoints", "policy");
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
//?}
