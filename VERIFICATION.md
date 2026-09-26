# Verification — Duplicard 1.1.2

## Fresh automated verification — 2026-09-17 UTC

- Gradle `testDebugUnitTest`, optimized `assembleRelease`, and `lintDebug`: **BUILD SUCCESSFUL**.
- Android unit tests: **12 passed, 0 failures, 0 errors**.
- Standalone Java core: **48 checks passed**, including conflicting-scan reset behavior and UPC-E prefix validation.
- Barcode round trips cover EAN-13, EAN-8, UPC-A, Code 128, Code 39, ITF and QR. UPC-E validation is covered separately.
- Backups cover version-2 format/color round trips and version-1 migration to EAN-13 with the default color.
- Lint: **0 errors**. Ten advisory warnings: six KTX style suggestions and four dependency-update notices.
- Optimized APK signature schemes v2 and v3 verified with the same saved development key as version 1.0.0.
- Package `nl.duplicard.wallet`, version code 4, version name 1.1.2, minimum SDK 26, target SDK 35.
- Merged APK has camera permission and no Internet permission. Automatic Android backup remains disabled.
- APK size: 23,628,042 bytes (22.5 MiB).
- APK SHA-256: `28892fd65b07652fc9346cea011a0a14b773d490c6718f618f323a87831e4fae`.

Evidence files are included in `verification-results/`.

## Physical-phone checks still needed

1. Install version 1.1.2 over the earlier build and confirm existing cards remain usable.
2. Add manual examples for every format, including UPC-E and QR; restart and confirm persistence.
3. Scan several real card types and confirm the format shown in the editor is correct.
4. Try preset and custom colors, including very dark and light colors; verify text remains readable.
5. Present one-dimensional and QR cards to real checkout scanners. Confirm screen brightness returns after leaving the card.
6. Deny camera permission, then enable it from settings; confirm manual entry remains usable.
7. Reorder, edit and delete cards, then restart to confirm order and changes.
8. Export a version-2 backup and re-import it; confirm formats/colors persist and duplicates are skipped.
9. Rotate in the editor/scanner and background the app while the document picker is open.

Use the fictional demo cards first. Real store scanners vary in their support for phone screens and barcode formats.
