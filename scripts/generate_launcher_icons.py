"""Generate Android launcher icons from app icon source PNG."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "scripts" / "assets" / "ic_launcher_source.png"
RES = ROOT / "app" / "src" / "main" / "res"

MIPMAP_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

FOREGROUND_SIZES = {
    "drawable-mdpi": 108,
    "drawable-hdpi": 162,
    "drawable-xhdpi": 216,
    "drawable-xxhdpi": 324,
    "drawable-xxxhdpi": 432,
}


def sample_background_color(img: Image.Image) -> tuple[int, int, int]:
    rgba = img.convert("RGBA")
    w, h = rgba.size
    inset = max(w, h) // 8
    points = [
        (inset, h // 2),
        (w - inset - 1, h // 2),
        (w // 2, inset),
        (w // 2, h - inset - 1),
    ]
    samples: list[tuple[int, int, int]] = []
    for x, y in points:
        r, g, b, a = rgba.getpixel((x, y))
        if a > 200:
            samples.append((r, g, b))
    if not samples:
        return (13, 34, 63)
    return (
        sum(p[0] for p in samples) // len(samples),
        sum(p[1] for p in samples) // len(samples),
        sum(p[2] for p in samples) // len(samples),
    )


def resize_square(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    if not SOURCE.exists():
        raise SystemExit(f"Source not found: {SOURCE}")

    src = Image.open(SOURCE).convert("RGBA")
    bg_rgb = sample_background_color(src)
    bg_hex = f"#{bg_rgb[0]:02X}{bg_rgb[1]:02X}{bg_rgb[2]:02X}"

    # Legacy launcher icons (pre-API 26 fallback in mipmap folders)
    for folder, size in MIPMAP_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = resize_square(src, size)
        icon.save(out_dir / "ic_launcher.png", "PNG")
        icon.save(out_dir / "ic_launcher_round.png", "PNG")

    # Adaptive icon foreground layers
    for folder, size in FOREGROUND_SIZES.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        resize_square(src, size).save(out_dir / "ic_launcher_foreground.png", "PNG")

    colors_path = RES / "values" / "ic_launcher_colors.xml"
    colors_path.write_text(
        f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">{bg_hex}</color>
</resources>
""",
        encoding="utf-8",
    )

    print(f"Background color: {bg_hex}")
    print("Launcher icons generated.")


if __name__ == "__main__":
    main()
