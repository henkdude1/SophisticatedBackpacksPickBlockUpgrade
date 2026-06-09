# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A NeoForge mod for Minecraft **1.21.1** that adds a "Pick Block Upgrade" to
the **Sophisticated Backpacks** mod. With the upgrade installed in a backpack a
player carries, middle-clicking (pick block) a block in the world pulls the
matching item out of the backpack into the hotbar instead of doing nothing
(survival) — searching the player's inventory, armor/offhand, and Curios slots
for any backpack holding the upgrade.

## Build & run

The project uses Gradle with the **ModDevGradle** (`net.neoforged.moddev`)
plugin. Use the wrapper (`./gradlew`). Java 21 is required.

- Build the mod jar: `./gradlew build` (output in `build/libs/`)
- Refresh dependencies / fix IDE library issues: `./gradlew --refresh-dependencies`
- Full reset (does not touch source): `./gradlew clean`
- Run the client in dev: `./gradlew runClient`
- Run a dedicated server in dev: `./gradlew runServer`
- Run data generation: `./gradlew runData` (outputs to `src/generated/resources/`)
- Run gametests headlessly: `./gradlew runGameTestServer`

There is **no unit test suite**. CI (`.github/workflows/build.yml`) only runs
`./gradlew build` on push/PR with JDK 21 (Temurin). Verification of behavior is
done by launching the game via `runClient` — there are no automated tests to run
for a single case. In-game gametests (if added) live under the namespace
`mod_id` and run via `runGameTestServer` or the `/test` command in `runClient`.

## Key versions & properties

All version pins live in `gradle.properties` (do not hardcode them elsewhere —
`neoforge.mods.toml` is templated from these):

- `minecraft_version` / `neo_version` must agree (1.21.1 / 21.1.217)
- `mod_id = sophisticated_backpacks_pick_block_upgrade` — must equal the
  `MODID` constant in the main mod class and the `@Mod` annotation
- Parchment mappings supply readable parameter names (`parchment_*`)

Dependencies are pulled from **CurseMaven** by project/file id (Sophisticated
Backpacks, Sophisticated Core, JEI, Curios). Bumping a dependency means changing
the `curse.maven:...` coordinates in `build.gradle`.

## Architecture

The mod is small. The interesting design point is **how the pick-block action
flows from client input to server-side inventory mutation**, and how it hooks
into Sophisticated Backpacks/Core without registering a real upgrade type.

### Entry point & registration

- `SophisticatedBackpacksPickBlockUpgrade` — `@Mod` main class. Constructor
  registers `ModItems` and `ModCreativeModeTabs` against the mod event bus.
- `registry/ModItems` — registers the single `pick_block_upgrade` item
  (`PickBlockUpgradeItem`), passing `Config.SERVER.maxUpgradesPerStorage` from
  Sophisticated Backpacks as the per-storage count limit.
- `registry/ModCreativeModeTabs` — one creative tab containing the upgrade item.
- `registry/ModNetworking` — `@EventBusSubscriber` that registers the C2S
  payload handler on `RegisterPayloadHandlersEvent` (protocol version `"1"`).

### The pick-block flow (client → server)

1. **Client tick** (`client/ClientPickBlockHandler`, `Dist.CLIENT` only):
   detects a fresh press of the pick-item key (edge-detected via `wasDown`),
   confirms the player actually carries a backpack containing the upgrade
   (`playerHasPickBlockUpgrade`), resolves the target block under the crosshair
   to its `Item`, and sends a `C2SRequestPickBlock` packet with that item's
   `ResourceLocation`. If the player has no such upgrade, it returns early so
   **vanilla pick-block still works**.
2. **Server handler** (`network/C2SRequestPickBlock.handle`): re-scans the
   player's inventory, equipment, and Curios for backpacks, finds one with the
   upgrade (`hasPickBlockUpgrade`), extracts the best matching stack
   (`extractBestStack`, preferring a full stack of up to 64), and places it in
   the player's hand/inventory (`giveToPlayer`), then broadcasts menu changes.

The "does this backpack have the upgrade" check is done by **scanning the
backpack's upgrade handler for an item whose registry id is
`<mod_id>:pick_block_upgrade`** (see `hasPickBlockUpgrade` on both client and
server) — not by querying a registered Sophisticated Core `UpgradeType`.

### Upgrade type plumbing (`upgrade/` package)

`PickBlockUpgradeItem` (extends `UpgradeItemBase`), `PickBlockUpgradeWrapper`
(extends `UpgradeWrapperBase`, implements `IPickBlockUpgrade`), and the
`UpgradeType` they define follow the Sophisticated Core upgrade API. Note:
`PickBlockUpgradeWrapper.tryPickFromThisBackpack` implements per-backpack
extraction with cooldown/hotbar logic, but the **live functionality currently
runs through `C2SRequestPickBlock.handle`**, not through this wrapper method.
Treat the wrapper as the Sophisticated-Core-idiomatic path that exists alongside
the network-driven implementation; keep both consistent if you change behavior.

### Data/resources wiring

The item only becomes a usable backpack upgrade because of data files placed in
**Sophisticated Backpacks' own namespace**, not this mod's:

- `data/sophisticatedbackpacks/tags/item/upgrade.json` — adds the item to the
  `sophisticatedbackpacks:upgrade` tag so it can be inserted into a backpack.
- `data/sophisticatedbackpacks/recipe/pick_block_upgrade.json` — crafting recipe
  using `sophisticatedbackpacks:upgrade_base`.

Client-facing assets (model, texture, lang) live under this mod's namespace in
`assets/sophisticated_backpacks_pick_block_upgrade/`.

### Mod metadata is templated

`src/main/templates/META-INF/neoforge.mods.toml` is **not** the final file — the
`generateModMetadata` task in `build.gradle` expands `${...}` placeholders from
`gradle.properties` into `build/generated/sources/modMetadata`. Edit the
template, never a generated copy. The same task runs on IDE sync.

## Conventions

- Package root: `net.henkdude.sophisticatedbackpackspickblockupgrade`, split into
  `registry/`, `network/`, `upgrade/`, and `client/`. Client-only code must be
  gated with `Dist.CLIENT` (the handler is annotated accordingly).
- Always reference the mod id via `SophisticatedBackpacksPickBlockUpgrade.MODID`.
- Use `DeferredRegister` / `DeferredHolder` for all registry objects.
- Note `mod_group_id` in `gradle.properties` is still the template default
  (`com.example.examplemod`); the actual Java package is `net.henkdude.*`.
