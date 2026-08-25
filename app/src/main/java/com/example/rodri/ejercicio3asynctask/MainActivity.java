package com.example.rodri.ejercicio3asynctask;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rodri.ejercicio3asynctask.databinding.ActivityMainBinding;

import net.i2p.android.router.util.ConnectivityAndInternetAccess;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ConnectivityAndInternetAccess connectivity =
            new ConnectivityAndInternetAccess.Builder().build();

    private Future<?> downloadFuture;
    private ConnectivityAndInternetAccess.NetworkObserver networkObserver;
    private ConnectivityAndInternetAccess.Request diagnosticRequest;
    private volatile boolean cancellationRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.downloadButton.setOnClickListener(v -> startDownload());
        binding.cancelButton.setOnClickListener(v -> cancelDownload());
        binding.networkStatus.setOnClickListener(v -> runActiveDiagnostic());
        setBusy(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        networkObserver = ConnectivityAndInternetAccess.observeNetwork(
                this,
                this::renderNetworkState);
    }

    @Override
    protected void onStop() {
        if (networkObserver != null) {
            networkObserver.close();
            networkObserver = null;
        }
        super.onStop();
    }

    private void startDownload() {
        String address = binding.urlInput.getText().toString().trim();
        try {
            ImageDownloader.validateHttpUrl(address);
        } catch (IllegalArgumentException e) {
            binding.urlInput.setError(getString(R.string.invalid_url));
            return;
        }

        cancelDownloadInternal(false);
        cancellationRequested = false;
        setBusy(true);
        binding.resultText.setText(R.string.downloading);
        binding.downloadedImage.setImageDrawable(null);
        binding.progressBar.setIndeterminate(true);
        binding.progressBar.setProgress(0);

        File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picturesDir == null) {
            picturesDir = new File(getFilesDir(), "pictures");
        }
        File destination = new File(picturesDir, ImageDownloader.fileNameFromUrl(address));

        downloadFuture = downloadExecutor.submit(() -> {
            try {
                String path = ImageDownloader.download(
                        address,
                        destination,
                        this::postProgress,
                        () -> cancellationRequested || Thread.currentThread().isInterrupted());
                mainHandler.post(() -> onDownloadSucceeded(path));
            } catch (ImageDownloader.DownloadException e) {
                if (cancellationRequested || Thread.currentThread().isInterrupted()) {
                    mainHandler.post(this::onDownloadCancelled);
                } else {
                    mainHandler.post(() -> onDownloadFailed(e));
                }
            } catch (InterruptedIOException e) {
                mainHandler.post(this::onDownloadCancelled);
            } catch (Exception e) {
                mainHandler.post(() -> onUnexpectedFailure(e));
            }
        });
    }

    private void postProgress(long downloadedBytes, long totalBytes) {
        mainHandler.post(() -> {
            if (binding == null) {
                return;
            }
            if (totalBytes > 0L) {
                binding.progressBar.setIndeterminate(false);
                int percent = (int) Math.min(100L, downloadedBytes * 100L / totalBytes);
                binding.progressBar.setProgress(percent);
                binding.resultText.setText(getString(
                        R.string.progress_format,
                        percent,
                        downloadedBytes,
                        totalBytes));
            } else {
                binding.progressBar.setIndeterminate(true);
                binding.resultText.setText(getString(
                        R.string.progress_unknown_size,
                        downloadedBytes));
            }
        });
    }

    private void onDownloadSucceeded(String path) {
        if (binding == null) {
            return;
        }
        setBusy(false);
        binding.progressBar.setIndeterminate(false);
        binding.progressBar.setProgress(100);

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap == null) {
            binding.resultText.setText(getString(R.string.image_decode_failed, path));
            return;
        }

        binding.downloadedImage.setImageBitmap(bitmap);
        binding.resultText.setText(getString(R.string.download_success, path));
    }

    private void onDownloadFailed(ImageDownloader.DownloadException error) {
        if (binding == null) {
            return;
        }
        setBusy(false);
        if (error.hasHttpResponse()) {
            binding.resultText.setText(getString(
                    R.string.http_error,
                    error.getHttpStatusCode()));
            return;
        }

        binding.resultText.setText(R.string.download_failed_diagnosing);
        runActiveDiagnostic();
    }

    private void onUnexpectedFailure(Exception error) {
        if (binding == null) {
            return;
        }
        setBusy(false);
        binding.resultText.setText(getString(
                R.string.download_failed_detail,
                error.getClass().getSimpleName()));
    }

    private void cancelDownload() {
        if (downloadFuture == null || downloadFuture.isDone()) {
            return;
        }
        cancelDownloadInternal(true);
    }

    private void cancelDownloadInternal(boolean updateUi) {
        cancellationRequested = true;
        if (downloadFuture != null && !downloadFuture.isDone()) {
            downloadFuture.cancel(true);
        }
        downloadFuture = null;
        if (updateUi) {
            onDownloadCancelled();
        }
    }

    private void onDownloadCancelled() {
        if (binding == null) {
            return;
        }
        setBusy(false);
        binding.resultText.setText(R.string.download_cancelled);
    }

    private void renderNetworkState(ConnectivityAndInternetAccess.NetworkState state) {
        final int textRes;
        if (!state.isConnected()) {
            textRes = R.string.network_offline;
        } else if (state.isCaptivePortalDetected()) {
            textRes = R.string.network_captive_portal;
        } else if (state.isInternetValidated()) {
            textRes = R.string.network_validated;
        } else {
            textRes = R.string.network_available;
        }
        binding.networkStatus.setText(textRes);
    }

    private void runActiveDiagnostic() {
        if (diagnosticRequest != null) {
            diagnosticRequest.cancel();
        }
        binding.networkStatus.setText(R.string.network_diagnosing);
        diagnosticRequest = connectivity.checkInternetAsync(this, result -> {
            if (result.isReachable()) {
                binding.networkStatus.setText(getString(
                        R.string.network_reachable_via,
                        result.getReachedHost()));
            } else {
                binding.networkStatus.setText(R.string.network_unreachable);
            }
            diagnosticRequest = null;
        });
    }

    private void setBusy(boolean busy) {
        binding.downloadButton.setEnabled(!busy);
        binding.cancelButton.setEnabled(busy);
        binding.progressBar.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        cancelDownloadInternal(false);
        if (diagnosticRequest != null) {
            diagnosticRequest.cancel();
            diagnosticRequest = null;
        }
        downloadExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
        super.onDestroy();
    }
}
