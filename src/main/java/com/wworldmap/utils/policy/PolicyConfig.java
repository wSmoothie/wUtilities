package com.wworldmap.utils.policy;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Consumer;

public final class PolicyConfig {
	public static final String FILE_NAME = "wworldmap-utils.properties";
	private static final String DEFAULT_TEXT = """
		# wWorldMap Utils server policy
		#
		# These settings restrict wWorldMap features while players are connected
		# to this server. Restart the server after changing this file.

		# Disable every wWorldMap feature.
		# true  = completely disable wWorldMap
		# false = allow wWorldMap, except features listed below
		disable-entire-mod=false

		# Comma-separated feature IDs to disable.
		# Leave empty to allow every feature.
		#
		# Available feature IDs:
		# entire-mod   - Disable all wWorldMap behavior.
		# player-radar - Hide other players from the map.
		#                 The local player's marker remains visible.
		# entity-radar - Hide mobs and other non-player entities.
		# orbit        - Disable orbit views and force maps into top-down view.
		# cave-mode    - Disable cave mode and level-cut functionality.
		#
		# Example:
		# disabled-features=player-radar,entity-radar,orbit
		disabled-features=
		""";

	private PolicyConfig() {}

	public static FeaturePolicy load(Path path, Consumer<String> warningSink) throws IOException {
		if (path == null) throw new IllegalArgumentException("path must not be null");
		Consumer<String> warnings = warningSink != null ? warningSink : ignored -> {};
		if (!Files.exists(path)) {
			Path parent = path.getParent();
			if (parent != null) Files.createDirectories(parent);
			Files.writeString(path, DEFAULT_TEXT, StandardCharsets.UTF_8);
		}

		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}

		int disabledMask = parseBoolean(properties.getProperty("disable-entire-mod"), false,
			"disable-entire-mod", warnings) ? WorldMapFeature.ENTIRE_MOD.mask() : 0;
		String disabledFeatures = properties.getProperty("disabled-features", "");
		for (String rawId : disabledFeatures.split(",")) {
			String id = rawId.trim().toLowerCase(Locale.ROOT);
			if (id.isEmpty()) continue;
			var feature = WorldMapFeature.fromId(id);
			if (feature.isPresent()) {
				disabledMask |= feature.get().mask();
			} else {
				warnings.accept("Unknown disabled feature ID: " + id);
			}
		}
		return new FeaturePolicy(disabledMask);
	}

	private static boolean parseBoolean(String raw, boolean fallback, String key,
		Consumer<String> warnings) {
		if (raw == null || raw.isBlank()) return fallback;
		if (raw.equalsIgnoreCase("true")) return true;
		if (raw.equalsIgnoreCase("false")) return false;
		warnings.accept("Invalid boolean for " + key + ": " + raw + "; using " + fallback);
		return fallback;
	}
}
