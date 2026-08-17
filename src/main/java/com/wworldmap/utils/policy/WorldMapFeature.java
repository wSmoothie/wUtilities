package com.wworldmap.utils.policy;

import java.util.Arrays;
import java.util.Optional;

public enum WorldMapFeature {
	ENTIRE_MOD(0, "entire-mod"),
	WORLD_MAP(1, "world-map"),
	MINIMAP(2, "minimap"),
	CAVE_VIEW(3, "cave-view"),
	ENTITY_RADAR(4, "entity-radar"),
	WAYPOINT_INTEGRATION(5, "waypoint-integration"),
	DETACHED_WINDOW(6, "detached-window");

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
