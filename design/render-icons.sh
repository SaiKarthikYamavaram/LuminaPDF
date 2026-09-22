#!/usr/bin/env bash
# Renders every platform's app-icon assets from the SVG masters in this directory.
# Requires rsvg-convert (librsvg), ImageMagick 7 (magick) and python3.
#
# Edit lumina-icon.svg / lumina-icon-foreground.svg / lumina-icon-monochrome.svg, then run
# this script. The generated files are committed, so the build needs no extra tooling.
set -euo pipefail

cd "$(dirname "$0")"
root=$(cd .. && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# Corner radius on the 1024 master grid: 224 is the usual ~22% app-icon squircle, 512 is a circle.
SQUIRCLE=224
CIRCLE=512

render() { # svg size out
  rsvg-convert -w "$2" -h "$2" "$1" -o "$3"
}

masked() { # svg size radius out
  rsvg-convert -w "$2" -h "$2" "$1" -o "$tmp/src.png"
  printf '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><rect width="1024" height="1024" rx="%s" fill="#fff"/></svg>' "$3" \
    | rsvg-convert -w "$2" -h "$2" -o "$tmp/mask.png"
  magick "$tmp/src.png" \( "$tmp/mask.png" -alpha extract \) -compose CopyOpacity -composite "$4"
}

echo "Android"
res="$root/androidApp/src/main/res"
# dpi:legacy-size:adaptive-size (adaptive layers are 108dp, legacy launcher icons are 48dp)
for entry in mdpi:48:108 hdpi:72:162 xhdpi:96:216 xxhdpi:144:324 xxxhdpi:192:432; do
  IFS=: read -r dpi legacy adaptive <<<"$entry"
  mkdir -p "$res/mipmap-$dpi"
  masked lumina-icon.svg "$legacy" "$SQUIRCLE" "$res/mipmap-$dpi/ic_launcher.png"
  masked lumina-icon.svg "$legacy" "$CIRCLE" "$res/mipmap-$dpi/ic_launcher_round.png"
  render lumina-icon-foreground.svg "$adaptive" "$res/mipmap-$dpi/ic_launcher_foreground.png"
  render lumina-icon-monochrome.svg "$adaptive" "$res/mipmap-$dpi/ic_launcher_monochrome.png"
done

echo "iOS"
# App Store icons must be square and fully opaque — no alpha channel allowed.
render lumina-icon.svg 1024 "$tmp/ios.png"
magick "$tmp/ios.png" -background '#2A2420' -alpha remove -alpha off \
  "$root/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png"

echo "Desktop"
icons="$root/desktopApp/icons"
mkdir -p "$icons"
masked lumina-icon.svg 1024 "$SQUIRCLE" "$tmp/rounded-1024.png"
masked lumina-icon.svg 512 "$SQUIRCLE" "$icons/lumina.png"
# The running window/taskbar icon is loaded from the classpath, not the packaged distribution.
mkdir -p "$root/desktopApp/src/main/resources"
masked lumina-icon.svg 256 "$SQUIRCLE" "$root/desktopApp/src/main/resources/lumina.png"
magick "$tmp/rounded-1024.png" -define icon:auto-resize=256,128,64,48,32,16 "$icons/lumina.ico"
for size in 32 64 128 256 512 1024; do
  masked lumina-icon.svg "$size" "$SQUIRCLE" "$tmp/icns-$size.png"
done
# ImageMagick here has no ICNS delegate, so pack the PNGs into an .icns container directly.
python3 - "$icons/lumina.icns" "$tmp" <<'PY'
import struct, sys

out, tmp = sys.argv[1], sys.argv[2]
# ICNS element types that take a raw PNG payload, and the pixel size each one expects.
types = [(b"ic11", 32), (b"ic12", 64), (b"ic07", 128), (b"ic08", 256), (b"ic09", 512), (b"ic10", 1024)]

body = b""
for kind, size in types:
    with open(f"{tmp}/icns-{size}.png", "rb") as fh:
        png = fh.read()
    body += kind + struct.pack(">I", len(png) + 8) + png

with open(out, "wb") as fh:
    fh.write(b"icns" + struct.pack(">I", len(body) + 8) + body)
PY

echo "Web"
web="$root/webApp/src/webMain/resources"
sed 's|<rect width="1024" height="1024" fill="url(#ground)"/>|<rect width="1024" height="1024" rx="224" fill="url(#ground)"/>|' \
  lumina-icon.svg > "$web/favicon.svg"
masked lumina-icon.svg 180 "$SQUIRCLE" "$web/apple-touch-icon.png"
masked lumina-icon.svg 32 "$SQUIRCLE" "$web/favicon-32.png"

echo "Done."
