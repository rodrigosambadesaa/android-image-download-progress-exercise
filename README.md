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

This exercise genuinely requires network access, so it incorporates Rodrigo Sambade's modernized
`ConnectivityAndInternetAccess.java` implementation from:

`https://gist.github.com/rodrigosambadesaa/729cca29a031fef4e2f15751863b655f`

The complete Java source is **vendored verbatim** in the repository at:

`app/src/main/java/net/i2p/android/router/util/ConnectivityAndInternetAccess.java`

It is the exact contents of Gist revision:

`ce7bd07ed17d10d48785a0c21527c8100ea6da7e`

This is the full expanded source (about 1,735 source lines), not the previously used compact/reformatted
copy. Git identifies the vendored file with blob SHA:

`10b6c8a7ed700f01b3a9d34cf8690bf0516f116a`

The Android CI downloads that pinned raw Gist revision and performs a byte-for-byte `cmp` against the
vendored Java file before compiling. A future accidental edit, truncation, reformatting or stale copy of
the connectivity helper therefore fails CI instead of silently changing the implementation.

The project does **not** download Java source as part of the Gradle build. A checkout remains
self-contained and the exact connectivity implementation is visible and reviewable in the normal source
tree.

### Integration used by the app

`MainActivity` uses the full helper through its public API:

- `ConnectivityAndInternetAccess.Builder().build()` for the reusable diagnostic instance;
- `ConnectivityAndInternetAccess.observeNetwork(...)` / `NetworkObserver` for passive lifecycle-aware
  default-network state;
- `NetworkState.isConnected()`, `isInternetValidated()` and `isCaptivePortalDetected()` to render the
  current network state without generating probe traffic;
- `checkInternetAsync(...)` only as an active diagnostic after a network-like download failure or when
  the user explicitly requests a diagnosis;
- `Request.cancel()` during replacement/teardown so an obsolete diagnostic does not update the UI.

The real image request remains the primary source of truth. A generic connectivity probe is deliberately
**not** used as a mandatory gate before every download: connectivity can change between a preflight and
the actual request, and the requested image host can fail even while generic Internet access works.

The required permissions are declared in the manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

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
