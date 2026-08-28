# Platform support and artifacts

## Server matrix

| Platform | Minecraft support | Java | Artifact family | Runtime dependency |
| --- | --- | ---: | --- | --- |
| Spigot / Paper / Folia | 1.20+ | 17+ | `Bukkit 1.20+` | None |
| Fabric / Quilt | 1.20 through 26.2 compatibility cohorts | 17 / 21 / 25 by Minecraft | `Fabric <cohort>` | Fabric API |
| NeoForge | 1.20.2 through 26.2 compatibility cohorts | 17 / 21 / 25 by Minecraft | `NeoForge <cohort>` | None |
| Forge | 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | 17 / 21 / 25 by Minecraft | `Forge <version>` | None |

The Fabric artifacts also support Quilt Loader through its Fabric compatibility layer. Do not install a Forge artifact on NeoForge or vice versa. Select the artifact whose filename contains the server loader and Minecraft version/cohort.

Fabric and NeoForge releases use these compatibility cohorts: `1.20-1.20.1`, `1.20.2`, `1.20.3-4`, `1.20.5-6`, `1.21-1.21.1`, `1.21.2-4`, `1.21.5`, `1.21.6-8`, `1.21.9-10`, `1.21.11`, `26.1.2`, and `26.2`. NeoForge begins at 1.20.2; use Forge for 1.20.1.

## Compatibility architecture

The policy/configuration core is loader-neutral. Stonecutter generates version-specific source at the API transitions, and Architectury Loom remaps each loader artifact. Runtime entrypoints remain narrow native adapters:

- Bukkit plugin messaging for Spigot/Paper/Folia;
- Fabric networking for Fabric/Quilt;
- Forge channels plus the legacy 1.20.1 payload path;
- NeoForge payload registration plus the legacy 1.20.2-1.20.4 path.

The Bukkit adapter performs no scheduled or world-region work. Join and channel-registration callbacks only encode an immutable policy snapshot and send it through the joining player's connection, which keeps the implementation safe for the Folia execution model.

## Validation

`buildAll` compiles, tests, remaps/packages, and collects every release artifact. Compilation establishes API compatibility; it does not by itself prove a live server startup. Runtime release testing should cover one clean server per artifact cohort, a vanilla client, a protocol-v2 wWorldMap client, disconnect/reset behavior, and Folia multi-region joins before publishing a release as runtime-tested.
