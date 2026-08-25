package com.example.rodri.ejercicio3asynctask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

/** Performs one cancellable HTTP(S) download without depending on Android UI classes. */
public final class ImageDownloader {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int BUFFER_SIZE = 16 * 1024;

    private ImageDownloader() {
    }

    public interface ProgressListener {
        void onProgress(long downloadedBytes, long totalBytes);
    }

    public interface CancellationSignal {
        boolean isCancelled();
    }

    public static final class DownloadException extends IOException {
        private final Integer httpStatusCode;

        DownloadException(String message, Integer httpStatusCode) {
            super(message);
            this.httpStatusCode = httpStatusCode;
        }

        DownloadException(String message, Throwable cause, Integer httpStatusCode) {
            super(message, cause);
            this.httpStatusCode = httpStatusCode;
        }

        public Integer getHttpStatusCode() {
            return httpStatusCode;
        }

        public boolean hasHttpResponse() {
            return httpStatusCode != null;
        }
    }

    /**
     * Downloads {@code address} into {@code destination}. The method returns the absolute
     * destination path on success, mirroring the original exercise requirement.
     */
    public static String download(
            String address,
            File destination,
            ProgressListener progressListener,
            CancellationSignal cancellationSignal) throws IOException {

        validateHttpUrl(address);
        File parent = destination.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            throw new IOException("Could not create destination directory");
        }

        File partial = new File(parent, destination.getName() + ".part");
        if (partial.exists() && !partial.delete()) {
            throw new IOException("Could not remove an old partial download");
        }

        HttpURLConnection connection = null;
        Integer responseCode = null;
        try {
            URL url = URI.create(address).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "image/*");
            connection.setRequestProperty("User-Agent", "AsyncDownloadExercise/1.0");

            responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new DownloadException(
                        "Server returned HTTP " + responseCode,
                        responseCode);
            }

            long totalBytes = connection.getContentLength();
            long downloadedBytes = 0L;

            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 BufferedOutputStream output = new BufferedOutputStream(
                         new FileOutputStream(partial))) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()
                            || (cancellationSignal != null && cancellationSignal.isCancelled())) {
                        throw new InterruptedIOException("Download cancelled");
                    }

                    output.write(buffer, 0, read);
                    downloadedBytes += read;
                    if (progressListener != null) {
                        progressListener.onProgress(downloadedBytes, totalBytes);
                    }
                }
            }

            if (destination.exists() && !destination.delete()) {
                throw new IOException("Could not replace the previous destination file");
            }
            if (!partial.renameTo(destination)) {
                throw new IOException("Could not finalize the downloaded file");
            }
            return destination.getAbsolutePath();
        } catch (InterruptedIOException e) {
            deleteQuietly(partial);
            throw e;
        } catch (DownloadException e) {
            deleteQuietly(partial);
            throw e;
        } catch (IOException | RuntimeException e) {
            deleteQuietly(partial);
            throw new DownloadException("Download failed", e, responseCode);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String fileNameFromUrl(String address) {
        validateHttpUrl(address);
        URI uri = URI.create(address);
        String path = uri.getPath();
        String candidate = path == null ? "" : path.substring(path.lastIndexOf('/') + 1);
        candidate = candidate.trim();
        if (candidate.isEmpty()) {
            return "downloaded-image.jpg";
        }

        // Keep file names deterministic and safe for app-private storage.
        String safe = candidate.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.length() > 120) {
            safe = safe.substring(safe.length() - 120);
        }
        if (!safe.contains(".")) {
            safe += ".jpg";
        }
        return safe.toLowerCase(Locale.ROOT);
    }

    public static void validateHttpUrl(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is empty");
        }
        URI uri = URI.create(address.trim());
        String scheme = uri.getScheme();
        if (uri.getHost() == null
                || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Only absolute HTTP(S) URLs are supported");
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            // Best effort. A failed cleanup should not hide the original network/storage error.
            file.delete();
        }
    }
}
