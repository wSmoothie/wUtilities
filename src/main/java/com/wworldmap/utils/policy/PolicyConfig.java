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
	public static final String FILE_NAME = "wutilities.properties";
	public static final String LEGACY_FILE_NAME = "wworldmap-utils.properties";
	private static final String DEFAULT_TEXT = """
		# wUtilities server policy
		#
		# These settings restrict client features while players are connected
		# to this server. Restart the server after changing this file.

		# Disable every wWorldMap feature.
		disable-wworldmap=false

		# Comma-separated wWorldMap feature IDs to disable.
		# Available: entire-mod, player-radar, entity-radar, orbit, cave-mode
		disabled-wworldmap-features=

		# Disable every wWaypoints feature.
		disable-wwaypoints=false

		# Comma-separated wWaypoints feature IDs to disable.
		# entire-mod              - Disable all wWaypoints behavior.
		# sneak-modifications     - Disable Toggle Sneak and all input/interaction changes.
		# death-waypoints         - Disable automatic death-waypoint creation.
		# chat-coordinate-capture - Disable clickable/captured chat coordinates.
		# hoplite-helpers         - Disable supply-drop and auto-pick automation.
		disabled-wwaypoints-features=
		""";

	private PolicyConfig() {}

	public static UtilityPolicy load(Path path, Consumer<String> warningSink) throws IOException {
		return load(path, null, warningSink);
	}

	public static UtilityPolicy load(Path path, Path legacyPath, Consumer<String> warningSink) throws IOException {
		if (path == null) throw new IllegalArgumentException("path must not be null");
		Consumer<String> warnings = warningSink != null ? warningSink : ignored -> {};
		if (!Files.exists(path)) {
			Path parent = path.getParent();
			if (parent != null) Files.createDirectories(parent);
			if (legacyPath != null && Files.isRegularFile(legacyPath)) {
				Files.copy(legacyPath, path);
				warnings.accept("Migrated legacy policy configuration to " + path.getFileName());
			} else {
				Files.writeString(path, DEFAULT_TEXT, StandardCharsets.UTF_8);
			}
		}

		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}

		int worldMapMask = parseBoolean(first(properties, "disable-wworldmap", "disable-entire-mod"),
			false, "disable-wworldmap", warnings) ? WorldMapFeature.ENTIRE_MOD.mask() : 0;
		String worldMapFeatures = first(properties, "disabled-wworldmap-features", "disabled-features");
		for (String rawId : valueOrEmpty(worldMapFeatures).split(",")) {
			String id = normalize(rawId);
			if (id.isEmpty()) continue;
			var feature = WorldMapFeature.fromId(id);
			if (feature.isPresent()) worldMapMask |= feature.get().mask();
			else warnings.accept("Unknown disabled wWorldMap feature ID: " + id);
		}

		int waypointsMask = parseBoolean(properties.getProperty("disable-wwaypoints"),
			false, "disable-wwaypoints", warnings) ? WaypointsFeature.ENTIRE_MOD.mask() : 0;
		for (String rawId : valueOrEmpty(properties.getProperty("disabled-wwaypoints-features")).split(",")) {
			String id = normalize(rawId);
			if (id.isEmpty()) continue;
			var feature = WaypointsFeature.fromId(id);
			if (feature.isPresent()) waypointsMask |= feature.get().mask();
			else warnings.accept("Unknown disabled wWaypoints feature ID: " + id);
		}

		return new UtilityPolicy(new FeaturePolicy(worldMapMask), new WaypointsPolicy(waypointsMask));
	}

	private static String first(Properties properties, String preferred, String legacy) {
		return properties.containsKey(preferred) ? properties.getProperty(preferred) : properties.getProperty(legacy);
	}

	private static String valueOrEmpty(String value) {
		return value != null ? value : "";
	}

	private static String normalize(String rawId) {
		return rawId.trim().toLowerCase(Locale.ROOT).replace('_', '-');
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
