package com.wworldmap.utils.protocol;

import java.io.ByteArrayOutputStream;

public final class PolicyProtocol {
	public static final String WORLD_MAP_CHANNEL = "wworldmap:policy";
	public static final int WORLD_MAP_VERSION = 1;
	public static final String WAYPOINTS_CHANNEL = "wwaypoints:policy";
	public static final int WAYPOINTS_VERSION = 1;
	/** Legacy aliases retained for integrations compiled against the original API. */
	public static final String CHANNEL = WORLD_MAP_CHANNEL;
	public static final int VERSION = WORLD_MAP_VERSION;

	private PolicyProtocol() {}

	public static byte[] encode(int disabledMask) {
		return encodeWorldMap(disabledMask);
	}

	public static byte[] encodeWorldMap(int disabledMask) {
		return encode(WORLD_MAP_VERSION, disabledMask);
	}

	public static byte[] encodeWaypoints(int disabledMask) {
		return encode(WAYPOINTS_VERSION, disabledMask);
	}

	private static byte[] encode(int version, int disabledMask) {
		if (disabledMask < 0) throw new IllegalArgumentException("disabledMask must be non-negative");
		ByteArrayOutputStream output = new ByteArrayOutputStream(6);
		output.write(version);
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
