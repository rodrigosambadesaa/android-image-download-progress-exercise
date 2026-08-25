# Review of the recovered exercise

## What was found

The recovered `Ejercicio3AsyncTask` source contained only a skeleton `MainActivity` that called
`setContentView(R.layout.activity_main)`. No download task, progress handling, file result, or image
rendering code was present.

## What the original assignment required

The course handout says to update the earlier “download a file showing a ProgressDialog bar” exercise
so that the background task:

1. returns the destination file path on success, or an empty string on failure; and
2. makes `MainActivity` display the downloaded image in an `ImageView`.

The earlier exercise also required a real Internet download, progress updates based on bytes,
error handling and optional cancellation.

## Modernization decisions

This repository preserves the learning objectives but replaces obsolete/problematic mechanisms:

- `AsyncTask` -> `ExecutorService` + `Future` + main-thread `Handler` callbacks.
- `ProgressDialog` -> lifecycle-owned in-layout `ProgressBar`.
- shared external-storage root -> app-specific pictures directory; no storage permission required.
- partial downloads -> `.part` file that is deleted on error/cancellation and renamed only on success.
- silent exception swallowing -> explicit HTTP/network/storage error reporting.
- fixed URL only -> editable URL while retaining the historical course URL as the default.
- ad-hoc connectivity checks -> Rodrigo Sambade's pinned `ConnectivityAndInternetAccess.java` Gist.

The connectivity helper is intentionally *not* used as a mandatory active preflight. The app observes
Android's default-network state passively, performs the real image request, and launches the helper's
active generic Internet diagnostic only when a request fails before receiving an HTTP response.
