# wUtilities server policy protocols

## Common transport and framing

Both policies are server-to-client play-phase payloads sent after join and whenever policy changes. Each payload has exactly two fields and a maximum encoded size of 6 bytes:

| Offset | Type | Meaning |
| --- | --- | --- |
| 0 | unsigned byte | Protocol version |
| 1 | Minecraft unsigned VarInt | Bitmask of disabled features |

Senders must not append fields. Receivers ignore unknown feature bits so newer servers remain compatible with older clients. Malformed payloads and unsupported versions must not replace the last valid policy. Negative masks are invalid.

Policies are connection-scoped. Each client resets to unrestricted local behavior on disconnect. A server that never sends a channel does not restrict that mod.

## wWorldMap policy

- Channel: `wworldmap:policy`
- Protocol version: `2`

This protocol and its existing bit assignments are unchanged.

| Bit | Mask | Configuration ID | Effect |
| ---: | ---: | --- | --- |
| 0 | `0x01` | `entire-mod` | Disable all wWorldMap behavior for this connection. |
| 1 | `0x02` | `player-radar` | Hide other-player markers; retain the local player marker. |
| 2 | `0x04` | `entity-radar` | Hide non-player entity and mob markers. |
| 3 | `0x08` | `orbit` | Disable orbit views and force active map/minimap views to top-down. |
| 4 | `0x10` | `cave-mode` | Force cave mode and level cut off without changing local settings. |

A policy disabling player and entity radar has mask `0x06` and bytes `02 06`.

## wWaypoints policy

- Channel: `wwaypoints:policy`
- Protocol version: `1`

| Bit | Mask | Configuration ID | Effect |
| ---: | ---: | --- | --- |
| 0 | `0x01` | `entire-mod` | Disable all wWaypoints behavior and integration for this connection. |
| 1 | `0x02` | `sneak-modifications` | Disable Toggle Sneak, input overrides, container persistence, and interaction rerouting. |
| 5 | `0x20` | `sign-modifications` | Disable sign editor popup suppression, restoring the vanilla sign editor. |

Bit 0 overrides every feature-specific bit. `sneak_modifications` and other underscore forms are accepted as configuration aliases, but the canonical IDs use hyphens.

Bits 2-4 (`0x04`, `0x08`, `0x10`) are retired and ignored by updated clients. They formerly disabled death waypoints, chat-coordinate capture, and Hoplite helpers; those features can now only be restricted through `entire-mod`. Do not reuse these bits. Older clients ignore the new sign bit and require an update to enforce it. The protocol version stays `1`, preserving the existing whole-mod and sneak bit assignments.

A policy disabling sneak and sign modifications has mask `0x22` and bytes `01 22`. The two restrictions are independent; sign-only policy leaves Toggle Sneak available.

## Compatibility and trust

- These policies are advisory enforcement implemented by the official clients. They are not an anti-cheat boundary and cannot control modified clients.
- Vanilla clients and clients without the corresponding mod ignore its channel.
- Plugin messages use a player's connection; there is no delivery path when no player is connected.
- Proxies must permit and forward both policy channels from the trusted backend to the client.
- Clients must only accept policy from their current server connection and clear it on disconnect.
