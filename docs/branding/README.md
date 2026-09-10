# Android and Google Play branding

The identity is a white signing certificate with an integrated teal key on indigo.
It represents signature inspection, not antivirus protection or password storage.

- Background: `#252B60`
- Certificate: `#FFFFFF`
- Key: `#5EEAD4`
- Source: [`icon.svg`](icon.svg)

## Regenerate icons

Requirements: Python 3, Pillow, and `rsvg-convert` (librsvg). On macOS, install
librsvg with `brew install librsvg`; install Pillow in your preferred Python
virtual environment with `python -m pip install Pillow`.

From the repository root:

```sh
python3 scripts/generate-brand-assets.py
python3 scripts/generate-brand-assets.py --check
```

Edit the SVG, not the generated PNG/XML files. Its three flat filled paths share
Android's 108 × 108 coordinate space. The SVG viewBox displays the central
72 × 72 area for the Play/legacy icon; Android receives the full 108 × 108 layers
for adaptive masking and motion. Keep the mark in the central 66dp safe zone.
Transforms, strokes, and SVG masks are not supported by the exporter.

Generated outputs:

- `androidApp/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml`
- `androidApp/src/main/res/mipmap-*/ic_launcher{,_round}.png` (48–192px)
- `fastlane/metadata/android/{en-US,zh-CN}/images/icon.png` (512 × 512)

The Play icon is an opaque full square with no baked corner radius or shadow.
Only the pre-Android-8 legacy PNGs have baked rounded-square/circle masks.
The monochrome vector has real transparent cutouts, not background-colored
patches, so Android 13+ themed icons can tint it correctly.

`--check` renders the source again and compares PNG pixels and vector contents
without modifying files. A nonzero exit code means an asset is missing or stale.
Rasterization differences across librsvg/Pillow versions may require regeneration.

Desktop assets remain unchanged; this refresh is scoped to Android and Play.

## Store listing

English and Simplified Chinese title, short description, and full description
live in `fastlane/metadata/android/en-US/` and `zh-CN/`. Existing Russian copy is
preserved. Store titles have a descriptive subtitle; the installed app label
remains **KeyStoreViewer**.

Limits: title 30 characters, short description 80 characters, full description
4,000 characters. Only confirmed core features are advertised; the listing does
not promise offline-only operation, absence of ads, or app safety verification.

### Publication is a separate step

The existing `fastlane play_upload` lane uploads the AAB but **skips metadata,
images, and screenshots**. A normal app release will not publish these listing
changes. This refresh does not change that automation or upload anything.

To publish manually, open the app's main store listing in Google Play Console:

1. Select English (United States), replace the three text fields from `en-US`,
   and upload `en-US/images/icon.png` as the app icon.
2. Add/select Simplified Chinese (`zh-CN`), use its text files, and upload the
   identical `zh-CN/images/icon.png` where localized graphics are requested.
3. Preview both listings and submit the changes through the normal review flow.

Screenshots and feature graphics have not been replaced. The installed launcher
icon changes only when users install an APK/AAB containing the new resources.
