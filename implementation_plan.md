# KMP PDF Renderer — Production Architecture Plan

> **Goal:** Build a production-grade, reusable **Kotlin Multiplatform PDF Renderer Framework** using **Compose Multiplatform**, optimized for performance, extensibility, and platform-native rendering.

---

# Philosophy

This project is **not a PDF Reader app**.

It is a **reusable PDF rendering framework** that host applications can integrate.

Architecture order:

```text
PDF Rendering Engine
        ↓
Viewer Framework
        ↓
Optional Features (annotations/search)
        ↓
Host App
```

Avoid tightly coupling app concerns (bookmarks, libraries, reading timers) into the renderer core.

The renderer must remain:

* Reusable
* Performant
* Testable
* Extensible
* Platform-native

---

# High-Level Architecture

```text
:pdf-core
:pdf-renderer
:pdf-viewer
:pdf-annotations (optional)
:pdf-search (optional)
:sample-app
```

---

# Module Breakdown

## 1. `:pdf-core`

Pure Kotlin Multiplatform module.

Contains:

* Shared contracts
* Models
* Interfaces
* State management
* Cache abstractions

### Responsibilities

* Document abstraction
* Page rendering APIs
* Rendering requests
* Viewer state
* Cache interfaces
* Event contracts

### No UI

### No platform code

### No app features

---

## 2. `:pdf-renderer`

Platform-specific rendering implementations.

Uses `expect/actual`.

### Android

Use:

```text
android.graphics.pdf.PdfRenderer
```

Why:

* Native
* Lightweight
* Stable
* No JNI complexity

---

### iOS

Use:

```text
PDFKit
```

Why:

* Native Apple rendering
* Better text support
* Selection support
* Fast rendering

---

### Desktop (JVM)

Use:

```text
Apache PDFBox
```

Dependency:

```text
org.apache.pdfbox:pdfbox
```

Why:

* Battle-tested
* Reliable rendering
* Text extraction support
* Search support

---

### Web (Later Phase)

Use:

```text
pdf.js
```

via JS interop.

**Recommendation:**
Skip Web in Phase 1.

Compose WASM + pdf.js introduces significant complexity.

Ship:

* Android
* iOS
* Desktop

first.

---

## 3. `:pdf-viewer`

Compose Multiplatform UI module.

Provides:

```kotlin
PdfViewer()
```

Features:

* Zoom
* Pan
* Scroll
* Page virtualization
* Continuous mode
* Page snapping
* Night mode

No persistence.

No annotations.

No database logic.

---

## 4. `:pdf-annotations` (Optional)

Plugin-style module.

Contains:

* Highlight
* Underline
* Ink
* Text notes

Decoupled from viewer.

Host app chooses whether to include it.

---

## 5. `:pdf-search` (Optional)

Provides:

* Full text extraction
* Search indexing
* Search highlights
* Match navigation

Can be lazy-loaded.

---

## 6. `:sample-app`

Only for:

* Testing
* Showcase
* Debugging

Not part of framework architecture.

---

# Recommended Tech Stack

## Shared (`commonMain`)

| Dependency             | Purpose                 |
| ---------------------- | ----------------------- |
| kotlinx-coroutines     | Async rendering         |
| kotlinx-serialization  | Config serialization    |
| Kermit                 | Logging                 |
| Koin (optional)        | DI                      |
| Multiplatform Settings | Lightweight preferences |

---

## Android

| Dependency        | Purpose           |
| ----------------- | ----------------- |
| PdfRenderer       | Native rendering  |
| Coil3             | Thumbnail loading |
| Lifecycle Compose | State integration |

---

## iOS

| Dependency | Purpose          |
| ---------- | ---------------- |
| PDFKit     | Native rendering |

---

## Desktop

| Dependency | Purpose                     |
| ---------- | --------------------------- |
| PDFBox     | Rendering + text extraction |

---

## Web

| Dependency | Purpose   |
| ---------- | --------- |
| pdf.js     | Rendering |

---

# Core Design Principle

## Native Rendering via `expect/actual`

Pure Kotlin PDF rendering is not mature enough for production.

The correct strategy is:

```text
Common APIs
        ↓
Platform-native engines
```

---

## Shared Interface

```kotlin
interface PdfDocument {

    suspend fun pageCount(): Int

    suspend fun renderPage(
        page: Int,
        width: Int,
        height: Int
    ): ImageBitmap

    suspend fun renderTile(
        page: Int,
        region: PdfRect,
        scale: Float
    ): ImageBitmap

    suspend fun extractText(
        page: Int
    ): String

    suspend fun getOutline(): List<OutlineItem>

    fun close()
}
```

### Why `renderTile()` matters

This separates hobby implementations from production renderers.

Without tiling:

```text
Zoom = 4x
A4 Page = 6000x9000 bitmap
```

Result:

```text
Huge memory spikes
OOM crashes
Slow rendering
```

With tiles:

```text
[0,0] [0,1]
[1,0] [1,1]
```

Only visible sections are rendered.

Like Google Maps.

---

# Viewer API Design

Target developer experience:

```kotlin
val state = rememberPdfViewerState()

PdfViewer(
    document = document,
    state = state,
    config = PdfViewerConfig(
        maxZoom = 5f,
        pageSpacing = 12.dp,
        continuousScroll = true,
        nightMode = false
    )
)
```

---

## Viewer State

Immutable state model.

```kotlin
data class PdfViewerState(
    val currentPage: Int = 0,
    val zoom: Float = 1f,
    val scrollOffset: Offset = Offset.Zero,
    val visiblePages: List<Int> = emptyList()
)
```

Why:

* Predictable UI
* Easier testing
* Better Compose interoperability
* Prevents state bugs

---

# Rendering Pipeline

This is the most important system.

## Multi-Resolution Rendering

Always render progressively.

Pipeline:

```text
Placeholder
    ↓
Thumbnail render
    ↓
Medium render
    ↓
High-quality render
```

Benefits:

* Fast page appearance
* Smooth scrolling
* Better UX

---

## Smart Zoom Rendering

When zoom changes:

```text
Show cached lower resolution
        ↓
Render better quality
        ↓
Swap result
```

Avoid blank pages while zooming.

---

# Page Virtualization

Never render all pages.

Only render:

```text
Current page
±2 nearby pages
```

Example:

Viewing page 10:

```text
8
9
10
11
12
```

Everything else:

```text
Disposed
```

Benefits:

* Massive memory savings
* Smooth scrolling
* Scales to 1000+ page PDFs

---

# Tile Rendering Strategy

For zoomed pages:

Instead of:

```text
Single large bitmap
```

Render:

```text
Tiles
```

Example:

```text
Page 4

┌──────┬──────┐
│ 0,0  │ 0,1  │
├──────┼──────┤
│ 1,0  │ 1,1  │
└──────┴──────┘
```

Render only visible tiles.

---

# Cache Architecture

Use a 3-level cache.

```text
L1 → Memory Cache
L2 → Disk Cache
L3 → Native Document Handle
```

---

## L1 — Memory Cache

Stores:

```text
page-scale bitmap
```

Example key:

```text
doc123_page5_scale2
```

Eviction:

```text
LRU
```

---

## L2 — Disk Cache

Stores:

* Thumbnails
* Render snapshots

Useful for reopening documents.

---

## L3 — Native Handle Cache

Keep PDFs open while viewing.

Avoid reopening file repeatedly.

Example:

```text
PdfRenderer instance
PDFDocument instance
PDDocument instance
```

---

# Rendering Queue

Must be prioritized.

Priority:

```text
1. Visible viewport
2. Next page
3. Previous page
4. Thumbnail prefetch
```

Recommended:

```kotlin
PriorityBlockingQueue
```

or

```kotlin
Coroutine Actor
```

Avoid uncontrolled rendering.

---

# Compose Rendering Strategy

Avoid stacking heavy composables.

Preferred:

```kotlin
Canvas
```

with:

```kotlin
Modifier.graphicsLayer()
```

for:

* Zoom
* Translation
* Scaling

Why:

* Better performance
* Less recomposition
* More control

---

# Viewer Modes

## Phase 1

Support:

### Continuous Scroll

```text
PDF scrolling
```

### Paged Mode

```text
Page snapping
```

---

## Phase 2

Add:

### Two-page spread

Desktop/iPad focus.

---

# Night Mode

Do not re-render PDFs.

Apply:

```text
ColorMatrix
```

at Compose layer.

Benefits:

* Fast
* Cheap
* No duplicated rendering

---

# Search Architecture

Search is optional.

Use native engines.

Process:

```text
Search Query
      ↓
Native text extraction
      ↓
Bounding boxes
      ↓
Highlight overlay
```

Compose draws highlights.

---

# Annotation Architecture

Keep independent from renderer.

Data model:

```kotlin
sealed interface Annotation

data class Highlight()

data class Underline()

data class Ink()

data class TextNote()
```

Coordinates should be:

```text
normalized (0f → 1f)
```

Why:

Annotations survive:

* screen size changes
* zoom
* rotation

---

# Persistence (Host App Responsibility)

Renderer should not own persistence.

Host app stores:

```text
Last page
Zoom
Scroll offset
Bookmarks
Notes
```

Suggested:

```text
SQLDelight
```

Schema example:

```sql
CREATE TABLE PdfState (
    documentId TEXT PRIMARY KEY,
    lastPage INTEGER,
    zoomScale REAL,
    scrollOffsetX REAL,
    scrollOffsetY REAL
);
```

---

# Recommended Build Phases

## Phase 1 — MVP Renderer

Goal:

Smooth PDF rendering.

Deliver:

* open document
* render pages
* zoom
* pan
* virtualization
* cache
* Android/iOS/Desktop

No annotations.

No search.

---

## Phase 2 — Performance

Deliver:

* tile rendering
* multi-resolution rendering
* render queue
* prefetch
* memory optimizations

---

## Phase 3 — Viewer UX

Deliver:

* continuous mode
* page snap
* night mode
* double tap zoom
* outline/TOC

---

## Phase 4 — Advanced

Deliver:

* text selection
* search
* annotations
* highlights
* links
* forms

---

# Testing Strategy

## Unit Tests

Test:

* cache eviction
* state restoration
* render queue

---

## Integration Tests

Test:

* open large PDFs
* memory usage
* zoom performance

---

## Stress Tests

Cases:

```text
1000+ page PDF
Huge engineering drawings
Rapid zoom gestures
Fast scrolling
```

---

# Things To Avoid

❌ Rendering all pages

❌ Full-page bitmap rendering at large zoom

❌ Tight coupling to SQL/database

❌ Building app features before renderer stability

❌ Reopening PDF file repeatedly

❌ Heavy recompositions

❌ Premature Web support

---

# Final Recommendation

Build in this order:

```text
Engine
    ↓
Viewer
    ↓
Performance
    ↓
UX
    ↓
Advanced features
    ↓
Host app
```

Focus on making:

```text
Page rendering extremely smooth
```

before adding features.

A fast renderer with fewer features wins over a feature-rich but laggy implementation.
