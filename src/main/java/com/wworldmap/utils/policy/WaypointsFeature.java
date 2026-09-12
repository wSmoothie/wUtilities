package com.wworldmap.utils.policy;

import java.util.Arrays;
import java.util.Optional;

/** Stable feature bits used by the {@code wwaypoints:policy} protocol. */
public enum WaypointsFeature {
	ENTIRE_MOD(0, "entire-mod"),
	SNEAK_MODIFICATIONS(1, "sneak-modifications"),
	// Bits 2-4 are retired; never reuse them for a different restriction.
	SIGN_MODIFICATIONS(5, "sign-modifications");

	private final int mask;
	private final String id;

	WaypointsFeature(int bit, String id) {
		this.mask = 1 << bit;
		this.id = id;
	}

	public int mask() {
		return mask;
	}

	public String id() {
		return id;
	}

	public static Optional<WaypointsFeature> fromId(String id) {
		if (id == null) return Optional.empty();
		String normalized = id.replace('_', '-');
		return Arrays.stream(values()).filter(feature -> feature.id.equals(normalized)).findFirst();
	}
}
