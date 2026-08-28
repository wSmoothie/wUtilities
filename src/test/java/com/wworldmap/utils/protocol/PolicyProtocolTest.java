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
}
