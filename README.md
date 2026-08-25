# Android image download with progress

Modern reconstruction of **Ejercicio 3 AsyncTask** from the DAM course *Programación multimedia y
dispositivos móviles*.

The recovered student project contained only a `MainActivity` skeleton, while the original handout
required a real image download, byte progress, a destination-path result, and rendering the downloaded
image in an `ImageView`. This repository completes that exercise and updates it for current Android.

## What it demonstrates

- background networking with `ExecutorService` instead of deprecated `AsyncTask`;
- cancellable work via `Future.cancel(true)` and cooperative cancellation checks;
- `HttpURLConnection` with bounded connect/read timeouts and explicit HTTP status handling;
- byte-accurate progress when `Content-Length` is available;
- safe `.part` files and cleanup on failed/cancelled downloads;
- app-specific picture storage (no legacy `WRITE_EXTERNAL_STORAGE` permission);
- returning the **downloaded file path** and decoding that path into an `ImageView`;
- lifecycle-aware passive connectivity observation;
- an active Internet diagnostic only after network-like request failures;
- View Binding, unit tests and GitHub Actions.

## ConnectivityAndInternetAccess Gist

This exercise genuinely requires network access, so it uses Rodrigo Sambade's modernized
`ConnectivityAndInternetAccess.java` implementation:

`https://gist.github.com/rodrigosambadesaa/729cca29a031fef4e2f15751863b655f`

The build pins revision:

`ce7bd07ed17d10d48785a0c21527c8100ea6da7e`

`app/build.gradle` downloads exactly that Java snapshot into `build/generated/connectivity` before the
Android build. Pinning the revision means future edits to the Gist cannot silently change an old build.
The app follows the Gist's recommended pattern: passive network state -> real backend/file request ->
active generic reachability diagnostic only when useful.

## Original URL

The historical handout used:

`https://tesdai.com/images/tesdai/fachada.jpg`

It remains the default text in the UI for traceability, but the field is editable because course-hosted
assets may move or disappear over time.

## Build

Requirements:

- JDK 21 to run the current AGP 9.3 / Android Lint toolchain reliably;
- Java source/target compatibility remains Java 17;
- Gradle 9.5.0;
- Android SDK 37.

```bash
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The CI deliberately runs Gradle on JDK 21 while compiling the project itself with Java 17 compatibility.
This avoids an AGP 9.3 Android Lint runtime incompatibility that can occur on JDK 17 without raising the
app's Java language requirement.

To add the standard Gradle Wrapper locally:

```bash
gradle wrapper --gradle-version 9.5.0
```

## Project status

This repository is a modern completion/reconstruction of an incomplete 2018 student exercise, not a
claim that all of this code existed in the original submission. See `docs/ORIGINAL_REVIEW.md` for the
source-vs-reconstruction distinction.
