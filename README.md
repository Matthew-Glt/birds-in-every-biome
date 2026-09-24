# Birds in Every Biome

Parrots that spawn everywhere — and pick up the colour of the biome they were born in.

A Fabric mod for **Minecraft 26.2**. Vanilla only spawns parrots in jungles, on grass or leaves.
This mod opens them up to every biome, relaxes the spawn surface to "any solid floor", and gives six
regions their own plumage: a desert parrot is sand-coloured, a snowy one is white-blue, and the Nether
and the End hatch variants of their own. The variant is rolled once, when the parrot spawns, and then
saved with it forever.

| | |
|---|---|
| **Minecraft** | 26.2 |
| **Loader** | Fabric Loader 0.19.5+ |
| **Requires** | Fabric API, Java 25 |
| **Environment** | Client and server (single-player, LAN and dedicated servers) |
| **Optional** | [Mod Menu](https://modrinth.com/mod/modmenu) — adds an in-game config screen |
| **License** | MIT |

---

## Contents

- [Features](#features)
- [Install](#install)
- [Biome → look](#biome--look)
- [Configuration](#configuration)
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
- **Per-biome control.** Every biome has an on/off toggle in the config screen (or an entry in
  `disabledBiomes`), so birds can be kept out of oceans, deserts, or anywhere else — both the natural
  spawn entry and the flocks respect it.
- **A spawner that actually fires.** Small groups of parrots are placed near players on a timer,
  using the same spawn rules as the natural entry, so birds show up in worlds you have already
  explored and not only in freshly generated terrain.
- **`/birds` diagnostics.** Ask why a bird would or wouldn't spawn where you're standing, or force a
  flock attempt and read exactly what happened.
- **Works with vanilla clients.** Players without the mod still see parrots on a modded server — in
  the normal five colours, since their client doesn't know the skin data.

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

Existing worlds work: parrots that never had a skin get one the first time their chunk loads after the
mod is added, and birds that already have one keep it.

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
  "maxBirdsNearby": 8,
  "disabledBiomes": []
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
| `disabledBiomes` | `[]` | — | Biome ids parrots may not spawn in, e.g. `["minecraft:ocean", "minecraft:desert"]`. Empty means every biome is allowed. The config screen has a toggle for each biome. |

Values are clamped on load: `spawnWeight ≥ 0`, `minGroupSize ≥ 1`,
`maxGroupSize ≥ minGroupSize`, `flockDelaySeconds ≥ 5`, `maxBirdsNearby ≥ 1`. A config that can't be
parsed falls back to defaults instead of crashing the game.

### Editing it in game (Mod Menu)

Install **Mod Menu** (optional — the mod runs fine without it), then **Mods → Birds in Every Biome →
Config**. Sliders for spawn weight, group sizes, flock timing and both toggles, plus **Save**,
**Cancel** and **Restore defaults**. The screen is built from plain vanilla widgets, so Cloth Config
is not needed.

**Biome toggles** opens a second page: one on/off switch per biome, seven per page, with **Enable
all** / **Disable all** and **Back**. The switch shows whether parrots may spawn in that biome — turn
one off and both the natural spawn entry and the flock spawner skip it. The page also shows every
biome the loaded world knows about, so datapack and modded biomes appear there too.

Spawn-rate and biome changes apply **the next time the world loads**: biome spawn lists are baked at
world load, so restart the world or the game after saving.

## Commands

`/birds` requires gamemaster (permission level 2) and works in single-player and on a server console.

| Command | What it does |
|---|---|
| `/birds check` | Biome, the skin a parrot would get there, whether the spawn rules pass at your feet (with the light level), how many parrots are nearby, and the state of the flock spawner. |
| `/birds spawn` | Force one flock attempt immediately and report the outcome — spawned, rejected and why, or no loaded chunk in range. |

Example:

```
Birds in Every Biome - position check at 118, 64, -232
  biome: minecraft:desert
  skin a parrot would get here: arid
  spawning enabled in this biome: yes
  natural spawn entry: weight 4
  spawn rules here: ok (light 15)
  parrots within 64 blocks: 2 (flock limit 8)
  flock spawner: on, every 30s - vanilla's own passive spawner is capped and
  throttled, so flocks are what you normally see
```

## Known behaviour and limits

- **No ocean, river or mid-air spawns.** Parrots need a solid block under them, and water and air are
  not solid.
- **Flocks are not gated by the `doMobSpawning` game rule.** Set `"flockEnabled": false` to stop
  them.
- **Flocks spawn 32+ blocks away**, so a bird can occasionally appear at the edge of your view.
- **The small rainbow patch** in the bottom-right of the vanilla parrot texture is leftover canvas
  art — no model part samples it, so it never shows in game.
- **Adding a parrot entry to every biome** increases creature spawn pressure slightly; lower
  `spawnWeight` (or set it to `0`) if a world feels overrun with birds.
- The light-level rule does not apply in the Nether or the End.

## Documentation

| Document | Contents |
|---|---|
| [`docs/TECHNICAL.md`](docs/TECHNICAL.md) | Architecture, the two spawn paths, entity data and NBT, client rendering, config plumbing, how to add a skin, 26.2 API notes, and how to test a change. |
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

The build JVM is pinned in `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`), so a JDK 25
has to be installed. If Gradle picks a different JDK anyway, set `JAVA_HOME` before calling the
wrapper. Two Windows helper scripts are included that do this for you:

```powershell
.\build.ps1        # jar -> build\libs\
.\runclient.ps1    # dev client with the mod loaded
```

The build also produces a `-sources.jar`, and embeds `LICENSE` into the mod jar.

## How it works

Parrots get a spawn entry in every biome through Fabric's `BiomeModifications`, a relaxed spawn check
that accepts any solid floor, and one synced entity-data field carrying the biome skin — saved by
name, so reordering the skin list never changes an existing bird. An independent flock spawner places
small groups near players, using the same rules as the natural entry.

Implementation notes, the client rendering path and extension recipes (adding a skin, per-biome spawn
weights) are in [`docs/TECHNICAL.md`](docs/TECHNICAL.md).

## Credits

- Fabric API and the Fabric example mod, which the toolchain is based on.
- Parrot model and base textures are Mojang's; the biome variants are recolours of
  `parrot_red_blue.png`, generated by `tools/make_textures.py`. See the licensing note in
  [`docs/TEXTURES.md`](docs/TEXTURES.md#publishing-and-asset-licensing).

## License

MIT — see [`LICENSE`](LICENSE).
