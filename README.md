# ClearPath

Offline-first Android route planning with CCTV and surveillance camera awareness. Plan walking, cycling, or driving routes that minimise exposure to known surveillance infrastructure.

No accounts. No analytics. No persistent network requirement after initial data download.

---

## What it does

ClearPath overlays publicly available surveillance camera data from OpenStreetMap onto an offline map, then uses that data to score and colour-code route alternatives by how much camera coverage they pass through.

**Plan a route** — enter an origin and destination (search, tap, or long-press). The app requests up to three route alternatives from OSRM and scores each one against every camera in the dataset. Routes are colour-coded green (low), amber (medium), or red (high) exposure, with individual segments highlighted where they pass through coverage zones.

**Understand the cameras** — tap any camera marker to see its type, operator, coverage radius, direction, and OSM source. Each camera type has an educational card explaining what it records, how long footage is retained, who can access it, and the relevant UK legislation.

**Navigate live** — start turn-by-turn navigation on a chosen route. The app warns you as you approach and enter surveillance zones, tracks how many seconds you have spent under coverage, and shows a live HUD with distance to the next camera.

**Tag cameras yourself** — place markers for cameras you spot that are not yet in the dataset. User-tagged cameras are stored locally and can be exported as GeoJSON to share with others.

**Review journeys** — after completing a navigated route, a debrief screen summarises what different camera types along your route likely recorded, what was not captured, and an exposure timeline chart showing intensity at each point on the route.

**Understand the data** — a statistics dashboard shows total time under surveillance across all recorded journeys, camera type breakdown, most-surveilled route, and ANPR-specific callouts with plain-English explanations.

---

## Camera types

| Type | Colour | Notes |
|---|---|---|
| ANPR | Red | Automatic Number Plate Recognition — logs plate, location, and timestamp to police databases |
| Fixed CCTV | Orange | Fixed field of view, typically 28-day retention |
| PTZ | Amber | Pan-tilt-zoom, may be actively monitored, unpredictable coverage direction |
| Dome | Amber | Obscured lens direction — assume full circular coverage |
| Unknown | Grey | Unclassified surveillance node from OSM |

Directional cameras (where OSM includes a `direction` tag) are rendered as coverage cones. Cameras without a known direction show full circular coverage zones. Opacity reflects confidence level.

---

## Tech stack

- **Language** — Kotlin
- **UI** — Jetpack Compose with osmdroid `MapView` wrapped in `AndroidView`
- **Maps** — osmdroid 6.1.18, offline MBTiles tile source
- **Routing** — OSRM HTTP API (MVP); swap point clearly documented in `RoutingEngine.kt` for GraphHopper full-offline
- **Camera data** — OpenStreetMap Overpass API, cached in Room, bundled snapshot included
- **Database** — Room (camera nodes, saved routes, geocoding cache)
- **Background work** — WorkManager (tile download, Overpass sync)
- **Geocoding** — Nominatim with 1,100ms rate limiter and permanent Room cache
- **Charts** — Vico for the exposure timeline
- **Architecture** — MVVM + Repository, StateFlow, coroutines throughout

---

## Project structure

```
app/src/main/kotlin/com/clearpath/
├── data/
│   ├── camera/          Room entity, DAO, repository
│   ├── route/           SavedRoute entity, DAO, repository
│   └── tiles/           MBTilesArchive (IArchiveFile), TileDownloadWorker
├── map/                 MapViewModel, osmdroid overlay managers, UserLocationManager
├── routing/             RoutingEngine, ExposureCalculator, NavigationEngine
├── overpass/            Ktor client, JSON parser, WorkManager sync worker
├── geocoding/           Nominatim client, Room-backed cache
├── export/              GeoJSON export, debrief image share
├── util/                GeoUtils (Haversine, polyline sampling), ConeContainment
└── ui/
    ├── map/             OsmMapView (AndroidView wrapper), SearchBar, FABs, overlays
    ├── routing/         RouteComparisonSheet, ExposureTimelineChart, NavigationHUD
    ├── tagging/         TagCameraSheet
    ├── dashboard/       StatsScreen + StatsViewModel
    ├── education/       CameraTypeCard, CameraEducationScreen, RouteDebriefScreen
    ├── download/        RegionDownloadScreen + DownloadViewModel
    ├── navigation/      NavGraph (Map, Download, Stats, Education)
    └── theme/           Colors, Typography (JetBrains Mono), Theme
```

---

## Exposure scoring

For each route, the app samples the polyline every 5 metres. At each sample point it queries a grid-based spatial index (50m cells) to find nearby cameras, checks whether the point falls within each camera's coverage radius and directional cone, then accumulates a weighted score:

```
contribution = (coverageRadius - distance) / coverageRadius * confidence
```

The raw sum is normalised to a 0–100 scale. Route segments are coloured individually so you can see exactly where exposure is concentrated.

Directional cone containment handles the 0°/360° wrap-around case so cameras facing north are handled correctly.

---

## Building

Requires Java 21. Gradle 8.7 is incompatible with Java 25 (the Fedora default as of early 2026).

```bash
# Build debug APK and install to connected device
./scripts/build_and_install.sh

# Build only
./scripts/build_and_install.sh build

# Build, install, and launch with logcat
./scripts/build_and_install.sh launch
```

Manual build:
```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## First-run data setup

The app ships with a small bundled GeoJSON snapshot (a few sample nodes) so it opens without errors. For real data:

**Camera data** — fetch live OSM surveillance nodes for a region:
```bash
./scripts/fetch_overpass_snapshot.sh london
```
This queries the Overpass API and writes `app/src/main/assets/bundled/uk_cameras_snapshot.geojson`. Rebuild and reinstall to bundle the updated data. Alternatively, use the in-app Overpass sync button to pull fresh data over the network at any time.

**Map tiles** — download MBTiles for offline map rendering:
```bash
./scripts/download_tiles.sh london
```
Then push to the device:
```bash
adb push tiles/london.mbtiles /sdcard/Android/data/com.clearpath/files/tiles/london.mbtiles
```
Or import via the app's Region Download screen. Without tiles the app falls back to Mapnik online tiles.

Available regions: `london`, `manchester`, `birmingham`, `edinburgh`, `bristol`, `leeds`.

---

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Overpass sync, OSRM routing, Nominatim geocoding — not required for offline use |
| `ACCESS_FINE_LOCATION` | GPS for live navigation and "my location" dot |
| `ACCESS_COARSE_LOCATION` | Fallback location |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION` | Navigation mode keeps running with screen off |
| `POST_NOTIFICATIONS` | Zone entry/exit alerts during navigation |
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_VISUAL_USER_SELECTED` | MBTiles file import |

No contacts, microphone, camera, or persistent background data collection.

---

## Milestones

| Tag | Description |
|---|---|
| `v0.1-scaffold` | Complete project scaffold, all source files, builds and installs |
| `v0.2-cameras` | *(planned)* Camera overlay from live Overpass + bundled data |
| `v0.3-routing` | *(planned)* Route planning with polyline display |
| `v0.4-exposure` | *(planned)* Exposure scoring and colour-coded routes |
| `v0.5-nav` | *(planned)* Live navigation with zone alerts |
| `v0.6-social` | *(planned)* Camera tagging, GeoJSON export, alias integration |
| `v1.0-beta` | *(planned)* Feature complete |

---

## Legal context

All camera data is sourced from OpenStreetMap contributors and reflects publicly documented surveillance infrastructure. The app does not enable any surveillance activity — it helps users understand existing public infrastructure for personal security education and practical OPSEC use.

Relevant UK legislation covered in the in-app education screens: Protection of Freedoms Act 2012, Surveillance Camera Code of Practice (2013), UK GDPR / Data Protection Act 2018, NPCC National ANPR Standards for Policing.
