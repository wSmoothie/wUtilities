package com.wworldmap.utils.policy;

import java.util.Arrays;
import java.util.Optional;

public enum WorldMapFeature {
	ENTIRE_MOD(0, "entire-mod"),
	PLAYER_RADAR(1, "player-radar"),
	ENTITY_RADAR(2, "entity-radar"),
	ORBIT(3, "orbit"),
	CAVE_MODE(4, "cave-mode");

	private final int mask;
	private final String id;

	WorldMapFeature(int bit, String id) {
		this.mask = 1 << bit;
		this.id = id;
	}

	public int mask() {
		return mask;
	}

	public String id() {
		return id;
	}

	public static Optional<WorldMapFeature> fromId(String id) {
		if (id == null) return Optional.empty();
		return Arrays.stream(values()).filter(feature -> feature.id.equals(id)).findFirst();
	}
}
