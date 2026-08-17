# wWorldMap server policy protocol

## Transport

- Channel: `wworldmap:policy`
- Direction: server to client
- Minecraft phase: play
- Delivery: send after the player joins and again whenever a server changes its policy
- Maximum version 1 payload size: 6 bytes

The payload is connection-scoped. wWorldMap resets to unrestricted local behavior when it disconnects. A server that never sends this payload does not restrict anything.

## Version 1 payload

| Offset | Type | Meaning |
| --- | --- | --- |
| 0 | unsigned byte | Protocol version, currently `1` |
| 1 | Minecraft unsigned VarInt | Bitmask of disabled features |

Version 1 senders must not append fields. Receivers should ignore unknown feature bits so a newer server does not break an older client. The official client ignores malformed payloads and unsupported protocol versions without replacing the last valid policy.

Minecraft VarInt encoding writes seven value bits per byte, least-significant group first, and sets bit 7 when another byte follows. Negative masks are invalid.

## Feature bits

| Bit | Mask | Configuration ID | Effect |
| ---: | ---: | --- | --- |
| 0 | `0x01` | `entire-mod` | Disable all wWorldMap behavior for this connection. |
| 1 | `0x02` | `world-map` | Prevent the main world-map screen and its terrain work. |
| 2 | `0x04` | `minimap` | Hide the minimap and stop minimap-only work. |
| 3 | `0x08` | `cave-view` | Force cave view and level cut off without changing local settings. |
| 4 | `0x10` | `entity-radar` | Hide mob and other-player markers. The player's own marker remains available. |
| 5 | `0x20` | `waypoint-integration` | Disable wWaypoints rendering and controls inside wWorldMap. It does not disable the separate wWaypoints mod. |
| 6 | `0x40` | `detached-window` | Prevent and close the detached Window View. |

Bit 0 overrides all other bits. A policy disabling the minimap and entity radar, for example, has mask `0x14` and bytes `01 14`.

## Compatibility and trust

- The policy is advisory enforcement implemented by the wWorldMap client. It is not an anti-cheat boundary and cannot control modified clients.
- Vanilla clients and clients without wWorldMap ignore the channel.
- Plugin messages use a player's connection. There is no delivery route when no player is connected.
- Proxies must permit/forward `wworldmap:policy` from the trusted backend to the client.
- Clients must only accept this policy from their current server connection and must clear it on disconnect.
