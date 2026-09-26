# Duplicard 1.1.2

A native offline Android wallet for common loyalty-card barcodes and QR codes, built in Kotlin and Jetpack Compose. Android 8.0 or later.

## Install the supplied APK

Download the separately supplied `Duplicard-1.1.2-test.apk` to your Android phone and open it. Allow installation from your browser or file manager if Android asks. This is an optimized release build signed with a development key for personal testing.

## Open in Android Studio

1. Extract the ZIP to a permanent folder such as `C:\Users\Ivar\AndroidStudioProjects\Duplicard`.
2. Choose **Open** in Android Studio and select the folder containing `settings.gradle.kts`. Do not create another project or copy it into a template.
3. Allow Gradle sync and installation of **Android SDK Platform 35** and **Build Tools 35.0.0**. First-time dependency downloads require Internet.
4. Under **Settings → Build, Execution, Deployment → Build Tools → Gradle**, select **JDK 17** for Gradle JDK. Use the IDE's **Download JDK** option if necessary.
5. Connect your phone with USB debugging enabled, select it in the device selector, and press **Run**.

The standard Gradle wrapper is included. No terminal is required.

## Features

- **Add new card:** scan with the camera or enter the code manually. Camera scans preserve their detected format. Manual entry offers automatic detection or explicit EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, ITF and QR selection.
- **Stable scanning:** a camera result is accepted after three identical consecutive reads; conflicting or missed reads reset verification.
- **Card color:** choose a preset or create a custom RGB color. Text contrast adjusts automatically; displayed codes remain black on white for reliable scanning.
- **Checkout:** tap a card for a large barcode. This view temporarily raises brightness and keeps the screen awake.
- **Edit/delete:** open a card and use its buttons. Deletion asks for confirmation.
- **Reorder:** hold a card and drag; hold near the top/bottom to scroll. Alternatively use **Options → Move card up/down** in the card view. TalkBack custom actions are included.
- **Export:** wallet screen **Options → Export backup → Choose file**.
- **Import:** wallet screen **Options → Import backup**. Review counts and confirm. Existing cards remain unchanged; new cards are appended and duplicate numbers skipped.

## Data and backups

Cards use an app-private file with atomic writes, survive restarts, and work offline. There are no accounts, servers, ads or Internet permission. Camera permission is requested for scanning; the ML Kit model is bundled for offline use from first launch. Automatic Android backup is disabled.

Uninstalling or clearing app data removes cards. Export first. Exported JSON contains card names and numbers as plain text; keep it private. The system file picker can save locally or to a cloud provider you choose.

Import accepts Duplicard version 1 and 2 JSON, at most 2 MiB and 2,000 cards. Version 1 EAN-13 cards receive the default green color. Every entry is validated before the wallet changes. A malformed entry rejects the entire import. New IDs are assigned to imported entries. Other apps' export formats are not supported.

`examples/demo-backup.json` contains two fictional, format-valid cards for testing.

```json
{"app":"Duplicard","version":2,"cards":[{"id":"unique-id","name":"Example card","number":"4006381333931","format":"EAN_13","color":"#DDE8D5"}]}
```

## Scope

Static EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, ITF and QR codes are supported. NFC, payment cards and rotating/dynamic barcodes remain outside this version. Your store's scanner must support scanning a phone screen.

## Build an APK in the IDE

For a test APK, choose **Build → Generate App Bundles or APKs → Generate APKs** (sometimes called **Build APK(s)**). Output: `app/build/outputs/apk/debug/`.

For an optimized release, choose **Build → Generate Signed App Bundle or APK → APK**, create/select your own keystore and choose **release**. Release builds enable code and resource shrinking. Keep the keystore and password for future updates. No private signing key is included in the source.

## Tests and structure

Right-click the package under `app/src/test` and choose **Run Tests**. `CoreTest.java` can also run as a Java main class. See `VERIFICATION.md` for actual build/test results and remaining device checks.

| File | Purpose |
|---|---|
| `core/Card.java`, `core/CardRules.java` | Validation, checksums, merge, ordering |
| `BackupCodec.kt`, `CardRepository.kt` | Bounded JSON and atomic persistence |
| `WalletViewModel.kt` | State, restored forms, import/export, queued picker results |
| `WalletApp.kt`, `WalletList.kt` | Navigation, dialogs and reordering |
| `CardScreens.kt`, `BarcodeEncoding.kt` | Editor and checkout barcode |
| `ScannerScreen.kt` | Camera permissions, lifecycle and scanning |
| `Theme.kt`, `MainActivity.kt` | Theme and entry point |

Pinned versions: AGP 8.9.2, Gradle 8.11.1, Kotlin 2.0.21, SDK 35, JDK 17.

Technical references: [AGP compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes), [Compose/Kotlin](https://developer.android.com/jetpack/androidx/releases/compose-kotlin), [ML Kit scanning](https://developers.google.com/ml-kit/vision/barcode-scanning/android), [Android file picker](https://developer.android.com/training/data-storage/shared/documents-files).
