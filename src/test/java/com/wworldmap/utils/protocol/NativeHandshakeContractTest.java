package com.wworldmap.utils.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Native loader negotiation must advertise the same version as the wire codec. */
final class NativeHandshakeContractTest {
	@Test
	void forgeUsesEachWireVersionForChannelNegotiation() throws Exception {
		String adapter = source("forge/WorldMapUtilsForge.java");
		assertTrue(adapter.contains("networkProtocolVersion(PolicyProtocol.WORLD_MAP_VERSION)"));
		assertTrue(adapter.contains("networkProtocolVersion(PolicyProtocol.WAYPOINTS_VERSION)"));
	}

	@Test
	void neoForgeUsesEachWireVersionForPayloadNegotiation() throws Exception {
		String adapter = source("neoforge/WorldMapUtilsNeoForge.java");
		assertTrue(adapter.contains("registrar(Integer.toString(PolicyProtocol.WORLD_MAP_VERSION))"));
		assertTrue(adapter.contains("registrar(Integer.toString(PolicyProtocol.WAYPOINTS_VERSION))"));
	}

	private static String source(String relativePath) throws Exception {
		return Files.readString(Path.of("src/main/java/com/wworldmap/utils", relativePath));
	}
}
