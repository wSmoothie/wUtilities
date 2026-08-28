# wWorldMap Utils

wWorldMap Utils lets a Minecraft server disable all of wWorldMap or selected client features. It supports Minecraft 1.20 and newer through platform-specific artifacts:

- Bukkit: Spigot, Paper, and Folia (also compatible derivatives such as Purpur and Leaf);
- Fabric and Quilt through the shared Fabric-compatible server artifact;
- native Forge artifacts for the Forge versions supported by wWorldMap;
- native NeoForge artifacts across the 1.20+ compatibility cohorts.

Clients need a wWorldMap build that implements protocol v2 on `wworldmap:policy`. Players without wWorldMap can join normally. Architectury Loom and Stonecutter provide the cross-version build, while runtime adapters use native platform APIs; no Architectury API runtime JAR is required.

## Build

```powershell
.\gradlew.bat buildAll
```

Deployable JARs are collected under `build/artifacts`. Source JARs are not deployable. See [platform support and artifact selection](documentation/PLATFORMS.md) for the complete matrix.

## Install

- Spigot/Paper/Folia: put the Bukkit JAR in `plugins`.
- Fabric/Quilt: put the matching Fabric cohort JAR and Fabric API in `mods`.
- Forge/NeoForge: put the matching native loader JAR in `mods`.

Start once to generate `wworldmap-utils.properties`, edit it, and restart. Bukkit-family servers store it under `plugins/wWorldMapUtils`; mod loaders store it under `config`.

```properties
disable-entire-mod=false
disabled-features=cave-mode,entity-radar
```

Available feature IDs are `entire-mod`, `player-radar`, `entity-radar`, `orbit`, and `cave-mode`. Local client preferences are never overwritten; server policy is an additional connection-scoped restriction.

See the [protocol specification](documentation/PROTOCOL.md), [platform guide](documentation/PLATFORMS.md), and [third-party integration guide](documentation/THIRD_PARTY_INTEGRATION.md).

## Development servers

`runServer.bat` prepares an isolated 1.21.11 Fabric or Bukkit-family development server under `servers/`, builds the correct artifact, installs it, and launches the selected server. Use loader-native run configurations or a normal test server for the other compatibility cohorts.
