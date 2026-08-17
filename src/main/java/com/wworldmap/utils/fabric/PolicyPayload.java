package com.wworldmap.utils.fabric;

import com.wworldmap.utils.protocol.PolicyProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PolicyPayload(int disabledMask) implements CustomPacketPayload {
	public static final Type<PolicyPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath("wworldmap", "policy"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PolicyPayload> CODEC = new StreamCodec<>() {
		@Override
		public PolicyPayload decode(RegistryFriendlyByteBuf buffer) {
			int version = buffer.readUnsignedByte();
			int mask = buffer.readVarInt();
			if (version != PolicyProtocol.VERSION || mask < 0) return new PolicyPayload(0);
			return new PolicyPayload(mask);
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, PolicyPayload payload) {
			buffer.writeByte(PolicyProtocol.VERSION);
			buffer.writeVarInt(payload.disabledMask);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
