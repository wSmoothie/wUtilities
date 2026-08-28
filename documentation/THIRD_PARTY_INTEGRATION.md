# Sending wWorldMap policy from another plugin or mod

You do not need to depend on wWorldMap Utils. Implement the two-field protocol in [PROTOCOL.md](PROTOCOL.md) and send it on `wworldmap:policy`.

## Bukkit/Paper example

Register the outgoing channel during `onEnable`, then send after join. Re-send after any policy change.

```java
private static final String CHANNEL = "wworldmap:policy";

@Override
public void onEnable() {
    getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
    getServer().getPluginManager().registerEvents(new Listener() {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onJoin(PlayerJoinEvent event) {
            int disabledMask = 0x02 | 0x04; // player-radar + entity-radar
            event.getPlayer().sendPluginMessage(
                MyPlugin.this, CHANNEL, encodePolicy(disabledMask));
        }
    }, this);
}

private static byte[] encodePolicy(int disabledMask) {
    if (disabledMask < 0) throw new IllegalArgumentException("negative mask");
    ByteArrayOutputStream out = new ByteArrayOutputStream(6);
    out.write(2); // protocol version
    do {
        int next = disabledMask & 0x7F;
        disabledMask >>>= 7;
        if (disabledMask != 0) next |= 0x80;
        out.write(next);
    } while (disabledMask != 0);
    return out.toByteArray();
}
```

Call `unregisterOutgoingPluginChannel` from `onDisable` if your plugin owns the registration. On Folia, keep any player/world work on its correct owner; encoding immutable policy bytes does not require world access.

## Fabric example

Define a `CustomPacketPayload` whose codec writes an unsigned byte with value `2`, followed by the disabled-mask VarInt. Register it with `PayloadTypeRegistry.playS2C()` during mod initialization. At `ServerPlayConnectionEvents.JOIN`, check `ServerPlayNetworking.canSend(player, TYPE)` before sending so clients without the receiver are left alone.

```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    if (ServerPlayNetworking.canSend(handler, PolicyPayload.TYPE)) {
        ServerPlayNetworking.send(handler.player, new PolicyPayload(disabledMask));
    }
});
```

The `PolicyPayload` type ID must be `wworldmap:policy`; its codec must match the byte layout exactly. Fabric's networking registry must be populated before any receiver or sender uses the type.

## Updating a live policy

The official Utils build reads configuration only at startup. A third-party implementation may support live updates. Publish a complete replacement mask to every connected player; masks are snapshots, not patches. Sending mask `0` removes all server restrictions.

Keep payload generation bounded and deterministic. Do not accept a client-supplied mask as server policy, and do not use this channel for secrets or durable server-to-server messaging.

## Forge and NeoForge

Minecraft 1.20.5 and newer use the typed `CustomPacketPayload` form of this protocol. Register the clientbound play payload as optional, use the exact `wworldmap:policy` type and version-2 codec, and send only when the remote connection advertises the channel. Forge and NeoForge use different native registration and distribution APIs; do not make either loader depend on the other's classes.

Minecraft 1.20.1 Forge and 1.20.2-1.20.4 NeoForge use the legacy custom-payload form. The bytes remain identical (`02`, then the disabled-mask VarInt), but the vanilla packet package/constructor changes at 1.20.2. Keep that compatibility edge isolated instead of reflecting across packet classes.
