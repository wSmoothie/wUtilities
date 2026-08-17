package com.wworldmap.utils.protocol;

import java.io.ByteArrayOutputStream;

public final class PolicyProtocol {
	public static final String CHANNEL = "wworldmap:policy";
	public static final int VERSION = 1;

	private PolicyProtocol() {}

	public static byte[] encode(int disabledMask) {
		if (disabledMask < 0) throw new IllegalArgumentException("disabledMask must be non-negative");
		ByteArrayOutputStream output = new ByteArrayOutputStream(6);
		output.write(VERSION);
		writeVarInt(output, disabledMask);
		return output.toByteArray();
	}

	private static void writeVarInt(ByteArrayOutputStream output, int value) {
		int remaining = value;
		do {
			int next = remaining & 0x7F;
			remaining >>>= 7;
			if (remaining != 0) next |= 0x80;
			output.write(next);
		} while (remaining != 0);
	}
}
