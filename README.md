# wWorldMap Utils

wWorldMap Utils lets a Minecraft server disable all of wWorldMap or selected client features. One JAR detects its loader through isolated entrypoints and runs as either:

- a Fabric server mod;
- a Bukkit plugin on Paper, Folia, Purpur, or Leaf.

The current release supports Minecraft 1.21.11 and Java 21. Clients need a wWorldMap build that implements the `wworldmap:policy` channel. Players without wWorldMap can join normally.

## Build

```powershell
.\gradlew.bat clean test remapJar
```

The deployable artifact is `build/libs/wWorldMapUtils-<version>.jar` (not the sources JAR).

## Install

- Fabric: put the JAR and Fabric API in the server's `mods` directory.
- Paper/Folia/Purpur/Leaf: put the same JAR in the server's `plugins` directory.

Start once to generate `wworldmap-utils.properties`, edit it, and restart. On Fabric it is stored under `config`; on Bukkit-family servers it is under `plugins/wWorldMapUtils`.

```properties
disable-entire-mod=false
disabled-features=cave-mode,entity-radar
```

Available feature IDs are `entire-mod`, `player-radar`, `entity-radar`, `orbit`, and `cave-mode`. Local client preferences are never overwritten; the server policy is an additional connection-scoped restriction.

See [the protocol specification](documentation/PROTOCOL.md) and [third-party integration guide](documentation/THIRD_PARTY_INTEGRATION.md).

## Development servers

Run `runServer.bat`, choose a server implementation and supported Minecraft version, and the script will:

1. resolve the latest build from the project's official API;
2. rebuild wWorldMap Utils;
3. install the new JAR into that server directory;
4. download Fabric API when Fabric is selected;
5. launch the selected server.

Each selection has an isolated directory under `servers/`.
