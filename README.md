# Tinta - native Android manhwa reader

Kotlin, Jetpack Compose (customised dark Material 3), MVVM + repository, Retrofit/OkHttp/kotlinx.serialization,
Coil, Room, DataStore, Navigation Compose. Min SDK 26, compile/target SDK 35.

## Status (read this first)

* The Gradle/Compose project was **written but not compiled or run** in the authoring environment (no Android SDK, no
  network). Expect to fix a few compile errors on first sync. Nothing here has been tested on a device.
* API integration is based on the server source in `manhwa-api.zip`, not on live calls. See `docs/VERIFIED_ENDPOINTS.md`.
* No reference screenshots were available; the UI follows the written design tokens in the brief.

## Build

1. Open this folder in **Android Studio Ladybug (2024.2) or newer**, JDK 17. Let it sync. The wrapper jar is not
   included; Android Studio will use the bundled Gradle, or run `gradle wrapper --gradle-version 8.9` once.
2. `./gradlew assembleDebug` (APK in `app/build/outputs/apk/debug/`).
3. Unit tests: `./gradlew testDebugUnitTest`. UI smoke test (needs emulator/device): `./gradlew connectedDebugAndroidTest`.

Configuration (all in `app/build.gradle.kts`, overridable in `~/.gradle/gradle.properties`, never committed):

```
TINTA_BASE_URL=https://tets-server-production.up.railway.app/
TINTA_API_KEY=            # only if the server sets API_KEY
```

App name: `resValue("string", "app_name", ...)`. Logo: `TintaLogo` composable + `ic_launcher_foreground.xml`.

## Structure

```
data/remote/{api,dto}   Retrofit interface + DTOs matching the server JSON
data/local              Room (bookmarks, history, recent searches) + DataStore (reader settings)
data/mapper             DTO -> domain (drops items without an id, never fills missing metadata)
data/repository         CatalogRepository (network), LibraryRepository (local)
domain/{model,usecase}  Models; ResolveReadTarget (resume, else first chapter)
ui/{theme,components}   Design tokens, shared composables, paging helper
ui/screens/*            home, explore, search (+ view all), detail, reader, library, profile
ui/navigation           Routes (IDs are URI-encoded) and the back stack rules
util                    Error mapping, all UI strings (Strings.kt), small helpers
```

## Behaviour notes

* Home: hero = first 5 items of the server's `popular` list with a cover; sections "Update Terbaru", "Populer",
  "Baru Ditambahkan" come straight from `/api/home`. Continue Reading appears only when local history exists.
* Reader: vertical long-strip, original image ratios, tap toggles controls and system bars, per-page retry,
  progress saved (debounced) to Room and restored on reopen. Prev/next use the server's neighbours.
* Novel chapters (source `noveldex`) render as text.
* All UI text is in `util/Strings.kt` (Indonesian, matching the sources); move to `strings.xml` to localise.

## Known gaps

* Hero "Read" and the detail "Mulai baca" fetch the detail first to learn the chapter key (one extra request).
* Scroll restore in the reader is approximate because page heights are unknown until images load.
* Content comes from third-party sites via the server; check their terms before distributing the app.
