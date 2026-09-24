# Birds in Every Biome

Parrots that spawn everywhere — and pick up the colour of the biome they were born in.

A Fabric mod for **Minecraft 26.2**. Vanilla only spawns parrots in jungles, on grass or leaves.
This mod opens them up to every biome, relaxes the spawn surface to "any solid floor", and gives six
biomes their own plumage: a desert parrot is sand-coloured, a snowy one is white-blue, and the Nether
and the End hatch variants of their own. The variant is rolled once, when the parrot spawns, and then
saved with it forever.

| | |
|---|---|
| **Minecraft** | 26.2 |
| **Loader** | Fabric Loader 0.19.5+ |
| **Requires** | Fabric API, Java 25 |
| **Environment** | Client and server (works in single-player, on LAN and on dedicated servers) |
| **Optional** | [Mod Menu](https://modrinth.com/mod/modmenu) — adds an in-game config screen |
| **License** | MIT |

---

## Contents

- [Features](#features)
- [Install](#install)
- [Biome → look](#biome--look)
- [Configuration](#configuration)
- [Why a high spawn weight alone does nothing](#why-a-high-spawn-weight-alone-does-nothing)
- [Commands](#commands)
- [Known behaviour and limits](#known-behaviour-and-limits)
- [Documentation](#documentation)
- [Building from source](#building-from-source)
- [How it works](#how-it-works)
- [Credits](#credits)

---

## Features

- **Parrots spawn in every biome.** 63 of the 66 biomes get a parrot spawn entry. Jungles keep
  vanilla's own entry instead of gaining a second one.
- **Any solid floor counts.** Sand, snow, stone, netherrack, end stone — the vanilla "grass, leaves or
  logs only" rule is replaced by "something solid below, somewhere to stand, no fluid". Outside the
  Overworld the light-level rule is skipped, because the Nether and the End have no meaningful sky
  light.
- **Biome variants.** Six named skins (`vanilla`, `arid`, `snowy`, `mushroom`, `nether`, `end`).
  The skin is chosen from the biome the parrot spawns in and stored on the entity — it does not
  re-roll when the bird wanders to another biome.
- **Shoulder parrots keep their skin.** Put a desert parrot on your shoulder and it stays a desert
  parrot, on every client, including in someone else's game.
- **A config file, not a rebuild.** Spawn weight, group size, flock timing and both toggles live in
  `config/birds-in-every-biome.json`. Edit it and restart the game.
- **A spawner that actually fires.** Vanilla's passive spawner is throttled and cap-limited, so a
  spawn entry alone changes nothing in an explored world. The mod ships an independent flock spawner
  that drops small groups of parrots near players, using the same rules as the natural entry.
- **`/birds` diagnostics.** Ask why a bird would or wouldn't spawn where you're standing, or force a
  flock attempt and read exactly what happened.
- **Client-only clients and vanilla clients both work.** A vanilla client on a modded server still
  sees parrots — just in the normal five colours, since it doesn't know the skin data.

## Install

**From Modrinth (recommended)**

1. Install [Fabric Loader 0.19.5+](https://fabricmc.net/use/installer/) for Minecraft 26.2.
2. Add [Fabric API](https://modrinth.com/mod/fabric-api) and **Birds in Every Biome** to your mods.
   In the Modrinth App: search for the mod, **Install**, pick an instance that already has Fabric API.

**By hand**

1. Install Fabric Loader 0.19.5+ for Minecraft 26.2.
2. Put `birds-in-every-biome-<version>.jar` **and** `fabric-api-0.161.0+26.2.jar` in your `mods`
   folder (`%APPDATA%\.minecraft\mods` on Windows, `~/.minecraft/mods` on Linux/macOS).
3. Launch the Fabric profile.

Existing worlds work. Parrots that never had a skin get one the first time their chunk loads after
the mod is added; parrots that already had one keep it, because the skin is saved by name rather than
by index.

**On a server:** install on the server for the spawning behaviour, and on clients that want the new
textures. Nothing breaks if only one side has it.

## Biome → look

| Skin | Biomes | Texture file |
|---|---|---|
| `vanilla` | everything else, including jungles | the five vanilla parrot textures |
| `arid` | desert, badlands (+ eroded, wooded), savanna (+ plateau, windswept) | `parrot_arid.png` |
| `snowy` | snowy_plains, ice_spikes, snowy_taiga, snowy_beach, snowy_slopes, frozen_peaks, jagged_peaks, grove, frozen_river, frozen_ocean, deep_frozen_ocean | `parrot_snowy.png` |
| `mushroom` | mushroom_fields | `parrot_mushroom.png` |
| `nether` | all five Nether biomes | `parrot_nether.png` |
| `end` | every End biome | `parrot_end.png` |

The table lives in `ParrotSkins.RULES` (`src/main/java/com/mg3dp/birdsineverybiome/ParrotSkins.java`).
Rules are matched top to bottom and each rule is a set of biome IDs and/or biome tags, so
`BiomeTags.IS_BADLANDS`-style entries work alongside `"minecraft:desert"`. Adding a skin is three
steps — see [`docs/TECHNICAL.md`](docs/TECHNICAL.md#adding-a-biome-skin).

## Configuration

`config/birds-in-every-biome.json` is created on first launch:

```json
{
  "spawnWeight": 4,
  "minGroupSize": 1,
  "maxGroupSize": 2,
  "skipJungles": true,
  "flockEnabled": true,
  "flockDelaySeconds": 30,
  "maxBirdsNearby": 8
}
```

| Option | Default | Range in the config screen | Meaning |
|---|---|---|---|
| `spawnWeight` | `4` | 0–50 | How often parrots are picked when a creature spawn happens, relative to that biome's other animals. Vanilla jungles use 5. `0` disables the biome entry (flocks are separate). |
| `minGroupSize` | `1` | 1–8 | Fewest parrots in one spawn. |
| `maxGroupSize` | `2` | 1–8 | Most parrots in one spawn. |
| `skipJungles` | `true` | on/off | Leave vanilla's jungle parrots alone. `false` adds a second parrot entry to jungles, so they spawn parrots noticeably more often than everywhere else. |
| `flockEnabled` | `true` | on/off | Turn the independent flock spawner off if you want vanilla-only spawning. |
| `flockDelaySeconds` | `30` | 5–300 | Seconds between flock attempts, per player. Lower = more birds. |
| `maxBirdsNearby` | `8` | 1–32 | Stop adding flocks while this many parrots are already within 64 blocks of a player. |

Values are clamped on load: `spawnWeight ≥ 0`, `minGroupSize ≥ 1`,
`maxGroupSize ≥ minGroupSize`, `flockDelaySeconds ≥ 5`, `maxBirdsNearby ≥ 1`. A config that can't be
parsed falls back to defaults instead of crashing the game.

### Editing it in game (Mod Menu)

Install **Mod Menu** (optional — the mod runs fine without it), then **Mods → Birds in Every Biome →
Config**. Sliders for spawn weight, group sizes, flock timing and both toggles, plus **Save**,
**Cancel** and **Restore defaults**. The screen is built from plain vanilla widgets, so Cloth Config
is not needed.

Spawn-rate changes apply **the next time the world loads**: biome spawn lists are baked at world load,
so restart the world or the game after saving.

## Why a high spawn weight alone does nothing

`spawnWeight` feeds vanilla's own passive-mob spawner, and that spawner has two hard limits:

- friendly mobs only get a spawn attempt **once every 400 ticks (20 seconds)**, and
- it only runs while the **creature cap** has room — roughly 10–15 creatures for the loaded area, and
  those slots are taken by the animals that were generated together with the world.

Passive mobs never despawn, so in terrain you have already explored the cap stays full and **no new
animal ever spawns**, parrots included. That is why you can set the weight to 50 and still see
nothing: the entry is there (`/birds check` prints it), the spawner just never fires. The natural
entry does work in freshly generated chunks and in brand-new worlds.

So the mod also runs a **flock spawner**: every `flockDelaySeconds` it picks a random surface spot
32–56 blocks from each player, applies the exact same spawn rules, and adds `minGroupSize`–`maxGroupSize`
parrots there, stopping once `maxBirdsNearby` parrots are within 64 blocks. That is the bird
population you will notice in normal play.

The mechanic is documented in more detail — including the unloaded-chunk trap it has to dodge — in
[`docs/TECHNICAL.md`](docs/TECHNICAL.md#the-flock-spawner).

### Per-biome tuning

To give one biome its own weight on top of the global entry, add a line to
`BirdsInEveryBiome.onInitialize`:

```java
// lots of parrots in badlands, on top of the global entry
BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_BADLANDS), MobCategory.CREATURE,
        EntityTypes.PARROT, 20, 2, 4);
```

Spawn lists are built at world load, so a change needs a world reload before it shows up.

## Commands

`/birds` requires gamemaster (permission level 2) and works in single-player and on a server console.

| Command | What it does |
|---|---|
| `/birds check` | Biome, the skin a parrot would get there, whether the spawn rules pass at your feet (with the light level), how many parrots are nearby, and the state of the flock spawner. |
| `/birds spawn` | Force one flock attempt right now and report exactly what happened — spawned, rejected and why, or no loaded chunk in range. |

Example:

```
Birds in Every Biome - position check at 118, 64, -232
  biome: minecraft:desert
  skin a parrot would get here: arid
  natural spawn entry: weight 4
  spawn rules here: ok (light 15)
  parrots within 64 blocks: 2 (flock limit 8)
  flock spawner: on, every 30s - vanilla's own passive spawner is capped and
  throttled, so flocks are what you normally see
```

## Known behaviour and limits

- **No ocean or river spawns.** Their surface is water, which has no collision shape, so it never
  counts as a floor. Same reason there are no mid-air spawns.
- **Flocks ignore the `doMobSpawning` game rule** (the gamerule API moved in 26.2). Set
  `"flockEnabled": false` to stop them.
- **Flocks spawn 32+ blocks away**, so a bird can occasionally appear at the edge of your view.
- **The small rainbow patch** in the bottom-right of the vanilla parrot texture is leftover canvas
  art — no model part samples it, so it never shows in game.
- **Adding a parrot entry to every biome** increases creature spawn pressure slightly; lower
  `spawnWeight` (or set it to `0`) if a world feels overrun with birds.
- The Nether and the End have no daylight cycle, so a "day" there is irrelevant to spawns: the light
  rule is skipped entirely rather than faked.

## Documentation

| Document | Contents |
|---|---|
| [`docs/TECHNICAL.md`](docs/TECHNICAL.md) | Architecture, the two spawn paths, entity data and NBT, client rendering, config plumbing, how to add a skin, 26.2 API notes, how to verify a build. |
| [`docs/TEXTURES.md`](docs/TEXTURES.md) | The 32×32 parrot texture spec: UV layout, colour roles, the generator script, the resource-pack hot-reload loop, and the licensing note about Mojang art. |
| [`CHANGELOG.md`](CHANGELOG.md) | Release notes. |

## Building from source

Requires **JDK 25** and a network connection for Gradle, Loom and Minecraft.

```bash
git clone https://github.com/Matthew-Glt/birds-in-every-biome.git
cd birds-in-every-biome
./gradlew build          # jar -> build/libs/birds-in-every-biome-<version>.jar
./gradlew runClient      # dev client with the mod loaded
./gradlew runServer      # dev server
```

On Windows with several JDKs installed, Gradle can pick the wrong one. This project pins the build
JVM itself in `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`) — so make sure a JDK 25
is installed, and if Gradle still picks a different JDK, set `JAVA_HOME` for the launcher before
calling the wrapper. The two helper scripts in the repository root do that explicitly:

```powershell
.\build.ps1        # sets JAVA_HOME, then gradlew build
.\runclient.ps1    # sets JAVA_HOME, then gradlew runClient
```

The build also produces a `-sources.jar`, and embeds `LICENSE` into the mod jar.

## How it works

- `BirdsInEveryBiome` — Fabric's `BiomeModifications.addSpawn` adds one parrot spawn entry per biome
  (jungles skipped by default, since vanilla already has one there).
- `ParrotSpawnRules` — the relaxed spawn check: solid floor below, no block in the way, no fluid, and
  vanilla's `light > 8` rule in the Overworld only. Injected into `Parrot.checkParrotSpawnRules`.
- `ParrotMixin` — one extra synced entity-data field (`birdsineverybiome:skin`, an index into
  `ParrotSkins.SKIN_NAMES`). Assigned once from the spawn biome, saved to NBT **by name** (so
  reordering the skin list never breaks existing parrots) and synced to clients.
- `FlockSpawner` / `BirdSpawner` — the independent spawner: pick a surface spot 32–56 blocks out,
  throw away candidates in unloaded chunks, run the same rules, then create real parrots and let
  `finalizeSpawn` assign their skin.
- `BirdsCommand` — `/birds check` and `/birds spawn`.
- `BirdsConfig` — reads and writes `config/birds-in-every-biome.json` with Gson (no extra
  dependency), including a fallback for the mod's old `biome-parrots.json` file name.
- Client mixins — `ParrotRendererMixin` and `ParrotOnShoulderLayerMixin` swap the texture in
  `getTextureLocation` / the shoulder layer's variant lookup, reading the skin out of the render
  state. `AvatarRendererMixin`, `AvatarRenderStateMixin` and `PlayerMixin` carry the shoulder parrot's
  skin from the entity to the player model on every client.

## Credits

- Fabric API and the Fabric example mod, which the toolchain is based on.
- Parrot model and base textures are Mojang's; the biome variants are recolours of
  `parrot_red_blue.png`, generated by `tools/make_textures.py`. See the licensing note in
  [`docs/TEXTURES.md`](docs/TEXTURES.md#publishing-and-asset-licensing).

## License

MIT — see [`LICENSE`](LICENSE).
