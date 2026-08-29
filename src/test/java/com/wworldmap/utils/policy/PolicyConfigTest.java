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

		FeaturePolicy policy = PolicyConfig.load(path, null);

		assertTrue(Files.isRegularFile(path));
		String generatedConfig = Files.readString(path);
		assertTrue(generatedConfig.contains("disable-entire-mod=false"));
		assertTrue(generatedConfig.contains("disabled-features="));
		for (WorldMapFeature feature : WorldMapFeature.values()) {
			assertTrue(generatedConfig.contains(feature.id()));
		}
		for (WorldMapFeature feature : WorldMapFeature.values()) {
			assertFalse(policy.disables(feature));
		}
	}

	@Test
	void parsesEntireModAndIndividualFeatureNames() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, "disable-entire-mod=true\ndisabled-features=player-radar, cave-mode\n");

		FeaturePolicy policy = PolicyConfig.load(path, null);

		assertTrue(policy.disables(WorldMapFeature.ENTIRE_MOD));
		assertTrue(policy.disables(WorldMapFeature.PLAYER_RADAR));
		assertTrue(policy.disables(WorldMapFeature.CAVE_MODE));
		assertFalse(policy.disables(WorldMapFeature.ENTITY_RADAR));
	}

	@Test
	void warnsAndIgnoresUnknownFeaturesAndInvalidBooleans() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, "disable-entire-mod=maybe\ndisabled-features=orbit,unknown\n");
		var warnings = new ArrayList<String>();

		FeaturePolicy policy = PolicyConfig.load(path, warnings::add);

		assertFalse(policy.disables(WorldMapFeature.ENTIRE_MOD));
		assertTrue(policy.disables(WorldMapFeature.ORBIT));
		assertTrue(warnings.size() == 2);
	}
}
