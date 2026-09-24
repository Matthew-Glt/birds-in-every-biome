# Birds in Every Biome

Parrots that spawn **everywhere** — and pick up the colour of the biome they were born in.

Vanilla only spawns parrots in jungles, and only on grass, leaves or logs. This mod opens them up to
every biome, relaxes the spawn surface to "any solid floor", and gives six regions their own plumage:
a desert parrot is sand-coloured, a snowy one is white-blue, and the Nether and the End hatch variants
of their own. The variant is rolled once, when the parrot spawns, and then saved with it forever.

| | |
|---|---|
| **Minecraft** | 26.3 |
| **Loader** | Fabric Loader 0.19.5+ |
| **Requires** | [Fabric API](https://modrinth.com/mod/fabric-api) and Java 25 |
| **Environment** | Client and server — single-player, LAN and dedicated servers |
| **Optional** | [Mod Menu](https://modrinth.com/mod/modmenu) for the in-game config screen |
| **License** | MIT |

---

## What it does

- **Parrots spawn in every biome.** 64 of the 67 biomes get a parrot spawn entry. Jungles keep
  vanilla's own entry instead of gaining a second one.
- **Any solid floor counts.** Sand, snow, stone, netherrack, end stone — the vanilla
  "grass, leaves or logs only" rule is replaced by "something solid below, somewhere to stand, no
  fluid". In the Nether and the End the light-level rule is skipped, because they have no meaningful
  sky light. Oceans, rivers and open air stay empty: water and air are not floors.
- **Six biome skins.** `vanilla` (everywhere else, including jungles), `arid`, `snowy`, `mushroom`,
  `nether` and `end`. The skin is decided by the biome the parrot spawns in and stored on the entity,
  so a bird that wanders into another biome keeps the look it was born with.
- **Shoulder parrots keep their skin too** — on your shoulder and on every other player's screen.
- **A spawner that actually fires.** Small groups of parrots are placed near players on a timer,
  using the same spawn rules as the natural entry, so birds show up in worlds you have already
  explored and not only in freshly generated terrain.
- **Everything is configurable** without rebuilding: spawn weight, group sizes, jungle handling,
  flock timing and a nearby-bird cap, in `config/birds-in-every-biome.json` or from the Mod Menu
  config screen.
- **A toggle for every biome.** The config screen's **Biome toggles** page switches spawning on or
  off per biome — both the natural spawn entry and the flocks respect it. Every biome the loaded
  world knows about is listed, including datapack and modded ones.
- **Diagnostics built in** — `/birds check` explains why a bird would or wouldn't spawn where you are
  standing, and `/birds spawn` forces one attempt and reports exactly what happened.
- **Vanilla clients keep working** on a modded server: they simply draw the normal five parrot
  colours, because they don't know the skin data.

## Biome → look

| Skin | Biomes |
|---|---|
| `vanilla` | everything else, including jungles |
| `arid` | desert, badlands (+ eroded, wooded), savanna (+ plateau, windswept) |
| `snowy` | snowy plains, ice spikes, snowy taiga, snowy beach, snowy slopes, frozen peaks, jagged peaks, grove, frozen river, frozen ocean, deep frozen ocean |
| `mushroom` | mushroom fields |
| `nether` | all five Nether biomes |
| `end` | every End biome |

## Install

1. Install **Fabric Loader 0.19.5+** for **Minecraft 26.3**.
2. Add [Fabric API](https://modrinth.com/mod/fabric-api) and this mod to your `mods` folder — the
   Modrinth App does it for you if you pick an instance that already has Fabric API.
3. Launch. Existing worlds work: parrots that never had a skin get one the first time their chunk
   loads; birds that already have one keep it.

On a server: install it on the server for the spawning behaviour and on clients that want the new
textures. Nothing breaks if only one side has it.

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

| Option | Default | Meaning |
|---|---|---|
| `spawnWeight` | `4` | How often parrots are picked when a creature spawn happens in a biome, relative to that biome's other animals. Vanilla jungles use 5. `0` disables the biome entry. |
| `minGroupSize` / `maxGroupSize` | `1` / `2` | Parrots per spawn. `2`-`3` gives small flocks. |
| `skipJungles` | `true` | Leave vanilla's jungle parrots alone. `false` adds a second parrot entry to jungles, so jungles spawn parrots noticeably more often. |
| `flockEnabled` | `true` | Turn the independent flock spawner off for vanilla-only spawning. |
| `flockDelaySeconds` | `30` | Seconds between flock attempts, per player. |
| `maxBirdsNearby` | `8` | Stop adding flocks while this many parrots are already within 64 blocks of a player. |
| `disabledBiomes` | `[]` | Biome ids parrots may not spawn in, e.g. `["minecraft:ocean", "minecraft:desert"]`. Empty means every biome is allowed. |

Values are clamped on load, and a broken file falls back to defaults instead of crashing.
Spawn-rate changes apply the next time a world loads — biome spawn lists are built at world load.

**With Mod Menu**, the config screen gives you sliders for all of it plus **Save**, **Cancel** and
**Restore defaults**, and a **Biome toggles** page with an on/off switch per biome (seven per page,
plus **Enable all** / **Disable all**). It is built from vanilla widgets, so Cloth Config is not
required.

## Commands

`/birds` needs gamemaster permission (level 2) and works in single-player, from a server console and
in command blocks.

- **`/birds check`** — biome, the skin a parrot would get there, whether spawning is enabled in that
  biome, whether the spawn rules pass at your feet (and the light level), how many parrots are
  nearby, and the state of the flock spawner.
- **`/birds spawn`** — forces one flock attempt immediately and reports the outcome ("spawned 2
  parrot(s) at 118, 64, -232 (minecraft:desert)" or the exact reason it failed).

## Notes and known limits

- **No ocean or river spawns.** Their surface is water, which has no collision shape, so it never
  counts as a floor. Same reason there are no mid-air spawns.
- **Flocks are not gated by the `doMobSpawning` game rule.** Set `"flockEnabled": false` to stop
  them.
- **Flocks spawn 32+ blocks away**, so a bird can occasionally appear at the edge of your view.
- **Adding a parrot entry to every biome** increases creature spawn pressure slightly; lower
  `spawnWeight`, or set it to `0`, if a world feels overrun with birds.
- The small rainbow patch in the bottom-right of the vanilla parrot texture is leftover canvas art —
  no model part samples it, so it never appears in game.

## Links

- **Source and issues:** [github.com/Matthew-Glt/birds-in-every-biome](https://github.com/Matthew-Glt/birds-in-every-biome)
- **Full documentation:** [README](https://github.com/Matthew-Glt/birds-in-every-biome#readme) ·
  [technical notes](https://github.com/Matthew-Glt/birds-in-every-biome/blob/main/docs/TECHNICAL.md) ·
  [texture guide](https://github.com/Matthew-Glt/birds-in-every-biome/blob/main/docs/TEXTURES.md) ·
  [changelog](https://github.com/Matthew-Glt/birds-in-every-biome/blob/main/CHANGELOG.md)

## Credits

Built on the Fabric toolchain and the Fabric example mod. The parrot model and base textures are
Mojang's; the biome variants are recolours generated by the included `tools/make_textures.py`.
