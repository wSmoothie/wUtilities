package com.wworldmap.utils.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PolicyConfigTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void createsAnUnrestrictedDefaultConfiguration() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);

		UtilityPolicy policy = PolicyConfig.load(path, null);

		assertTrue(Files.isRegularFile(path));
		String generatedConfig = Files.readString(path);
		assertTrue(generatedConfig.contains("disable-wworldmap=false"));
		assertTrue(generatedConfig.contains("disabled-wwaypoints-features="));
		for (WorldMapFeature feature : WorldMapFeature.values()) assertFalse(policy.worldMap().disables(feature));
		for (WaypointsFeature feature : WaypointsFeature.values()) assertFalse(policy.waypoints().disables(feature));
	}

	@Test
	void parsesBothPolicyMasksAndUnderscoreAliases() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, """
			disable-wworldmap=true
			disabled-wworldmap-features=player-radar, cave-mode
			disable-wwaypoints=false
			disabled-wwaypoints-features=sneak_modifications, hoplite-helpers
			""");

		UtilityPolicy policy = PolicyConfig.load(path, null);

		assertTrue(policy.worldMap().disables(WorldMapFeature.ENTIRE_MOD));
		assertTrue(policy.worldMap().disables(WorldMapFeature.PLAYER_RADAR));
		assertTrue(policy.worldMap().disables(WorldMapFeature.CAVE_MODE));
		assertTrue(policy.waypoints().disables(WaypointsFeature.SNEAK_MODIFICATIONS));
		assertTrue(policy.waypoints().disables(WaypointsFeature.HOPLITE_HELPERS));
		assertFalse(policy.waypoints().disables(WaypointsFeature.DEATH_WAYPOINTS));
	}

	@Test
	void migratesLegacyFileAndKeysWithoutChangingWorldMapPolicy() throws Exception {
		Path current = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Path legacy = temporaryDirectory.resolve(PolicyConfig.LEGACY_FILE_NAME);
		Files.writeString(legacy, "disable-entire-mod=true\ndisabled-features=entity-radar\n");
		var warnings = new ArrayList<String>();

		UtilityPolicy policy = PolicyConfig.load(current, legacy, warnings::add);

		assertTrue(Files.isRegularFile(current));
		assertTrue(policy.worldMap().disables(WorldMapFeature.ENTIRE_MOD));
		assertTrue(policy.worldMap().disables(WorldMapFeature.ENTITY_RADAR));
		assertTrue(warnings.stream().anyMatch(message -> message.contains("Migrated legacy")));
	}

	@Test
	void warnsAndIgnoresUnknownFeaturesAndInvalidBooleans() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, "disable-wwaypoints=maybe\ndisabled-wwaypoints-features=death-waypoints,unknown\n");
		var warnings = new ArrayList<String>();

		UtilityPolicy policy = PolicyConfig.load(path, warnings::add);

		assertFalse(policy.waypoints().disables(WaypointsFeature.ENTIRE_MOD));
		assertTrue(policy.waypoints().disables(WaypointsFeature.DEATH_WAYPOINTS));
		assertTrue(warnings.size() == 2);
	}
}
