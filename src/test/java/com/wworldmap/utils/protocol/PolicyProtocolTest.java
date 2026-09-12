package com.wworldmap.utils.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

final class PolicyProtocolTest {
	@Test
	void encodesVersionAndSingleByteMask() {
		assertArrayEquals(new byte[] {2, 0x15}, PolicyProtocol.encode(0x15));
	}

	@Test
	void encodesMinecraftUnsignedVarInts() {
		assertArrayEquals(new byte[] {2, (byte)0xAC, 0x02}, PolicyProtocol.encode(300));
	}

	@Test
	void preservesWorldMapV2AndAddsIndependentWaypointsV1() {
		assertArrayEquals(new byte[] {2, 0x06}, PolicyProtocol.encodeWorldMap(0x06));
		assertArrayEquals(new byte[] {1, 0x22}, PolicyProtocol.encodeWaypoints(0x22));
	}
}
