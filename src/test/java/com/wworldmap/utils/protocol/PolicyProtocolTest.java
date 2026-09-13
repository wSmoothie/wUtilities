package com.wworldmap.utils.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

final class PolicyProtocolTest {
	@Test
	void encodesVersionAndSingleByteMask() {
		assertArrayEquals(new byte[] {1, 0x15}, PolicyProtocol.encode(0x15));
	}

	@Test
	void encodesMinecraftUnsignedVarInts() {
		assertArrayEquals(new byte[] {1, (byte)0xAC, 0x02}, PolicyProtocol.encode(300));
	}

	@Test
	void encodesBothIndependentPoliciesAsVersionOne() {
		assertArrayEquals(new byte[] {1, 0x06}, PolicyProtocol.encodeWorldMap(0x06));
		assertArrayEquals(new byte[] {1, 0x06}, PolicyProtocol.encodeWaypoints(0x06));
	}
}
