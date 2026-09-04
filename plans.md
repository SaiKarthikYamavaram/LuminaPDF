# LuminaPDF — Improvement Plan

Findings from a full project scan (2026-09-04), cross-checked against `implementation_plan.md`,
the DB/persistence layers, and test coverage. Not yet implemented. Grouped into phases,
most-impactful first.

## Phase 1 — Critical gaps (groundwork already exists, blocks real-world use)

1. **Password-protected PDFs.** No password/encryption handling in any `PdfDocumentFactory`
   (Android/JVM/iOS). Opening one throws, caught generically in `App.kt` as "Error opening file"
   — no password prompt. Common real-world PDFs hit this today with a dead-end error.
2. **Highlight annotations + text selection.** `PdfDatabase.saveHighlights`/`getHighlights` are
   implemented on every platform (`pdf-viewer/src/commonMain/kotlin/com/lumina/pdf/viewer/db/PdfDatabase.kt:29-30`
   plus all 5 platform actuals) but nothing ever calls them, and there's no text-selection UI to
   drive it. `PdfDocument.extractText()` already exists to build selection on top of. Core
   PDF-reader feature users expect; persistence is already done.
3. **Accessibility.** Only 2 `contentDescription` uses across all shared/pdf-viewer UI — sort
   button, view switcher, nav tabs are bare `clickable` Rows with custom-drawn icons. TalkBack/
   VoiceOver users get almost nothing. Affects every screen-reader user and store compliance.

## Phase 2 — Viewer feature completeness

4. **Share / print / export.** `PdfViewerTopBar.kt` menu is just zoom in/out, fit page, night
   mode — no share-file, print, or export.
5. **Page manipulation.** No rotate, delete-page, reorder, or merge/split tools.
6. **Jump to page.** Navigation only via outline/notes/first-last buttons — no typed
   "go to page N" field.
7. **User bookmarks.** No bookmarks distinct from the PDF's own outline; the Notes feature
   already has the exact pattern (page + timestamp + DB row) to copy for this.

## Phase 3 — Document organization

8. **Folders/tags/collections.** "Your documents" is the same flat list as Recents, filtered by
   one bool (`HomeScreen.kt:148`, `inRecents`) — no real organization.
9. **Rename** for stored documents — not currently possible.
10. **Storage usage UI.** Files are fully copied into app storage (`FilePersistence`), dedup only
    by name+size, no size cap, no "free up space" screen.

## Phase 4 — Engineering health & platform completeness

11. **Real test coverage.** Outside `LruMemoryCacheTest` (real, good coverage), every other test
    file is the unmodified KMP template placeholder (`assertEquals(3, 1+2)`). Zero coverage on
    `RecentFilesStore`, sort/filter logic, note/highlight CRUD, or the Koin double-start guard
    added 2026-09-04.
12. **CI pipeline.** `.github/workflows` absent — none of the 4 platform builds/tests run on
    push/PR.
13. **iOS/Web verification.** iOS (`iosApp/iosApp/ContentView.swift`, 17 lines) and Web
    (`webApp/.../main.kt`, 11 lines) entry points are minimal — not launched/verified as of this
    scan.
14. **iOS dSYM upload.** `./gradlew setupKotzillaXcode` still not run.

## Suggested next step

Start with #1 and #2 — both have existing groundwork (renderer interfaces, DB persistence) and
the highest user-facing impact of anything on this list.
