package com.wworldmap.utils.policy;

import java.util.List;

public record WaypointsPolicy(int disabledMask) {
	public WaypointsPolicy {
		if (disabledMask < 0) throw new IllegalArgumentException("disabledMask must be non-negative");
	}

	public boolean disables(WaypointsFeature feature) {
		return feature != null && (disabledMask & feature.mask()) != 0;
	}

	public List<String> disabledFeatureIds() {
		return java.util.Arrays.stream(WaypointsFeature.values())
			.filter(this::disables)
			.map(WaypointsFeature::id)
			.toList();
	}
}
