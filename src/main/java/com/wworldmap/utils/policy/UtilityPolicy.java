package com.wworldmap.utils.policy;

public record UtilityPolicy(FeaturePolicy worldMap, WaypointsPolicy waypoints) {
	public UtilityPolicy {
		if (worldMap == null) throw new IllegalArgumentException("worldMap must not be null");
		if (waypoints == null) throw new IllegalArgumentException("waypoints must not be null");
	}
}
