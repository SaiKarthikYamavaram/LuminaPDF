# Graph Report - LuminaPDF  (2026-07-02)

## Corpus Check
- 98 files · ~63,043 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 849 nodes · 1288 edges · 81 communities (65 shown, 16 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 113 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `2513803b`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Mutablelist|Mutablelist]]
- [[_COMMUNITY_Pdf Viewer Src Commonmain|Pdf Viewer Src Commonmain]]
- [[_COMMUNITY_Pdf Renderer Src Androidmain|Pdf Renderer Src Androidmain]]
- [[_COMMUNITY_Pdf Platform|Pdf Platform]]
- [[_COMMUNITY_Core Lrumemorycache|Core Lrumemorycache]]
- [[_COMMUNITY_Db Pdfdatabase Jvm|Db Pdfdatabase Jvm]]
- [[_COMMUNITY_Db Pdfdatabase Wasmjs|Db Pdfdatabase Wasmjs]]
- [[_COMMUNITY_Db Pdfdatabase Android|Db Pdfdatabase Android]]
- [[_COMMUNITY_Db Pdfdatabase Ios|Db Pdfdatabase Ios]]
- [[_COMMUNITY_Core Pdfdocument|Core Pdfdocument]]
- [[_COMMUNITY_Db Pdfdatabase|Db Pdfdatabase]]
- [[_COMMUNITY_Db Pdfdatabase Js|Db Pdfdatabase Js]]
- [[_COMMUNITY_Iosapp Contentview|Iosapp Contentview]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Pdf Greeting|Pdf Greeting]]
- [[_COMMUNITY_Bundle|Bundle]]
- [[_COMMUNITY_Pdf Renderer Src Commonmain|Pdf Renderer Src Commonmain]]
- [[_COMMUNITY_Pdf Renderer Src Iosmain|Pdf Renderer Src Iosmain]]
- [[_COMMUNITY_Pdf Renderer Src Jsmain|Pdf Renderer Src Jsmain]]
- [[_COMMUNITY_Pdf Renderer Src Wasmjsmain|Pdf Renderer Src Wasmjsmain]]
- [[_COMMUNITY_App|App]]
- [[_COMMUNITY_Core Pdfbitmap|Core Pdfbitmap]]
- [[_COMMUNITY_Pdf Filepicker|Pdf Filepicker]]
- [[_COMMUNITY_Pdf Filepicker Android|Pdf Filepicker Android]]
- [[_COMMUNITY_Pdf Filepicker Ios|Pdf Filepicker Ios]]
- [[_COMMUNITY_Pdf Filepicker Js|Pdf Filepicker Js]]
- [[_COMMUNITY_Pdf Filepicker Jvm|Pdf Filepicker Jvm]]
- [[_COMMUNITY_Pdf Filepicker Wasmjs|Pdf Filepicker Wasmjs]]
- [[_COMMUNITY_Core Pdfrect|Core Pdfrect]]
- [[_COMMUNITY_Pdf Sharedcommontest|Pdf Sharedcommontest]]
- [[_COMMUNITY_Pdf Sharedlogicandroidhosttest|Pdf Sharedlogicandroidhosttest]]
- [[_COMMUNITY_Pdf Sharedlogicdesktoptest|Pdf Sharedlogicdesktoptest]]
- [[_COMMUNITY_Pdf Sharedlogiciostest|Pdf Sharedlogiciostest]]
- [[_COMMUNITY_Pdf Viewer Src Androidmain|Pdf Viewer Src Androidmain]]
- [[_COMMUNITY_Pdf Viewer Src Commonmain|Pdf Viewer Src Commonmain]]
- [[_COMMUNITY_Pdf Viewer Src Iosmain|Pdf Viewer Src Iosmain]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Pdf Viewer Src Jvmmain|Pdf Viewer Src Jvmmain]]
- [[_COMMUNITY_Core Outlineitem|Core Outlineitem]]
- [[_COMMUNITY_Viewer Pdfviewerconfig|Viewer Pdfviewerconfig]]
- [[_COMMUNITY_Viewer Pdfviewerstate|Viewer Pdfviewerstate]]
- [[_COMMUNITY_Androidapp Build Gradle|Androidapp Build Gradle]]
- [[_COMMUNITY_Community 55|Community 55]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Community 57|Community 57]]
- [[_COMMUNITY_Community 58|Community 58]]
- [[_COMMUNITY_Community 59|Community 59]]
- [[_COMMUNITY_Community 60|Community 60]]
- [[_COMMUNITY_Community 61|Community 61]]
- [[_COMMUNITY_Community 62|Community 62]]
- [[_COMMUNITY_Community 63|Community 63]]
- [[_COMMUNITY_Community 64|Community 64]]
- [[_COMMUNITY_Community 65|Community 65]]
- [[_COMMUNITY_Community 66|Community 66]]
- [[_COMMUNITY_Community 67|Community 67]]
- [[_COMMUNITY_Community 68|Community 68]]
- [[_COMMUNITY_Community 69|Community 69]]
- [[_COMMUNITY_Community 70|Community 70]]
- [[_COMMUNITY_Community 71|Community 71]]
- [[_COMMUNITY_Community 72|Community 72]]
- [[_COMMUNITY_Community 73|Community 73]]
- [[_COMMUNITY_Community 74|Community 74]]
- [[_COMMUNITY_Community 75|Community 75]]
- [[_COMMUNITY_Community 76|Community 76]]
- [[_COMMUNITY_Community 77|Community 77]]
- [[_COMMUNITY_Community 78|Community 78]]
- [[_COMMUNITY_Community 79|Community 79]]
- [[_COMMUNITY_Community 80|Community 80]]

## God Nodes (most connected - your core abstractions)
1. `Modifier` - 28 edges
2. `Color` - 28 edges
3. `PdfViewer()` - 25 edges
4. `ElementBoundsExtractor` - 22 edges
5. `HomeScreen()` - 20 edges
6. `Offset` - 19 edges
7. `TopBar()` - 19 edges
8. `PdfDatabase` - 19 edges
9. `UnifiedLruCache` - 15 edges
10. `JvmPdfDocument` - 14 edges

## Surprising Connections (you probably didn't know these)
- `PdfViewer()` --calls--> `LruMemoryCache`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → pdf-core/src/commonMain/kotlin/com/lumina/pdf/core/LruMemoryCache.kt
- `PdfViewer()` --calls--> `RenderQueue`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → pdf-core/src/commonMain/kotlin/com/lumina/pdf/core/RenderQueue.kt
- `PdfViewer()` --calls--> `PageSlice`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → pdf-core/src/commonMain/kotlin/com/lumina/pdf/core/SearchCorpus.kt
- `PdfViewer()` --calls--> `StripSpatialIndex`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → pdf-core/src/commonMain/kotlin/com/lumina/pdf/core/StripSpatialIndex.kt
- `PdfViewer()` --calls--> `UnifiedLruCache`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → pdf-core/src/commonMain/kotlin/com/lumina/pdf/core/UnifiedLruCache.kt

## Import Cycles
- None detected.

## Communities (81 total, 16 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.08
Nodes (27): PageQuadtree, QuadtreeBounds, PageSlice, SearchCorpus, App(), AppHomeScreen, AppPdfViewerScreen, formatSize() (+19 more)

### Community 1 - "Mutablelist"
Cohesion: 0.08
Nodes (24): Pair, Int, CharacterBox, Float, Int, List, MutableList, org (+16 more)

### Community 2 - "Pdf Viewer Src Commonmain"
Cohesion: 0.11
Nodes (54): com, Offset, formatRelativeTime(), HomeScreen(), RecentFile, GridItemCard(), ListItemRow(), onHover() (+46 more)

### Community 3 - "Pdf Renderer Src Androidmain"
Cohesion: 0.11
Nodes (15): Int, CharacterBox, Float, Int, List, OutlineItem, PdfBitmap, PdfRect (+7 more)

### Community 4 - "Pdf Platform"
Cohesion: 0.12
Nodes (18): AndroidPlatform, getPlatform(), getPlatform(), getPlatform(), IOSPlatform, getPlatform(), JsPlatform, getPlatform() (+10 more)

### Community 5 - "Core Lrumemorycache"
Cohesion: 0.14
Nodes (9): LruMemoryCache, DiskCache, MemoryCache, Long, PdfBitmap, String, ByteArray, PdfBitmap (+1 more)

### Community 6 - "Db Pdfdatabase Jvm"
Cohesion: 0.15
Nodes (11): getCurrentTimeMillis(), JvmPdfDatabase, rememberPdfDatabase(), LuminaDatabase, LuminaDatabaseQueries, DbNote, List, Long (+3 more)

### Community 7 - "Db Pdfdatabase Wasmjs"
Cohesion: 0.09
Nodes (18): DbNote, getCurrentTimeMillis(), PdfDatabase, PdfOffset, rememberPdfDatabase(), getCurrentTimeMillis(), jsDateNow(), rememberPdfDatabase() (+10 more)

### Community 8 - "Db Pdfdatabase Android"
Cohesion: 0.18
Nodes (9): AndroidPdfDatabase, getCurrentTimeMillis(), rememberPdfDatabase(), DbNote, List, Long, PdfOffset, SqlDriver (+1 more)

### Community 9 - "Db Pdfdatabase Ios"
Cohesion: 0.18
Nodes (9): getCurrentTimeMillis(), IosPdfDatabase, rememberPdfDatabase(), DbNote, List, Long, PdfOffset, SqlDriver (+1 more)

### Community 10 - "Core Pdfdocument"
Cohesion: 0.18
Nodes (9): PdfDocument, CharacterBox, Float, Int, List, OutlineItem, PdfBitmap, PdfRect (+1 more)

### Community 11 - "Db Pdfdatabase"
Cohesion: 0.12
Nodes (15): Annotation Architecture, Compose Rendering Strategy, Final Recommendation, High-Level Architecture, KMP PDF Renderer — Production Architecture Plan, Night Mode, Page Virtualization, Persistence (Host App Responsibility) (+7 more)

### Community 12 - "Db Pdfdatabase Js"
Cohesion: 0.06
Nodes (34): 10. Strip-Based Spatial Indexing for Text Selection (Phase 4), 11. Adaptive RDP Vector Path Simplification (Phase 5), 1. Thread-Safe LRU Memory Cache (`LruMemoryCache`), 2. Parallel Render Workers in `RenderQueue`, 3. JVM Tile Cache Optimization (`JvmPdfDocument`), 4. Progressive LOD Thumbnails & Background Warmup, 5. Tile Grouping & Viewport Culling, 6. Android GPU Acceleration & Memory Recycling (+26 more)

### Community 13 - "Iosapp Contentview"
Cohesion: 0.20
Nodes (9): ComposeView, ContentView, SwiftUI, Self, Shared, UIKit, UIViewController, UIViewControllerRepresentable (+1 more)

### Community 14 - "Community 14"
Cohesion: 0.19
Nodes (8): getCurrentTimeMillis(), JsPdfDatabase, rememberPdfDatabase(), DbNote, List, Long, PdfOffset, String

### Community 15 - "Pdf Greeting"
Cohesion: 0.29
Nodes (4): Greeting, sayHello(), String, String

### Community 16 - "Bundle"
Cohesion: 0.33
Nodes (3): Bundle, ComponentActivity, MainActivity

### Community 17 - "Pdf Renderer Src Commonmain"
Cohesion: 0.40
Nodes (3): ByteArray, PdfDocument, PdfDocumentFactory

### Community 18 - "Pdf Renderer Src Iosmain"
Cohesion: 0.40
Nodes (3): ByteArray, PdfDocument, PdfDocumentFactory

### Community 19 - "Pdf Renderer Src Jsmain"
Cohesion: 0.40
Nodes (3): ByteArray, PdfDocument, PdfDocumentFactory

### Community 20 - "Pdf Renderer Src Wasmjsmain"
Cohesion: 0.40
Nodes (3): ByteArray, PdfDocument, PdfDocumentFactory

### Community 21 - "App"
Cohesion: 0.40
Nodes (4): App, iOSApp, SwiftUI, Scene

### Community 23 - "Pdf Filepicker"
Cohesion: 0.30
Nodes (4): NSData, FilePersistence, ByteArray, String

### Community 24 - "Pdf Filepicker Android"
Cohesion: 0.31
Nodes (4): FilePersistence, ByteArray, File, String

### Community 25 - "Pdf Filepicker Ios"
Cohesion: 0.31
Nodes (4): FilePersistence, ByteArray, File, String

### Community 26 - "Pdf Filepicker Js"
Cohesion: 0.36
Nodes (3): FilePersistence, ByteArray, String

### Community 27 - "Pdf Filepicker Jvm"
Cohesion: 0.36
Nodes (3): FilePersistence, ByteArray, String

### Community 28 - "Pdf Filepicker Wasmjs"
Cohesion: 0.36
Nodes (3): FilePersistence, ByteArray, String

### Community 34 - "Pdf Viewer Src Androidmain"
Cohesion: 0.10
Nodes (17): BufferedImage, Font, ImagePyramid, PageDrawer, Char, Int, SdfGlyphAtlas, String (+9 more)

### Community 35 - "Pdf Viewer Src Commonmain"
Cohesion: 0.25
Nodes (8): ByteArray, DiskCache, ImageBitmap, PdfBitmap, asImageBitmap(), decodeByteArrayToPdfBitmap(), rememberDiskCache(), toByteArray()

### Community 36 - "Pdf Viewer Src Iosmain"
Cohesion: 0.07
Nodes (28): DiskCache, ByteArray, ImageBitmap, PdfBitmap, String, ByteArray, ImageBitmap, PdfBitmap (+20 more)

### Community 37 - "Community 37"
Cohesion: 0.29
Nodes (7): BoyerMooreHorspool, Map, Boolean, Char, Int, List, String

### Community 38 - "Pdf Viewer Src Jvmmain"
Cohesion: 0.21
Nodes (9): ByteArray, ImageBitmap, PdfBitmap, String, asImageBitmap(), decodeByteArrayToPdfBitmap(), JvmDiskCache, rememberDiskCache() (+1 more)

### Community 55 - "Community 55"
Cohesion: 0.20
Nodes (10): 2. `:pdf-renderer`, 3. `:pdf-viewer`, 4. `:pdf-annotations` (Optional), 5. `:pdf-search` (Optional), 6. `:sample-app`, Android, Desktop (JVM), iOS (+2 more)

### Community 56 - "Community 56"
Cohesion: 0.25
Nodes (7): Module pdf-core, Module pdf-renderer, Module pdf-viewer, Package com.lumina.pdf.core, Package com.lumina.pdf.renderer, Package com.lumina.pdf.viewer, Package com.lumina.pdf.viewer.db

### Community 57 - "Community 57"
Cohesion: 0.32
Nodes (4): RecentFilesStore, List, RecentFile, String

### Community 62 - "Community 62"
Cohesion: 0.26
Nodes (7): CacheEntryType, CacheNode, UnifiedLruCache, Int, Long, PdfBitmap, String

### Community 63 - "Community 63"
Cohesion: 0.25
Nodes (8): Page, RenderPriority, RenderQueue, RenderRequest, Thumbnail, Tile, Int, Set

### Community 65 - "Community 65"
Cohesion: 0.33
Nodes (5): Double, GeneralPath, List, Point2D, RdpPathSimplifier

### Community 66 - "Community 66"
Cohesion: 0.13
Nodes (10): COSName, Float, Int, List, PdfRect, PDImage, Point2D, PDFGraphicsStreamEngine (+2 more)

### Community 67 - "Community 67"
Cohesion: 0.21
Nodes (9): ByteArray, ImageBitmap, PdfBitmap, String, asImageBitmap(), decodeByteArrayToPdfBitmap(), JsNoOpDiskCache, rememberDiskCache() (+1 more)

### Community 68 - "Community 68"
Cohesion: 0.31
Nodes (5): SdfGlyph, SdfGlyphAtlas, Boolean, Char, Float

### Community 69 - "Community 69"
Cohesion: 0.36
Nodes (5): Point2D, SutherlandHodgman, Boolean, Float, List

### Community 70 - "Community 70"
Cohesion: 0.47
Nodes (4): ImagePyramid, PyramidLevel, Float, Int

### Community 71 - "Community 71"
Cohesion: 0.39
Nodes (4): StripSpatialIndex, CharacterBox, Float, List

### Community 73 - "Community 73"
Cohesion: 0.33
Nodes (6): Android, Desktop, iOS, Recommended Tech Stack, Shared (`commonMain`), Web

### Community 74 - "Community 74"
Cohesion: 0.33
Nodes (6): Continuous Scroll, Paged Mode, Phase 1, Phase 2, Two-page spread, Viewer Modes

### Community 75 - "Community 75"
Cohesion: 0.40
Nodes (5): 1. `:pdf-core`, No app features, No platform code, No UI, Responsibilities

### Community 76 - "Community 76"
Cohesion: 0.40
Nodes (5): Phase 1 — MVP Renderer, Phase 2 — Performance, Phase 3 — Viewer UX, Phase 4 — Advanced, Recommended Build Phases

### Community 77 - "Community 77"
Cohesion: 0.50
Nodes (4): Cache Architecture, L1 — Memory Cache, L2 — Disk Cache, L3 — Native Handle Cache

### Community 78 - "Community 78"
Cohesion: 0.50
Nodes (4): Core Design Principle, Native Rendering via `expect/actual`, Shared Interface, Why `renderTile()` matters

### Community 79 - "Community 79"
Cohesion: 0.50
Nodes (4): Integration Tests, Stress Tests, Testing Strategy, Unit Tests

### Community 80 - "Community 80"
Cohesion: 0.67
Nodes (3): Multi-Resolution Rendering, Rendering Pipeline, Smart Zoom Rendering

## Knowledge Gaps
- **209 isolated node(s):** `String`, `Bundle`, `UIKit`, `SwiftUI`, `Shared` (+204 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PdfViewer()` connect `Community 0` to `Mutablelist`, `Pdf Viewer Src Commonmain`, `Core Lrumemorycache`, `Community 71`, `Community 62`, `Community 63`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **Why does `Pair` connect `Mutablelist` to `Community 0`, `Pdf Viewer Src Androidmain`, `Community 62`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Why does `JvmPdfBitmap` connect `Mutablelist` to `Pdf Viewer Src Jvmmain`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Are the 14 inferred relationships involving `PdfViewer()` (e.g. with `App()` and `.Content()`) actually correct?**
  _`PdfViewer()` has 14 INFERRED edges - model-reasoned connections that need verification._
- **Are the 14 inferred relationships involving `HomeScreen()` (e.g. with `App()` and `.Content()`) actually correct?**
  _`HomeScreen()` has 14 INFERRED edges - model-reasoned connections that need verification._
- **What connects `String`, `Bundle`, `UIKit` to the rest of the system?**
  _209 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.07827260458839407 - nodes in this community are weakly interconnected._