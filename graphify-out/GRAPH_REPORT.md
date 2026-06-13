# Graph Report - LuminaPDF  (2026-06-13)

## Corpus Check
- 79 files · ~51,591 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 556 nodes · 815 edges · 63 communities (44 shown, 19 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 27 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9029bd9a`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Offset|Offset]]
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
- [[_COMMUNITY_Core Renderqueue|Core Renderqueue]]
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
- [[_COMMUNITY_Pdf Viewer Src Jsmain|Pdf Viewer Src Jsmain]]
- [[_COMMUNITY_Pdf Viewer Src Jvmmain|Pdf Viewer Src Jvmmain]]
- [[_COMMUNITY_Pdf Viewer Src Wasmjsmain|Pdf Viewer Src Wasmjsmain]]
- [[_COMMUNITY_Core Outlineitem|Core Outlineitem]]
- [[_COMMUNITY_Viewer Pdfviewerconfig|Viewer Pdfviewerconfig]]
- [[_COMMUNITY_Viewer Pdfviewerstate|Viewer Pdfviewerstate]]
- [[_COMMUNITY_Community 55|Community 55]]
- [[_COMMUNITY_Community 56|Community 56]]
- [[_COMMUNITY_Community 57|Community 57]]
- [[_COMMUNITY_Community 58|Community 58]]
- [[_COMMUNITY_Community 59|Community 59]]
- [[_COMMUNITY_Community 60|Community 60]]
- [[_COMMUNITY_Community 61|Community 61]]
- [[_COMMUNITY_Community 62|Community 62]]

## God Nodes (most connected - your core abstractions)
1. `HomeScreen()` - 28 edges
2. `Modifier` - 23 edges
3. `Color` - 22 edges
4. `Offset` - 21 edges
5. `TopBar()` - 19 edges
6. `PdfViewer()` - 19 edges
7. `PdfDatabase` - 19 edges
8. `Modifier` - 13 edges
9. `JvmPdfDatabase` - 13 edges
10. `Platform` - 13 edges

## Surprising Connections (you probably didn't know these)
- `PdfViewer()` --calls--> `LruMemoryCache`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → pdf-core/src/commonMain/kotlin/com/lumina/pdf/core/LruMemoryCache.kt
- `PdfViewer()` --calls--> `RenderQueue`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → pdf-core/src/commonMain/kotlin/com/lumina/pdf/core/RenderQueue.kt
- `TopBar()` --calls--> `RoundedCornerShape`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → shared/src/commonMain/kotlin/com/lumina/pdf/HomeScreen.kt
- `CloseIcon()` --calls--> `Offset`  [INFERRED]
  shared/src/commonMain/kotlin/com/lumina/pdf/HomeScreen.kt → pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt
- `GiftIcon()` --calls--> `Offset`  [INFERRED]
  shared/src/commonMain/kotlin/com/lumina/pdf/HomeScreen.kt → pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt

## Import Cycles
- None detected.

## Communities (63 total, 19 thin omitted)

### Community 0 - "Offset"
Cohesion: 0.20
Nodes (33): com, AcrobatPdfIcon(), ArrowRightIcon(), CloseIcon(), CloudIcon(), FolderIcon(), formatRelativeTime(), GiftIcon() (+25 more)

### Community 1 - "Mutablelist"
Cohesion: 0.09
Nodes (19): MutableList, org, Int, Float, Int, List, OutlineItem, PdfBitmap (+11 more)

### Community 2 - "Pdf Viewer Src Commonmain"
Cohesion: 0.22
Nodes (26): Offset, Boolean, Color, Float, Int, Modifier, PdfDocument, String (+18 more)

### Community 3 - "Pdf Renderer Src Androidmain"
Cohesion: 0.12
Nodes (13): Int, Float, Int, List, OutlineItem, PdfBitmap, PdfRect, String (+5 more)

### Community 4 - "Pdf Platform"
Cohesion: 0.12
Nodes (18): AndroidPlatform, getPlatform(), getPlatform(), getPlatform(), IOSPlatform, getPlatform(), JsPlatform, getPlatform() (+10 more)

### Community 5 - "Core Lrumemorycache"
Cohesion: 0.14
Nodes (8): LruMemoryCache, DiskCache, MemoryCache, PdfBitmap, String, ByteArray, PdfBitmap, String

### Community 6 - "Db Pdfdatabase Jvm"
Cohesion: 0.15
Nodes (11): getCurrentTimeMillis(), JvmPdfDatabase, rememberPdfDatabase(), LuminaDatabase, LuminaDatabaseQueries, DbNote, List, Long (+3 more)

### Community 7 - "Db Pdfdatabase Wasmjs"
Cohesion: 0.17
Nodes (10): getCurrentTimeMillis(), jsDateNow(), rememberPdfDatabase(), WasmJsPdfDatabase, Double, DbNote, List, Long (+2 more)

### Community 8 - "Db Pdfdatabase Android"
Cohesion: 0.18
Nodes (9): AndroidPdfDatabase, getCurrentTimeMillis(), rememberPdfDatabase(), DbNote, List, Long, PdfOffset, SqlDriver (+1 more)

### Community 9 - "Db Pdfdatabase Ios"
Cohesion: 0.18
Nodes (9): getCurrentTimeMillis(), IosPdfDatabase, rememberPdfDatabase(), DbNote, List, Long, PdfOffset, SqlDriver (+1 more)

### Community 10 - "Core Pdfdocument"
Cohesion: 0.19
Nodes (8): PdfDocument, Float, Int, List, OutlineItem, PdfBitmap, PdfRect, String

### Community 11 - "Db Pdfdatabase"
Cohesion: 0.04
Nodes (47): Android, Annotation Architecture, Cache Architecture, Compose Rendering Strategy, Continuous Scroll, Core Design Principle, Desktop, Final Recommendation (+39 more)

### Community 12 - "Db Pdfdatabase Js"
Cohesion: 0.10
Nodes (16): DbNote, getCurrentTimeMillis(), getCurrentTimeMillis(), JsPdfDatabase, rememberPdfDatabase(), PdfDatabase, PdfOffset, rememberPdfDatabase() (+8 more)

### Community 13 - "Iosapp Contentview"
Cohesion: 0.28
Nodes (6): ComposeView, ContentView, Self, UIViewController, UIViewControllerRepresentable, View

### Community 14 - "Core Renderqueue"
Cohesion: 0.39
Nodes (6): Page, RenderPriority, RenderQueue, RenderRequest, Thumbnail, Tile

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
Cohesion: 0.50
Nodes (3): App, iOSApp, Scene

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

### Community 55 - "Community 55"
Cohesion: 0.13
Nodes (15): 1. `:pdf-core`, 2. `:pdf-renderer`, 3. `:pdf-viewer`, 4. `:pdf-annotations` (Optional), 5. `:pdf-search` (Optional), 6. `:sample-app`, Android, Desktop (JVM) (+7 more)

### Community 56 - "Community 56"
Cohesion: 0.25
Nodes (7): Module pdf-core, Module pdf-renderer, Module pdf-viewer, Package com.lumina.pdf.core, Package com.lumina.pdf.renderer, Package com.lumina.pdf.viewer, Package com.lumina.pdf.viewer.db

### Community 57 - "Community 57"
Cohesion: 0.32
Nodes (4): RecentFilesStore, RecentFile, List, String

### Community 62 - "Community 62"
Cohesion: 0.29
Nodes (7): App(), AppHomeScreen, AppPdfViewerScreen, formatSize(), Screen, Int, String

## Knowledge Gaps
- **133 isolated node(s):** `Bundle`, `Scene`, `OutlineItem`, `Int`, `Float` (+128 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **19 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PdfDatabase` connect `Db Pdfdatabase Js` to `Db Pdfdatabase Android`, `Db Pdfdatabase Ios`, `Db Pdfdatabase Jvm`, `Db Pdfdatabase Wasmjs`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Why does `PdfViewer()` connect `Pdf Viewer Src Commonmain` to `Offset`, `Core Lrumemorycache`, `Community 62`, `Core Renderqueue`?**
  _High betweenness centrality (0.020) - this node is a cross-community bridge._
- **Why does `LruMemoryCache` connect `Core Lrumemorycache` to `Pdf Viewer Src Commonmain`?**
  _High betweenness centrality (0.012) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `HomeScreen()` (e.g. with `App()` and `.Content()`) actually correct?**
  _`HomeScreen()` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 11 inferred relationships involving `Offset` (e.g. with `CloseIcon()` and `GiftIcon()`) actually correct?**
  _`Offset` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Bundle`, `Scene`, `OutlineItem` to the rest of the system?**
  _133 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Mutablelist` be split into smaller, more focused modules?**
  _Cohesion score 0.09090909090909091 - nodes in this community are weakly interconnected._