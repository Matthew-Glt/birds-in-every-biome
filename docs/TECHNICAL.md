# Technical documentation

How *Birds in Every Biome* is put together, what each class does, why the awkward parts are awkward,
and where to extend it. Everything below is written against the source in this repository for
**Minecraft 26.2 / Fabric Loader 0.19.5 / Fabric API 0.161.0+26.2 / Java 25**.

---

## Module map

| File | Role |
|---|---|
| `BirdsInEveryBiome` | `ModInitializer`. Loads config, registers the biome spawn entry, the flock spawner and the command. |
| `BirdsConfig` | Plain Gson config object for `config/birds-in-every-biome.json` (load/save/defaults/clamping/legacy migration, per-biome allow list). |
| `ParrotSkins` | The biome → skin table: `SKIN_NAMES`, `RULES`, `skinFor(biome)`, `texture(index)`. |
| `ParrotSpawnRules` | The relaxed spawn check plus the `Denial` enum used by `/birds`. |
| `FlockSpawner` | Server-tick hook that drops flocks near players and reports a human-readable `Attempt` result. |
| `BirdSpawner` | Creates real parrots: surface spot search, entity construction, skin assignment, nearby count. |
| `BirdsCommand` | `/birds check` and `/birds spawn`. |
| `ParrotSkinAccess` | Duck interface on `Parrot`: read/write the synced skin index. |
| `PlayerShoulderSkins` | Duck interface on `Player`: shoulder skins + the pending-skin hand-off field. |
| `mixin/ParrotMixin` | Spawn rules, skin data field, skin assignment, NBT persistence. |
| `mixin/PlayerMixin` | Two synced shoulder-skin ints on the player and their NBT. |
| `mixin/ShoulderRidingEntityMixin` | Stashes the parrot's skin onto the player before the parrot is absorbed. |
| `client/ParrotSkinRenderState`, `client/AvatarSkinRenderState` | Duck interfaces on the two render states. |
| `client/mixin/ParrotRendererMixin` | Copies the skin into the parrot render state, swaps the texture. |
| `client/mixin/ParrotOnShoulderLayerMixin` | Draws the shoulder parrot with its biome texture. |
| `client/mixin/AvatarRendererMixin`, `AvatarRenderStateMixin` | Carry shoulder skins from player entity → avatar render state. |
| `client/BirdsConfigScreen` | Mod Menu config screen, built purely from vanilla widgets. Page 1 is the spawn settings, page 2 is a toggle per biome. |
| `client/VanillaBiomes` | The vanilla biome ids for the target Minecraft version, so the biome page works at the main menu (biomes are a dynamic registry). |
| `client/BirdsModMenu` | `ModMenuApi` entrypoint; only loaded when Mod Menu is present. |

Mixins are declared in `birdsineverybiome.mixins.json` (common) and
`birdsineverybiome.client.mixins.json` (client, `environment: client`).

---

## Startup

`BirdsInEveryBiome.onInitialize()`:

1. `BirdsConfig.load()` — reads or creates the config file (see below).
2. `BiomeModifications.addSpawn(selector, MobCategory.CREATURE, EntityTypes.PARROT, weight, min, max)`
   where `selector` is either `BiomeSelectors.all()` or "every biome that is not in `BiomeTags.IS_JUNGLE`"
   (default).
3. `FlockSpawner.register(config)` — end-of-server-tick hook.
4. `BirdsCommand.register(config)` — `/birds`.

The loaded `BirdsConfig` instance is captured by the flock spawner and the command and lives for the
whole game session. **Config edits are read once, at mod init.** The Mod Menu screen builds its own
`BirdsConfig.load()` copy, edits it and writes it back to disk, so new values take effect on the next
launch (and, because biome spawn lists are built when a world loads, a world reload is needed anyway
for `spawnWeight` changes).

---

## Spawn path A — the biome entry

`BiomeModifications.addSpawn` appends one parrot entry to the `CREATURE` (passive) spawn list of every
matching biome. That is the "correct" vanilla-shaped way to do this, and it has two consequences worth
knowing:

- **Spawn lists are baked at world load.** Changing `spawnWeight` needs a world (or game) reload.
- **The passive spawner barely fires.** Vanilla attempts a `CREATURE` spawn once every 400 ticks
  (20 s) per loaded area, and only while the creature cap has room (~10–15 in the loaded area). Those
  slots are filled by world-generation animals, and passive mobs never despawn, so in explored
  terrain the entry exists but never runs. `/birds check` prints the entry's weight to make this
  visible instead of mysterious.

Mountains of extra weight do not help: the entry competes with cows, sheep and pigs for a spawn event
that is itself rare, and the result is then clamped by the cap.

## Spawn path B — the flock spawner

`FlockSpawner` is the path players actually see. On `ServerTickEvents.END_SERVER_TICK`, when
`tickCount % (flockDelaySeconds * 20) == 0`, it runs one `attempt()` per player per dimension.

`attempt(level, origin, config)`:

1. **Cap check.** Count parrots within 64 blocks (`BirdSpawner.countNearby`). At or above
   `maxBirdsNearby`, abort with a reason.
2. **Pick a spot.** `BirdSpawner.findSpot` tries up to 8 times: a random angle, a random distance in
   `[32, 56)`, then the surface Y from `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES`.
   **Unloaded chunks report the minimum build height for every column**, so any candidate with
   `y <= level.getMinY() + 2` is discarded — otherwise a flock would aim at the bottom of the world
   and silently fail. If all 8 attempts land in unloaded chunks, `findSpot` returns `null` and the
   attempt reports "no loaded chunk 32-56 blocks away".
3. **Respect `skipJungles`.** A spot inside `BiomeTags.IS_JUNGLE` is rejected, so the flock spawner
   doesn't undo the setting that leaves vanilla jungle parrots alone.
4. **Run the real spawn rules** — the same `ParrotSpawnRules.check` the natural entry uses, so a
   flock can never appear somewhere a natural spawn would be refused.
5. **Spawn `min..max` parrots** (`minGroupSize + random.nextInt(max - min + 1)`), via
   `BirdSpawner.spawnFlock`.

`BirdSpawner.spawnFlock` creates parrots with `EntityTypes.PARROT.create(level, EntitySpawnReason.NATURAL)`,
places them with `snapTo` (a ±1 block jitter around the spot, random yaw), then calls
`finalizeSpawn(...)` — **that is what assigns the biome skin**, exactly as it would for a natural
spawn — and finally `level.addFreshEntity(parrot)`. It returns how many entities the level accepted,
so "the game refused the entities" is reported as its own outcome rather than looking like success.

Every attempt returns `Attempt(spawned, detail)`; the detail string is what `/birds spawn` shows and
what makes a failed attempt diagnosable instead of silent.

## Spawn rules (`ParrotSpawnRules`)

Injected at the head of `Parrot.checkParrotSpawnRules`, replacing vanilla's grass/leaves/logs test.
Reasons other than `NATURAL` and `CHUNK_GENERATION` (spawn eggs, `/summon`, spawners, breeding) are
never blocked. Otherwise, in order:

| Check | Failure (`Denial`) | Message |
|---|---|---|
| Block below has a non-empty collision shape | `NO_FLOOR` | "no solid block below" |
| Spawn position has an empty collision shape | `NO_SPACE` | "block in the way" |
| Spawn position holds no fluid | `FLUID` | "inside a fluid" |
| Overworld only: `getRawBrightness(pos, 0) > 8` | `TOO_DARK` | "too dark (needs light > 8 in the Overworld)" |
| Otherwise | `OK` | "ok" |

Design notes:

- **Collision shape, not block identity.** That single change is what lets sand, snow, stone,
  netherrack and end stone count as floors, and it is also why oceans and rivers never spawn birds:
  water has no collision shape, so it is never a floor.
- **The light rule is skipped outside the Overworld.** The Nether and the End have no meaningful sky
  light, so applying `light > 8` there would make spawn rates depend on lava glow.
- `Denial` is an enum with a message, so gameplay decisions and their explanations cannot drift apart.
  `/birds check` prints the enum's message verbatim.

## Skin assignment and persistence

`ParrotMixin` adds one `SynchedEntityData` int (`birdsineverybiome:skin`, an index into
`ParrotSkins.SKIN_NAMES`) plus a server-side `skinAssigned` flag.

- **Assignment** happens in `finalizeSpawn` (tail) and, as a safety net, at the head of `aiStep`.
  The `aiStep` path exists because chunk-generation parrots do not necessarily go through
  `finalizeSpawn`, and because a summoned entity's NBT is read before it is placed in the world.
- Assignment is **server-side only** and idempotent (`skinAssigned`). Clients never roll a skin; they
  only read what the server synced.
- **Persistence uses the skin *name***, not its index: `addAdditionalSaveData` writes
  `BirdsInEveryBiomeSkin` as a string, and `readAdditionalSaveData` maps it back through
  `ParrotSkins.indexOf`. Reordering `SKIN_NAMES` therefore cannot silently change existing parrots'
  appearance; an unknown name falls back to `vanilla`.
- **`readAdditionalSaveData` only adopts a skin that was actually saved.** A parrot from a world that
  predates the mod stays unassigned and gets its skin on the first tick, once its position is final —
  the biome at `(0, 0, 0)` is not the biome it will actually stand in.

## Shoulder parrots

Vanilla puts a parrot on a player's shoulder by deleting the parrot entity and syncing only
`Optional<Parrot.Variant>` to clients — which loses the biome skin. Three pieces fix that:

1. `ShoulderRidingEntityMixin` injects at the **head** of `ShoulderRidingEntity.setEntityOnShoulder`
   and stashes the parrot's skin on the player (`setPendingShoulderSkin`) *before* the parrot is
   absorbed.
2. `PlayerMixin` adds two synced ints (`shoulder left`/`right`) and copies the pending skin into the
   right one at the tail of `setShoulderParrotLeft` / `setShoulderParrotRight`; both are also saved to
   the player's NBT.
3. On the client, `AvatarRendererMixin` copies those two ints into the `AvatarRenderState` (via the
   `AvatarSkinRenderState` duck interface) at the tail of `extractRenderState`, and
   `ParrotOnShoulderLayerMixin` stashes the relevant one while `submitOnShoulder` runs, using
   `@Redirect` on the static `ParrotRenderer.getVariantTexture(variant)` call to return the biome
   texture instead. The static field is cleared on `RETURN` so the next submission can't inherit it.

## Client rendering of a world parrot

- `ParrotRendererMixin` copies the entity's synced skin into `ParrotRenderState` at the tail of
  `extractRenderState(...)` (runs once per frame per parrot).
- `getTextureLocation(ParrotRenderState)` is intercepted at the head: `skin > 0` returns
  `ParrotSkins.texture(skin)`, anything else falls through to vanilla's variant texture — so vanilla
  parrots look exactly as they always did.

## Per-biome toggles

`BirdsConfig.disabledBiomes` is a sorted list of biome ids that parrots may not spawn in. Empty means
every biome is allowed, and **unknown ids are allowed**, so a biome added by a future Minecraft
version or a datapack opts in by default. `load()` normalises the list (no nulls or duplicates,
`minecraft:` prefix added when missing, sorted) so a hand-edited file stays tidy.

Two call sites consume it, plus one reporter:

- **The natural entry.** The `BiomeModifications.addSpawn` selector asks
  `config.isBiomeAllowed(context.getBiomeKey())` for every biome, alongside the jungle rule. Because
  the spawn list is built at world load, a change needs a world reload.
- **The flock spawner.** `FlockSpawner.attempt` resolves the spot's biome with
  `level.getBiome(spot).getRegisteredName()` and refuses the attempt with
  `"biome <id> is switched off in the config"`. This check is per *spot*, so a flock can still be
  placed in a neighbouring enabled biome when the origin is in a disabled one.
- **`/birds check`** prints `spawning enabled in this biome: yes` or
  `no (switched off in the config)`.

The config screen's second page renders `BIOMES_PER_PAGE` (7) `CycleButton` toggles per page with
paging, Enable all / Disable all and Back. The biome list is
`VanillaBiomes.IDS` merged with the live registry (`minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME)`)
when a world is loaded, plus anything already in `disabledBiomes` — so a biome that is switched off
can always be switched back on, even if it is not present in the current world.

`VanillaBiomes` exists because biomes are a **dynamic** registry: at the main menu the client has no
biome registry at all (`BuiltInRegistries` has no `BIOME` entry in 26.2), so a hardcoded list of the
vanilla ids is the only way to offer the toggles before a world is loaded.

## Per-biome spawn weights

The toggles are on/off. For a different *weight* in one biome, add your own entry next to the global
one in `BirdsInEveryBiome.onInitialize`:

```java
// lots of parrots in badlands, on top of the global entry
BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_BADLANDS), MobCategory.CREATURE,
        EntityTypes.PARROT, 20, 2, 4);
```

Spawn lists are built at world load, so a change needs a world reload before it shows up.

## Config file

`config/birds-in-every-biome.json` is written on first launch (Gson with pretty printing — no extra
dependency). `BirdsConfig.load()`:

- if `birds-in-every-biome.json` is missing, the legacy `biome-parrots.json` name is checked and its
  values are migrated into the current file;
- clamps values into sane ranges (`spawnWeight ≥ 0`, `minGroupSize ≥ 1`,
  `maxGroupSize ≥ minGroupSize`, `flockDelaySeconds ≥ 5`, `maxBirdsNearby ≥ 1`);
- falls back to defaults (and logs a warning) if the file cannot be read or parsed.

`resetToDefaults()` backs the **Restore defaults** button; `save()` writes the file and logs the path.
The screen's slider ranges are wider than the file's legal range on purpose — clamping happens on load,
so a hand-edited file can never break the game.

## Adding a biome skin

1. Add the name to `ParrotSkins.SKIN_NAMES`. The name is also the texture file name, and index `0`
   must stay `vanilla` (it means "use the five normal parrot textures").
2. Add a `Rule` to `ParrotSkins.RULES`, top-down, using biome IDs (`"minecraft:desert"`) and/or tags
   (`BiomeTags.IS_BADLANDS`). First match wins.
3. Drop `parrot_<name>.png` into
   `src/main/resources/assets/birdsineverybiome/textures/entity/parrot/` (32×32) and rebuild.
   [`docs/TEXTURES.md`](TEXTURES.md) has the UV map, the colour roles and
   `tools/make_textures.py`, which generates a variant from the vanilla texture.

No mixin or registration changes are needed: the skin index, NBT name and texture path all flow from
`SKIN_NAMES`.

## Minecraft 26.2 API notes

Written against the 26.2 client jar, because 26.x renamed a lot of the 1.21 surface:

- `ResourceLocation` → `Identifier` (`net.minecraft.resources`).
- `EntityType.PARROT` → `EntityTypes.PARROT` (holder-style registry class).
- Spawn reasons are an enum: `EntitySpawnReason.NATURAL` / `CHUNK_GENERATION`, passed to spawn checks
  and `finalizeSpawn`.
- Entity NBT is read/written through `ValueInput` / `ValueOutput` (`getStringOr`, `putString`,
  `getIntOr`), not raw compound tags.
- Command permission checks use `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`
  rather than a numeric level comparison.
- Renderers are render-state based: extract into a `*RenderState` (`extractRenderState`), resolve
  textures from that state (`getTextureLocation(state)`). Player rendering goes through
  `Avatar` / `AvatarRenderer` / `AvatarRenderState`.
- Parrots live in `net.minecraft.world.entity.animal.parrot`, with `ShoulderRidingEntity` as the
  shoulder-riding base class.
- Building requires **Java 25** (Loom 1.17, `options.release = 25`).

## Testing a change

Mixin injection points are validated at runtime, not at compile time: a typo in an `@Inject` target
still compiles, and the game reports the failure when it loads. Read the log, not just the build.

1. `./gradlew build` — must end in `BUILD SUCCESSFUL`, producing
   `build/libs/birds-in-every-biome-<version>.jar` and the `-sources.jar`.
2. **Check the jar's contents** (`unzip -l` or any zip viewer): the five variant textures under
   `assets/birdsineverybiome/textures/entity/parrot/`, `fabric.mod.json` with the right `version`,
   both mixin configs, and the classes under `com/mg3dp/birdsineverybiome/`.
3. **Boot the dev client** (`./gradlew runClient`) and read `run/logs/latest.log` for mixin errors
   (`InvalidInjectionException`, `Mixin apply failed`). Any bad injection target shows up there, and
   the game will not reach the title screen cleanly.
4. **In game**, use `/birds check` where you stand, then `/birds spawn` and read the reported outcome.
   Create a new world in a desert or snowy biome to see the variants; `/birds check` tells you which
   skin the current biome would give.
5. **Dedicated server**: the client mixins are absent by design (they are declared with
   `environment: client`), so a server-only install spawns parrots and assigns skins correctly;
   only the rendering differs. Server-side testing can pin `/birds check` output but cannot exercise
   GPU-side texture swapping — that has to be observed in the client.

The spawn logic is all server side, so it can be exercised headlessly: start the dev server, enable
RCON in `run/server.properties`, and drive `/birds check` and `/birds spawn` from an RCON client
(`/execute positioned <x> <y> <z> run birds check` reaches any biome, and
`/locate biome minecraft:plains` finds a test spot). The config screen itself is client-only and has
to be looked at.

Textures can be iterated without rebuilding by using a resource pack that overrides
`assets/birdsineverybiome/...` and pressing **F3+T** — see
[`docs/TEXTURES.md`](TEXTURES.md#testing-without-rebuilding).

## Packaging

The mod jar contains only this mod: classes, mixin configs, `fabric.mod.json`, the variant textures
and the icon. Mod Menu is `clientCompileOnly` (never bundled), and `build.gradle`'s `jar` task renames
`LICENSE` to `LICENSE_birds-in-every-biome` and embeds it. `tools/` — including the extracted vanilla
texture and the generated previews — is development material and is not packaged.

`libs/modmenu-20.0.2.jar` is Mod Menu's own jar, kept in the repository because the config screen is
compiled against its API (`ModMenuApi`, `ConfigScreenFactory`) while Mod Menu itself stays an optional
runtime dependency — it is never bundled into the mod jar. Mod Menu is MIT-licensed.

The extracted vanilla texture in `tools/vanilla/` is Mojang art, so it is deliberately kept out of
version control and out of the distributed jar.

