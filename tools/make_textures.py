#!/usr/bin/env python3
"""
Birds in Every Biome - variant texture generator (Minecraft 26.2 / Fabric).

Usage:  python tools/make_textures.py

It will:
  1. pull the vanilla parrot texture out of your Minecraft client jar (cached in tools/vanilla/)
  2. recolour parrot_red_blue.png once per biome skin using the palette in SKINS
  3. write them into the mod: src/main/resources/assets/birdsineverybiome/textures/entity/parrot/
  4. write tools/preview.png (all skins side by side) and tools/texture-guide.png (labelled map)
  5. regenerate assets/birdsineverybiome/icon.png from the arid variant

Tweak SKINS and rerun to retune colours. Everything else can stay untouched.
"""
from __future__ import annotations

import colorsys
import io
import os
import zipfile

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(HERE)
MC_JAR = os.path.expandvars(r"%APPDATA%\.minecraft\versions\26.2\26.2.jar")
BASE_TEXTURE = "parrot_red_blue.png"
TEX_DIR = os.path.join(PROJECT, "src", "main", "resources", "assets", "birdsineverybiome",
                       "textures", "entity", "parrot")
ICON_PATH = os.path.join(PROJECT, "src", "main", "resources", "assets", "birdsineverybiome", "icon.png")
VANILLA_CACHE = os.path.join(HERE, "vanilla")

# skin name -> (primary body colour, secondary wing/tail colour, accent colour)
SKINS = {
    "arid":     ((230, 176, 74), (122, 63, 34), (245, 224, 163)),
    "snowy":    ((207, 228, 242), (110, 150, 204), (234, 246, 255)),
    "mushroom": ((168, 79, 208), (91, 59, 115), (233, 199, 245)),
    "nether":   ((142, 27, 18), (255, 138, 31), (255, 210, 74)),
    "end":      ((216, 203, 240), (126, 107, 196), (242, 237, 176)),
}

# Footprint of every box of ParrotModel on the 32x32 canvas (from ParrotModel.createBodyLayer, 26.2)
# (u, v, width, height, label)
PARTS = [
    (10, 0, 12, 5, "crest"),
    (22, 1, 8, 5, "tail"),
    (2, 2, 8, 5, "head + face (eyes = dark pixels)"),
    (11, 7, 4, 3, "beak (upper)"),
    (16, 7, 4, 3, "beak (lower)"),
    (2, 8, 12, 9, "body"),
    (19, 8, 8, 8, "wings (both share this)"),
    (2, 18, 8, 9, "long tail/leg feathers"),
    (14, 18, 4, 3, "feet"),
]

BAND_HUE = {"primary": 0.0, "secondary": 230.0, "accent": 55.0}


def font(size: int = 14):
    for path in (r"C:\Windows\Fonts\segoeui.ttf", r"C:\Windows\Fonts\arial.ttf"):
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                pass
    return ImageFont.load_default()


def load_base() -> Image.Image:
    """The vanilla base texture, cached into tools/vanilla/ so it never changes under you."""
    cache = os.path.join(VANILLA_CACHE, BASE_TEXTURE)
    if os.path.exists(cache):
        return Image.open(cache).convert("RGBA")
    if not os.path.exists(MC_JAR):
        raise SystemExit(
            f"Could not find the Minecraft jar at:\n  {MC_JAR}\n"
            "Edit MC_JAR at the top of this script (or copy the vanilla PNG into tools/vanilla/).")
    with zipfile.ZipFile(MC_JAR) as jar:
        raw = jar.read("assets/minecraft/textures/entity/parrot/" + BASE_TEXTURE)
    os.makedirs(VANILLA_CACHE, exist_ok=True)
    with open(cache, "wb") as handle:
        handle.write(raw)
    print(f"extracted vanilla base -> {os.path.relpath(cache, PROJECT)}")
    return Image.open(io.BytesIO(raw)).convert("RGBA")


def hue_distance(a: float, b: float) -> float:
    d = abs(a - b) % 360.0
    return min(d, 360.0 - d)


def band_of(r: int, g: int, b: int):
    """Which colour family a pixel belongs to, or None for greys/white/black (left untouched)."""
    h, s, _ = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if s < 0.15:
        return None
    d = h * 360.0
    if d < 30 or d > 330:
        return "primary"
    if 185 < d < 275:
        return "secondary"
    if 35 < d < 75:
        return "accent"
    return min(BAND_HUE, key=lambda key: hue_distance(d, BAND_HUE[key]))


def recolour(base: Image.Image, targets):
    """Swap the red/blue/yellow families of the vanilla texture for the target colours."""
    out = base.copy()
    width, height = out.size
    pixels = out.load()

    families = {name: [] for name in BAND_HUE}
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            band = band_of(r, g, b)
            if band is not None:
                families[band].append((x, y, r, g, b))

    stats = {}
    for name, group in families.items():
        if not group:
            continue
        tr, tg, tb = targets[name]
        th, ts, tv = colorsys.rgb_to_hsv(tr / 255, tg / 255, tb / 255)
        sats, vals = [], []
        for (_, _, r, g, b) in group:
            _, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            sats.append(s)
            vals.append(v)
        sats.sort()
        s_median = sats[len(sats) // 2] or 1e-6
        v_max = max(vals) or 1e-6

        for (x, y, r, g, b) in group:
            _, ps, pv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            new_s = max(0.15, min(1.0, ts * (ps / s_median)))
            new_v = max(0.15, min(1.0, tv * (pv / v_max)))
            nr, ng, nb = colorsys.hsv_to_rgb(th, new_s, new_v)
            pixels[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), 255)

        stats[name] = len(group)

    return out, stats


def preview(sheets):
    scale = 8
    tiles = [(label, image) for label, image in sheets]
    gap = 8
    width = gap + sum(image.width * scale + gap for _, image in tiles)
    height = max(image.height for _, image in tiles) * scale + 34
    canvas = Image.new("RGBA", (width, height), (26, 28, 34, 255))
    draw = ImageDraw.Draw(canvas)
    label_font = font(15)
    x = gap
    for label, image in tiles:
        big = image.resize((image.width * scale, image.height * scale), Image.NEAREST)
        canvas.alpha_composite(big, (x, 30))
        draw.text((x, 8), label, fill=(235, 235, 240, 255), font=label_font)
        x += big.width + gap
    return canvas


def texture_guide(base: Image.Image):
    scale = 16
    width, height = base.size
    art = base.resize((width * scale, height * scale), Image.NEAREST)
    column = 380
    canvas = Image.new("RGBA", (width * scale + column, max(height * scale, 470)), (26, 28, 34, 255))
    canvas.alpha_composite(art, (0, 0))
    draw = ImageDraw.Draw(canvas)
    label_font = font(14)
    head_font = font(17)

    for x in range(0, width * scale + 1, scale):
        draw.line([(x, 0), (x, height * scale)], fill=(255, 255, 255, 28))
    for y in range(0, height * scale + 1, scale):
        draw.line([(0, y), (width * scale, y)], fill=(255, 255, 255, 28))
    for (u, v, w, h, _) in PARTS:
        draw.rectangle([u * scale, v * scale, (u + w) * scale - 1, (v + h) * scale - 1],
                       outline=(255, 96, 96, 255), width=2)

    x = width * scale + 14
    draw.text((x, 10), "parrot_red_blue.png (vanilla base, 32x32)", fill=(255, 255, 255, 255), font=head_font)
    y = 40
    for (u, v, w, h, label) in PARTS:
        draw.text((x, y), f"({u:>2},{v:>2}) {w:>2}x{h:<2}  {label}", fill=(230, 230, 235, 255), font=label_font)
        y += 19
    y += 10
    legend = [
        ("red pixels", "= body colour  -> primary"),
        ("blue pixels", "= wing / tail  -> secondary"),
        ("yellow pixels", "= accents      -> accent"),
        ("dark + greys", "= outline, beak, eye, feet - leave them"),
        ("white area", "= unused canvas, no part samples it"),
    ]
    for name, meaning in legend:
        draw.text((x, y), name, fill=(255, 210, 120, 255), font=label_font)
        draw.text((x + 110, y), meaning, fill=(215, 215, 220, 255), font=label_font)
        y += 19
    y += 8
    draw.text((x, y), "boxes also contain a top/bottom strip, so keep the", fill=(160, 160, 170, 255), font=label_font)
    draw.text((x, y + 18), "shading pattern - recolouring by hue keeps it intact.", fill=(160, 160, 170, 255), font=label_font)
    return canvas


def make_icon(arid: Image.Image):
    head = arid.crop((2, 1, 22, 8))
    head = head.resize((head.width * 6, head.height * 6), Image.NEAREST)
    icon = Image.new("RGBA", (128, 128), (31, 36, 48, 255))
    icon.alpha_composite(head, ((128 - head.width) // 2, (128 - head.height) // 2))
    icon.save(ICON_PATH)
    print(f"wrote {os.path.relpath(ICON_PATH, PROJECT)}")


def main():
    base = load_base()
    os.makedirs(TEX_DIR, exist_ok=True)
    sheets = [("vanilla base (red_blue)", base)]
    arid = None

    for name, (primary, secondary, accent) in SKINS.items():
        out, stats = recolour(base, {"primary": primary, "secondary": secondary, "accent": accent})
        path = os.path.join(TEX_DIR, f"parrot_{name}.png")
        out.save(path)
        print(f"wrote {os.path.relpath(path, PROJECT)}   pixels changed: {stats}")
        sheets.append((name, out))
        if name == "arid":
            arid = out

    preview_path = os.path.join(HERE, "preview.png")
    preview(sheets).save(preview_path)
    print(f"wrote {os.path.relpath(preview_path, PROJECT)}")

    guide_path = os.path.join(HERE, "texture-guide.png")
    texture_guide(base).save(guide_path)
    print(f"wrote {os.path.relpath(guide_path, PROJECT)}")

    if arid is not None:
        make_icon(arid)


if __name__ == "__main__":
    main()
