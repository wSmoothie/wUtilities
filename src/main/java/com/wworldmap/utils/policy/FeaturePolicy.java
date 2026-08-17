package com.wworldmap.utils.policy;

import java.util.List;

public record FeaturePolicy(int disabledMask) {
	public FeaturePolicy {
		if (disabledMask < 0) throw new IllegalArgumentException("disabledMask must be non-negative");
	}

	public boolean disables(WorldMapFeature feature) {
		return feature != null && (disabledMask & feature.mask()) != 0;
	}

	public List<String> disabledFeatureIds() {
		return java.util.Arrays.stream(WorldMapFeature.values())
			.filter(this::disables)
			.map(WorldMapFeature::id)
			.toList();
	}
}
