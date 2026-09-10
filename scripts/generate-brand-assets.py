#!/usr/bin/env python3
"""Generate Android/Play icons from docs/branding/icon.svg (Pillow + librsvg)."""

import argparse
import io
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

from PIL import Image, ImageChops, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/branding/icon.svg"
RES = ROOT / "androidApp/src/main/res"
STORE = ROOT / "fastlane/metadata/android"
ANDROID = "http://schemas.android.com/apk/res/android"
SVG = "http://www.w3.org/2000/svg"
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
ET.register_namespace("android", ANDROID)


def vector(paths, monochrome=False):
    root = ET.Element("vector", {
        f"{{{ANDROID}}}width": "108dp",
        f"{{{ANDROID}}}height": "108dp",
        f"{{{ANDROID}}}viewportWidth": "108",
        f"{{{ANDROID}}}viewportHeight": "108",
    })
    for path in paths:
        ET.SubElement(root, "path", {
            f"{{{ANDROID}}}fillColor": "#FFFFFF" if monochrome else path.attrib["fill"],
            f"{{{ANDROID}}}fillType": "evenOdd" if path.get("fill-rule") == "evenodd" else "nonZero",
            f"{{{ANDROID}}}pathData": path.attrib["d"],
        })
    ET.indent(root, space="    ")
    return b'<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(root) + b"\n"


def render(size):
    result = subprocess.run(
        ["rsvg-convert", "--width", str(size), "--height", str(size), str(SOURCE)],
        check=True, capture_output=True,
    )
    return Image.open(io.BytesIO(result.stdout)).convert("RGBA")


def png(image):
    output = io.BytesIO()
    image.save(output, format="PNG", optimize=True)
    return output.getvalue()


def masked(image, circular):
    mask = Image.new("L", image.size)
    draw = ImageDraw.Draw(mask)
    edge = image.width - 1
    if circular:
        draw.ellipse((0, 0, edge, edge), fill=255)
    else:
        draw.rounded_rectangle((0, 0, edge, edge), radius=image.width * 0.22, fill=255)
    result = image.copy()
    result.putalpha(mask)
    return result


def outputs():
    source = ET.parse(SOURCE).getroot()
    paths = source.findall(f"{{{SVG}}}path")
    if len(paths) != 3 or paths[0].get("id") != "background":
        raise ValueError("Expected background, certificate, and key paths in the source SVG")
    # No unsupported SVG transforms, strokes, or masks: the same path data goes to Android.
    allowed = {"id", "fill", "fill-rule", "d"}
    for path in paths:
        if set(path.attrib) - allowed:
            raise ValueError("Only flat filled paths are supported by the Android exporter")
    result = {
        RES / "drawable/ic_launcher_background.xml": vector(paths[:1]),
        RES / "drawable/ic_launcher_foreground.xml": vector(paths[1:]),
        RES / "drawable/ic_launcher_monochrome.xml": vector(paths[1:], monochrome=True),
    }
    store_icon = render(512)
    if store_icon.getchannel("A").getextrema() != (255, 255):
        raise ValueError("The Play icon must be full-bleed and opaque")
    for locale in ("en-US", "zh-CN"):
        result[STORE / locale / "images/icon.png"] = png(store_icon)
    # Render large before masking/downsampling so legacy edges remain anti-aliased.
    large = render(1024)
    for circular in (False, True):
        image = masked(large, circular)
        name = "ic_launcher_round.png" if circular else "ic_launcher.png"
        for density, size in DENSITIES.items():
            result[RES / f"mipmap-{density}" / name] = png(
                image.resize((size, size), Image.Resampling.LANCZOS)
            )
    return result


def same_asset(path, content):
    if not path.exists():
        return False
    if path.suffix != ".png":
        return path.read_bytes() == content
    with Image.open(path) as actual, Image.open(io.BytesIO(content)) as expected:
        if actual.size != expected.size or actual.mode != expected.mode:
            return False
        return all(channel.getbbox() is None for channel in ImageChops.difference(actual, expected).split())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check generated assets without writing")
    args = parser.parse_args()
    if not shutil.which("rsvg-convert"):
        parser.error("rsvg-convert is required (macOS: brew install librsvg)")
    assets = outputs()
    stale = [path for path, content in assets.items() if not same_asset(path, content)]
    if args.check:
        if stale:
            for path in stale:
                print(f"Out of date: {path.relative_to(ROOT)}")
            raise SystemExit(1)
        print(f"OK: all {len(assets)} generated assets match {SOURCE.relative_to(ROOT)}")
        return
    # Render everything successfully before replacing any existing asset.
    for path, content in assets.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile(dir=path.parent, delete=False) as temp:
            temp.write(content)
            temp_path = Path(temp.name)
        temp_path.chmod(0o644)
        temp_path.replace(path)
    print(f"Generated {len(assets)} assets from {SOURCE.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
