//? if >=1.20.5 {
package com.wworldmap.utils.protocol;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11
import net.minecraft.resources.Identifier;
//? if <1.21.11
//import net.minecraft.resources.ResourceLocation;

public record ModPolicyPayload(int disabledMask) implements CustomPacketPayload {
	public static final Type<ModPolicyPayload> TYPE = new Type<>(channel());
	public static final StreamCodec<RegistryFriendlyByteBuf, ModPolicyPayload> CODEC = new StreamCodec<>() {
		@Override
		public ModPolicyPayload decode(RegistryFriendlyByteBuf buffer) {
			int version = buffer.readUnsignedByte();
			int mask = buffer.readVarInt();
			return new ModPolicyPayload(version == PolicyProtocol.VERSION && mask >= 0 ? mask : 0);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, ModPolicyPayload payload) {
			buffer.writeByte(PolicyProtocol.VERSION);
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
		return Identifier.fromNamespaceAndPath("wworldmap", "policy");
		//? if >=1.21 && <1.21.11
		//return ResourceLocation.fromNamespaceAndPath("wworldmap", "policy");
		//? if <1.21
		//return new ResourceLocation("wworldmap", "policy");
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
//?}
