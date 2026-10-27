# Dupe Button (Fabric, Minecraft 26.2)

Client-side mod that adds a **Dupe** button beside chest screens. Clicking it runs a packet-desync macro (see `MacroRunner.java`):

1. Hold outgoing container packets and shift-click the first item in the chest.
2. Wait `delayTicks` ticks.
3. Close the screen without the server receiving the close packet.
4. Release the held click.

A key (default **Right Alt**) shows/hides the button. Rebind it in **Options > Controls > Key Binds > Dupe Button**.
Settings live in `config/dupebutton.properties` (`showButton`, `delayTicks`).

**Important:** vanilla Minecraft has no dupe. This only does anything on servers that have a matching bug, and most servers prohibit it. Use it only where you are allowed to.

## Known gotcha after running the macro

The server still thinks the chest is open, so it ignores inventory clicks from your client even though they look like they work. Walk away from the chest or open and close it normally to resync.

## Build

Requires **Java 25**.

```
gradle wrapper --gradle-version 9.5.1   # first time only, if the wrapper files are missing
./gradlew build
```

The jar is in `build/libs/`. Install it in `mods/` with Fabric Loader 0.19.3+ and Fabric API for 26.2.

## If it doesn't compile

This was written without being able to compile it. The version-sensitive spots are:

- `MacroRunner`: `handleContainerInput(..., ContainerInput.QUICK_MOVE, ...)` (older name: `handleInventoryMouseClick` with `ClickType`).
- `DupeButtonClient`: `Screens.getWidgets`, `KeyMappingHelper`, `KeyMapping.Category.register`.
- `ClientCommonPacketListenerImplMixin`: the `send(Packet)` target.
