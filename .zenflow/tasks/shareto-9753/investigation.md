# Share-to Image Attachment Investigation

## Bug summary
When users share an image into the app ("Share to"), they can see a thumbnail in some flows, but:
- "Save and Open" shows a thumbnail that cannot be opened.
- "Save and Close" later shows an empty/blank image area in-app.

This behavior is consistent with storing external `content://` URIs without durable read access, and/or not copying the image into app-owned storage.

## Root cause analysis
From current source inspection:

1. No Android share-intent entry point exists in this codebase.
- `app/src/main/AndroidManifest.xml` only defines launcher intent filters for `MainActivity` and has no `SEND`/`SEND_MULTIPLE` intent filter.
- `MainActivity` has no incoming intent parsing for `Intent.ACTION_SEND` / `EXTRA_STREAM`.

2. No share-attachment pipeline exists for images.
- Global search found no handling for `EXTRA_STREAM`, `ACTION_SEND`, `ACTION_SEND_MULTIPLE`, or attachment image import logic.
- No image attachment model currently exists for notes, and cow photos are modeled (`Cow.photos`) but not wired to UI rendering/editing paths.

3. Existing URI persistence pattern appears only in backup flow.
- `BackupViewModel` uses `contentResolver.takePersistableUriPermission(...)`.
- No equivalent logic exists for shared image URIs.

Likely failure mode matching reported symptoms:
- A temporary grant is available while handling the incoming share, allowing thumbnail decode in-session.
- After navigation/app state changes, permission is gone (or URI is no longer readable), so full open fails and later renders as blank.

## Affected components
- `app/src/main/AndroidManifest.xml` (missing share intent filter)
- `app/src/main/java/com/jumblemint/cows/MainActivity.kt` (no share intent ingestion)
- Attachment domain model/storage (currently missing for shared-image workflow)
- UI flow that shows thumbnail/open actions for share import (not present in this checked-in tree; likely pending/uncommitted or in another module/branch)

## Existing test coverage
- No tests found for share-intent ingestion, image attachment import, URI permission persistence, or image reopen behavior.

## Proposed solution
Implement a robust share-import pipeline that never relies on transient URI access:

1. Add share intent handling.
- Register `SEND` / `SEND_MULTIPLE` (image MIME types) in manifest.
- Handle incoming intents in `MainActivity` (`onCreate` and `onNewIntent`).

2. Normalize incoming image URIs immediately.
- On receive, copy each shared image stream into app-private storage (for example `filesDir/shared_images/...`).
- Store app-owned URI/path in DB, not third-party `content://` URI.
- This avoids provider-specific permission expiry issues.

3. Fallback for persisted SAF URIs if copying is not feasible.
- If any URI must be stored as external URI, call `takePersistableUriPermission` when flags allow it.
- Guard and log failures; degrade gracefully.

4. Ensure open/view code reads from stable URI.
- Thumbnail and full-open must use the same persisted/app-owned source.
- Add explicit error state in UI if file missing/corrupt.

5. Add regression tests.
- Unit tests for intent parsing and URI normalization.
- Instrumented test for "share image -> save -> reopen after activity recreation".
- Test both single and multiple image share paths.

## Edge cases and side effects
- Large image files: add size guard and downsample strategy for previews.
- Duplicate imports: choose deterministic naming/hash dedupe strategy.
- Cleanup: remove orphaned imported files when associated record is deleted.
- Migration: if legacy external URIs already exist, add lazy migration/copy-on-open path.

## Implementation notes
- Updated list serialization for `List<String>` in `Converters` to JSON instead of comma-splitting.
  - This prevents URI corruption when a URI string contains commas.
  - Backward compatibility is preserved: legacy comma-separated values are still parsed via fallback logic.
- Added `ImageUriPersistence` utility to normalize incoming photo URI strings:
  - For `content://` URIs, image bytes are copied into app-private storage under `filesDir/shared_images`.
  - Persisted value becomes an app-owned `file://` URI string, so reads do not depend on transient external grants.
  - Non-content URIs are kept unchanged.
- Integrated normalization into `CowDetailViewModel.saveCow()` so both new and edited cows save stable photo URIs.

## Test results
- Added unit tests: `app/src/test/java/com/jumblemint/cows/data/database/ConvertersTest.kt`
  - `toStringList_parsesLegacyCommaSeparatedValues`
  - `stringList_roundTripsUriContainingComma`
- Command run:
  - `./gradlew testDebugUnitTest --tests "com.jumblemint.cows.data.database.ConvertersTest"`
- Result:
  - Passed (exit code 0)
