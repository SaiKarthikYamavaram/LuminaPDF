# Module pdf-core
Core abstract declarations, models, and interfaces for the LuminaPDF rendering engine.

Contains the base definition for PDF document representation, rendering requests/queues, and cache interfaces.

# Package com.lumina.pdf.core
Core abstractions and system pipeline models for PDF rendering.

Key components:
- `PdfDocument`: Common interface representing the platform-native PDF document.
- `PdfBitmap`: Platform-neutral bitmap wrapper for rendered page outputs.
- `LruMemoryCache` & `DiskCache`: Double-level caching system.
- `RenderQueue`: Prioritized asynchronous rendering request pipeline (`Page`, `Tile`, `Thumbnail`).

# Module pdf-renderer
Platform-specific implementations of the `PdfDocument` interface utilizing native OS libraries.

# Package com.lumina.pdf.renderer
Contains platform rendering engines:
- Android: `AndroidPdfDocument` using native `android.graphics.pdf.PdfRenderer`.
- JVM/Desktop: `JvmPdfDocument` using `Apache PDFBox`.
- iOS: planned, using `PDFKit` (not implemented yet).
- JS/WasmJS: planned, using `pdf.js` (not implemented yet).

# Module pdf-viewer
Compose Multiplatform UI components and state handlers for rendering and interacting with PDF files.

# Package com.lumina.pdf.viewer
UI viewer controls, configurations, and zoom/pan state models:
- `PdfViewer()`: Main Compose composable for displaying PDF documents.
- `PdfViewerState`: Zoom, panning, offset, and visible page trackers.
- `PdfViewerConfig`: Customize behavior such as continuous scroll, margins, and rendering constraints.

# Package com.lumina.pdf.viewer.db
Database entities and SQLite schemas managing document persistence:
- `PdfDatabase`: Interacts with SQLDelight driver per platform to save annotation/state properties.
