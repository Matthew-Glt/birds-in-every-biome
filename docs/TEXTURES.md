# Texture guide

Everything needed to make or change a biome variant for *Birds in Every Biome*: the spec, the UV
layout of the parrot model, what the colours in the base texture mean, and the fast iteration loop.

---

## The spec

- **PNG, 32×32 pixels.** Vanilla's are indexed-colour PNGs; plain RGBA PNGs work just as well.
- Path inside the mod:
  `src/main/resources/assets/birdsineverybiome/textures/entity/parrot/parrot_<skinname>.png`
- The file name must match the skin name in `ParrotSkins.SKIN_NAMES`. `parrot_vanilla.png` is never
  used — index `0` means "draw the normal five vanilla variants".

## Fast start

```powershell
python tools\make_textures.py
```

The script extracts the vanilla texture out of the game jar (cached in `tools\vanilla\`), recolours it
into every variant using the palette at the top of the file, writes them into the mod, and generates
`tools\preview.png` and `tools\texture-guide.png`. Change the RGB values in `SKINS` and rerun to
retune — that is the quickest way to iterate.

Doing it by hand:

```powershell
Copy-Item "$env:APPDATA\.minecraft\versions\26.2\26.2.jar" "$env:TEMP\26.2.zip" -Force
Expand-Archive "$env:TEMP\26.2.zip" "$env:TEMP\mc26" -Force
Copy-Item "$env:TEMP\mc26\assets\minecraft\textures\entity\parrot\parrot_red_blue.png" `
          "src\main\resources\assets\birdsineverybiome\textures\entity\parrot\parrot_yourskin.png"
```

Always start from `parrot_red_blue.png`: it is the only variant with two strong colours (red body +
blue wing/tail), which is what a biome variant wants.

## What lives where on the 32×32 canvas

Straight from `ParrotModel` in 26.2:

| Pixels (x, y) | Size | Part |
|---|---|---|
| (10, 0) | 12×5 | crest |
| (22, 1) | 8×5 | tail |
| ( 2, 2) | 8×5 | head + face (the eye is the dark pixel) |
| (11, 7) | 4×3 | beak, upper |
| (16, 7) | 4×3 | beak, lower |
| ( 2, 8) | 12×9 | body |
| (19, 8) | 8×8 | wings (both wings share this patch) |
| ( 2, 18) | 8×9 | long tail/leg feathers |
| (14, 18) | 4×3 | feet (both legs share this patch) |
| everything else | | unused canvas — no part samples it, so it can stay white |

Each patch is a cube's UV layout: a strip of the four side faces plus the top/bottom faces, so the
shading pattern inside a patch is what makes it look 3D. **Recolour, don't redraw** — that keeps the
layout intact. If you do redraw, keep the same patch boundaries; they are fixed by the model.

## Colour roles in the base texture

| Pixels | Meaning |
|---|---|
| red | body colour → becomes the *primary* colour |
| blue | wing/tail colour → becomes the *secondary* colour |
| yellow | accents (wing underside / tail edge) → becomes the *accent* colour |
| near-black | outlines, eye, beak — leave alone |
| greys | feet and shading — leave alone |
| white | unused canvas — leave alone |

`tools/make_textures.py` does exactly this classification (hue family per pixel, shading and relative
saturation preserved), which is why the generated variants keep the original light and shadow.

## Tools

Paint.NET, GIMP, Aseprite, or Photopea in a browser — anything that can edit a 32×32 PNG.

By hand: open your copy of the texture, use *select by colour* on a red pixel (Paint.NET: Magic Wand
with "global" and ~30% tolerance), then Recolour / Hue-shift the selection, and repeat for blue and
yellow. Save as PNG.

## Testing without rebuilding

Resource packs **override** mod assets, which gives a fast edit loop:

1. Make `run\resourcepacks\parrot-edit\pack.mcmeta` containing
   `{"pack":{"pack_format":64,"description":"parrot texture edits"}}`
   (in the real game: `%APPDATA%\.minecraft\resourcepacks\parrot-edit\`).
2. Put your PNG at
   `...\parrot-edit\assets\birdsineverybiome\textures\entity\parrot\parrot_arid.png`.
3. Enable the pack in-game, edit the PNG, press **F3+T** to hot-reload textures.

When you are happy, copy the PNG into `src\main\resources\...` and rebuild.

## Publishing and asset licensing

The parrot model and the base textures are **Mojang's**. Recolours are fine for your own game, and
this is what most mods that add mob variants do, but Mojang's asset terms do not grant redistribution
rights — so treat the recoloured PNGs as the weakest link in a published build.

If you want a build that is unambiguously yours:

1. Replace the variant PNGs with art you drew yourself (keep the 32×32 layout and the patch
   boundaries above), or
2. ship the generator instead — `tools/make_textures.py` stays in the repository and lets anyone
   produce the variants from their own game files.

`tools/vanilla/`, `tools/preview.png` and `tools/texture-guide.png` are development files and are
not part of the mod jar either way.
