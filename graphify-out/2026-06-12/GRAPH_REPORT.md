# Graph Report - .  (2026-06-12)

## Corpus Check
- 90 files · ~50,206 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 422 nodes · 634 edges · 55 communities (34 shown, 21 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 24 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

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

## God Nodes (most connected - your core abstractions)
1. `HomeScreen()` - 24 edges
2. `Modifier` - 21 edges
3. `Offset` - 20 edges
4. `Color` - 20 edges
5. `TopBar()` - 19 edges
6. `PdfDatabase` - 19 edges
7. `PdfViewer()` - 17 edges
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
- `App()` --calls--> `PdfViewer()`  [INFERRED]
  shared/src/commonMain/kotlin/com/lumina/pdf/App.kt → pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt
- `PdfViewer()` --calls--> `RoundedCornerShape`  [INFERRED]
  pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/PdfViewer.kt → shared/src/commonMain/kotlin/com/lumina/pdf/HomeScreen.kt

## Import Cycles
- None detected.

## Communities (55 total, 21 thin omitted)

### Community 0 - "Offset"
Cohesion: 0.19
Nodes (33): Offset, App(), formatSize(), AcrobatPdfIcon(), CloseIcon(), CloudIcon(), FolderIcon(), GiftIcon() (+25 more)

### Community 1 - "Mutablelist"
Cohesion: 0.09
Nodes (19): MutableList, org, Int, Float, Int, List, OutlineItem, PdfBitmap (+11 more)

### Community 2 - "Pdf Viewer Src Commonmain"
Cohesion: 0.20
Nodes (25): Boolean, Color, Float, Int, Modifier, PdfDocument, String, Unit (+17 more)

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
Cohesion: 0.21
Nodes (8): DbNote, getCurrentTimeMillis(), PdfDatabase, PdfOffset, rememberPdfDatabase(), List, Long, String

### Community 12 - "Db Pdfdatabase Js"
Cohesion: 0.19
Nodes (8): getCurrentTimeMillis(), JsPdfDatabase, rememberPdfDatabase(), DbNote, List, Long, PdfOffset, String

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

## Knowledge Gaps
- **75 isolated node(s):** `Bundle`, `Scene`, `OutlineItem`, `Int`, `Float` (+70 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PdfDatabase` connect `Db Pdfdatabase` to `Db Pdfdatabase Jvm`, `Db Pdfdatabase Wasmjs`, `Db Pdfdatabase Android`, `Db Pdfdatabase Ios`, `Db Pdfdatabase Js`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Why does `PdfViewer()` connect `Pdf Viewer Src Commonmain` to `Offset`, `Core Lrumemorycache`, `Core Renderqueue`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Why does `LruMemoryCache` connect `Core Lrumemorycache` to `Pdf Viewer Src Commonmain`?**
  _High betweenness centrality (0.019) - this node is a cross-community bridge._
- **Are the 10 inferred relationships involving `Offset` (e.g. with `CloseIcon()` and `GiftIcon()`) actually correct?**
  _`Offset` has 10 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Bundle`, `Scene`, `OutlineItem` to the rest of the system?**
  _75 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Mutablelist` be split into smaller, more focused modules?**
  _Cohesion score 0.09090909090909091 - nodes in this community are weakly interconnected._
- **Should `Pdf Renderer Src Androidmain` be split into smaller, more focused modules?**
  _Cohesion score 0.11666666666666667 - nodes in this community are weakly interconnected._