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
		for (WorldMapFeature feature : WorldMapFeature.values()) {
			assertFalse(policy.disables(feature));
		}
	}

	@Test
	void parsesEntireModAndIndividualFeatureNames() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, "disable-entire-mod=true\ndisabled-features=minimap, cave-view\n");

		FeaturePolicy policy = PolicyConfig.load(path, null);

		assertTrue(policy.disables(WorldMapFeature.ENTIRE_MOD));
		assertTrue(policy.disables(WorldMapFeature.MINIMAP));
		assertTrue(policy.disables(WorldMapFeature.CAVE_VIEW));
		assertFalse(policy.disables(WorldMapFeature.ENTITY_RADAR));
	}

	@Test
	void warnsAndIgnoresUnknownFeaturesAndInvalidBooleans() throws Exception {
		Path path = temporaryDirectory.resolve(PolicyConfig.FILE_NAME);
		Files.writeString(path, "disable-entire-mod=maybe\ndisabled-features=minimap,unknown\n");
		var warnings = new ArrayList<String>();

		FeaturePolicy policy = PolicyConfig.load(path, warnings::add);

		assertFalse(policy.disables(WorldMapFeature.ENTIRE_MOD));
		assertTrue(policy.disables(WorldMapFeature.MINIMAP));
		assertTrue(warnings.size() == 2);
	}
}
