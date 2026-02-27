#!/usr/bin/env bash
# Fetch surveillance camera data from Overpass API and bundle as GeoJSON.
# Usage: ./scripts/fetch_overpass_snapshot.sh [region]
# Regions: london, manchester, birmingham, edinburgh, bristol, leeds, all
#
# Output: app/src/main/assets/bundled/uk_cameras_snapshot.geojson
#
# Requires: curl, jq (brew install jq / apt install jq)

set -euo pipefail

REGION="${1:-london}"
OUT="app/src/main/assets/bundled/uk_cameras_snapshot.geojson"
ENDPOINT="https://overpass-api.de/api/interpreter"
FALLBACK="https://overpass.kumi.systems/api/interpreter"

declare -A BBOXES
BBOXES[london]="51.28,-0.51,51.69,0.33"
BBOXES[manchester]="53.35,-2.38,53.62,-1.96"
BBOXES[birmingham]="52.38,-2.05,52.58,-1.72"
BBOXES[edinburgh]="55.88,-3.36,56.00,-3.09"
BBOXES[bristol]="51.39,-2.72,51.54,-2.49"
BBOXES[leeds]="53.74,-1.67,53.88,-1.43"

if [[ "$REGION" == "all" ]]; then
  BBOX="49.85,-8.65,60.85,1.78"  # Rough GB bounding box
else
  BBOX="${BBOXES[$REGION]:-}"
  if [[ -z "$BBOX" ]]; then
    echo "Unknown region: $REGION. Use: london, manchester, birmingham, edinburgh, bristol, leeds, all"
    exit 1
  fi
fi

QUERY="[out:json][timeout:90];(node[\"man_made\"=\"surveillance\"](${BBOX});node[\"man_made\"=\"camera\"](${BBOX});node[\"surveillance\"](${BBOX}););out body;"

echo "Fetching surveillance data for region: $REGION (bbox: $BBOX)"

# Try primary endpoint, fall back to secondary
RAW=$(curl -s --max-time 120 -X POST "$ENDPOINT" --data-urlencode "data=$QUERY" 2>/dev/null) \
  || RAW=$(curl -s --max-time 120 -X POST "$FALLBACK" --data-urlencode "data=$QUERY")

if [[ -z "$RAW" ]]; then
  echo "ERROR: No response from Overpass API"
  exit 1
fi

ELEMENT_COUNT=$(echo "$RAW" | jq '.elements | length' 2>/dev/null || echo 0)
echo "Got $ELEMENT_COUNT surveillance nodes"

# Convert Overpass JSON to GeoJSON FeatureCollection
echo "$RAW" | jq '{
  type: "FeatureCollection",
  metadata: {
    generated: now | todate,
    source: "OpenStreetMap Overpass API",
    region: "'"$REGION"'",
    element_count: (.elements | length)
  },
  features: [
    .elements[] |
    {
      type: "Feature",
      geometry: {
        type: "Point",
        coordinates: [.lon, .lat]
      },
      properties: (
        { id: .id } + .tags
      )
    }
  ]
}' > "$OUT"

echo "Written to $OUT"
echo "Run the app to seed this data into Room on next launch."
