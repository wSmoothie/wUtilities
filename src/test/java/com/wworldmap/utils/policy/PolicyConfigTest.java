package com.wworldmap.utils.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
		assertTrue(generatedConfig.contains("sign-modifications"));
		assertFalse(generatedConfig.contains("death-waypoints"));
		assertFalse(generatedConfig.contains("chat-coordinate-capture"));
		assertFalse(generatedConfig.contains("hoplite-helpers"));
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
			disabled-wwaypoints-features=sneak_modifications, sign_modifications
			""");

		UtilityPolicy policy = PolicyConfig.load(path, null);

		assertTrue(policy.worldMap().disables(WorldMapFeature.ENTIRE_MOD));
		assertTrue(policy.worldMap().disables(WorldMapFeature.PLAYER_RADAR));
		assertTrue(policy.worldMap().disables(WorldMapFeature.CAVE_MODE));
		assertTrue(policy.waypoints().disables(WaypointsFeature.SNEAK_MODIFICATIONS));
		assertTrue(policy.waypoints().disables(WaypointsFeature.SIGN_MODIFICATIONS));
		assertFalse(policy.waypoints().disables(WaypointsFeature.ENTIRE_MOD));
		assertEquals(0x06, policy.waypoints().disabledMask());
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
		Files.writeString(path, "disable-wwaypoints=maybe\ndisabled-wwaypoints-features=sign-modifications,unknown\n");
		var warnings = new ArrayList<String>();

		UtilityPolicy policy = PolicyConfig.load(path, warnings::add);

		assertFalse(policy.waypoints().disables(WaypointsFeature.ENTIRE_MOD));
		assertTrue(policy.waypoints().disables(WaypointsFeature.SIGN_MODIFICATIONS));
		assertTrue(warnings.size() == 2);
	}

	@Test
	void unsupportedFeatureIdsCannotProduceRestrictions() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, "disabled-wwaypoints-features=death-waypoints,chat_coordinate_capture,hoplite-helpers\n");
		var warnings = new ArrayList<String>();

		UtilityPolicy policy = PolicyConfig.load(path, warnings::add);

		assertEquals(0, policy.waypoints().disabledMask());
		assertEquals(3, warnings.size());
		assertEquals(3, WaypointsFeature.values().length);
	}

	@Test
	void wholeModAndSignPolicyUseCompactWireAssignments() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, "disable-wwaypoints=true\ndisabled-wwaypoints-features=sign-modifications\n");
		assertEquals(0x05, PolicyConfig.load(path, null).waypoints().disabledMask());
		Files.writeString(path, "disabled-wwaypoints-features=entire-mod\n");
		assertEquals(0x01, PolicyConfig.load(path, null).waypoints().disabledMask());
	}
}
