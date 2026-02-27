#!/usr/bin/env bash
# Download MBTiles for a UK region and place in the tiles directory.
# Usage: ./scripts/download_tiles.sh [region]
#
# The tiles directory is excluded from git. Place downloaded .mbtiles files
# in app/src/main/assets/tiles/ for bundling, or push to device manually:
#   adb push london.mbtiles /sdcard/Android/data/com.clearpath/files/tiles/london.mbtiles
#
# Requires: curl, wget

set -euo pipefail

REGION="${1:-london}"
TILES_DIR="tiles"
mkdir -p "$TILES_DIR"

declare -A URLS
# Mirror — adjust to a reliable CDN or self-hosted tile server.
# openstreetmap.fr provides MBTiles extracts for major cities:
BASE="https://download.maptiles.openstreetmap.fr/mbtiles"
URLS[london]="${BASE}/london.mbtiles"
URLS[manchester]="${BASE}/manchester.mbtiles"
URLS[birmingham]="${BASE}/birmingham.mbtiles"
URLS[edinburgh]="${BASE}/edinburgh.mbtiles"
URLS[bristol]="${BASE}/bristol.mbtiles"
URLS[leeds]="${BASE}/leeds.mbtiles"

URL="${URLS[$REGION]:-}"
if [[ -z "$URL" ]]; then
  echo "Unknown region: $REGION"
  echo "Available: london, manchester, birmingham, edinburgh, bristol, leeds"
  exit 1
fi

OUT="$TILES_DIR/${REGION}.mbtiles"
echo "Downloading $REGION tiles from $URL"
echo "Output: $OUT"
echo ""

wget -c --progress=bar -O "$OUT" "$URL"

echo ""
echo "Download complete: $OUT"
echo ""
echo "To push to a connected Android device:"
echo "  adb push $OUT /sdcard/Android/data/com.clearpath/files/tiles/${REGION}.mbtiles"
echo ""
echo "Or import via the app's Region Download screen -> Import custom .mbtiles"
