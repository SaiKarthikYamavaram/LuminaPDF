# LuminaPDF

A PDF reader built with **Kotlin Multiplatform** and **Compose Multiplatform**. One shared codebase runs on **Android** and **desktop** (Linux, Windows, macOS). iOS and web targets are scaffolded but do not render PDFs yet.

The rendering engine is written as a reusable library, separate from the app: the reader is one host application built on top of it.

<!-- Screenshots: add Android and desktop screenshots here, e.g. docs/screenshots/android-viewer.png -->

## Features

- **Viewer:** continuous scrolling with zoom, pan and fling; tiled rendering at high zoom; page prefetching; auto-hiding toolbar; jump to page
- **Search and annotations:** full-text search, tap a match to highlight it, tap a highlight to remove it; per-document notes
- **Navigation:** the PDF's own outline plus user bookmarks
- **Password-protected PDFs** on Android and desktop
- **Library:** recent files, stored documents with folders, rename, and storage usage
- **Export:** save a copy of the open document
- **Theming and accessibility:** light and dark themes, reading mode, and screen-reader semantics across the home screen and viewer
- **Monitoring:** crash and session reporting through Kotzilla

## Architecture

```
:pdf-core       Pure Kotlin contracts — no UI, no platform code
:pdf-renderer   Platform renderers (expect/actual)
:pdf-viewer     Compose Multiplatform viewer component + persistence
:shared         The reader app: home screen, DI, file handling
:androidApp  :desktopApp  :webApp   Platform entry points
```

| Module | What it contains |
|---|---|
| `pdf-core` | `PdfDocument` and `PdfBitmap` abstractions, a prioritized `RenderQueue` (page, tile, thumbnail), and a two-level cache (`LruMemoryCache` + disk cache) |
| `pdf-renderer` | Android: `android.graphics.pdf.PdfRenderer`. Desktop: Apache PDFBox. iOS (PDFKit) and web (pdf.js) are planned. |
| `pdf-viewer` | The `PdfViewer()` composable, `PdfViewerState` (zoom, pan, visible pages), `PdfViewerConfig`, and SQLDelight storage for highlights, notes and bookmarks |
| `shared` | The reader app built on the modules above: home screen, Koin setup, FileKit file picking |

Each layer depends only on the ones below it, so the renderer and viewer can be dropped into another app without bringing the reader's features along.

## Tech stack

Kotlin 2.4 · Compose Multiplatform 1.11 · Coroutines · Koin 4 · SQLDelight 2 · FileKit · Kermit · Apache PDFBox 3 · Kotzilla · Dokka

Android: minSdk 24, targetSdk 36.

## Running

- **Android:** `./gradlew :androidApp:assembleDebug`
- **Desktop:** `./gradlew :desktopApp:run` (hot reload: `./gradlew :desktopApp:hotRun --auto`)

## Release builds

- **Android APK / App Bundle:** `./gradlew :androidApp:assembleRelease` / `./gradlew :androidApp:bundleRelease`
  Signing needs a `keystore.properties` file in `androidApp/` (`keyAlias`, `keyPassword`, `storeFile`, `storePassword`) or the `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_FILE`, `STORE_PASSWORD` environment variables. Without them the build is unsigned.
- **Desktop installer for the current OS:** `./gradlew :desktopApp:packageReleaseDistributionForCurrentOS`
  Or a specific format: `packageDmg`, `packageMsi`, `packageExe`, `packageDeb`.

## Tests

- Desktop (JVM), all modules: `./gradlew jvmTest`
- Android host tests: `./gradlew :shared:testAndroidHostTest`

Covered today: the LRU cache, opening password-protected PDFs, and highlight persistence.

## Roadmap

- iOS renderer on PDFKit
- Web renderer on pdf.js
- Text selection
- Share sheet and native printing
