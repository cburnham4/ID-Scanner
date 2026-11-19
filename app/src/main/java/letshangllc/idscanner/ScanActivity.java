package letshangllc.idscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Live camera PDF417 scanner using CameraX + ML Kit (on-device, no license key).
 */
public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "ScanActivity";

    private PreviewView previewView;
    private TextView hintView;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private final AtomicBoolean handled = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        previewView = findViewById(R.id.preview_view);
        hintView = findViewById(R.id.tv_scan_hint);
        Button cancel = findViewById(R.id.btn_cancel_scan);
        cancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scan_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_PDF417)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Prefer higher resolution for dense PDF417 symbols on IDs
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(
                                new android.util.Size(1920, 1080),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(resolutionSelector)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> analyzeImage(imageProxy));

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );
            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);
                Toast.makeText(this, R.string.err_camera_not_available, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (handled.get()) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage, imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(image)
                .addOnSuccessListener(this::onBarcodes)
                .addOnFailureListener(e -> Log.w(TAG, "Barcode process failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void onBarcodes(@NonNull List<Barcode> barcodes) {
        if (handled.get() || barcodes.isEmpty()) {
            return;
        }

        for (Barcode barcode : barcodes) {
            if (barcode.getFormat() != Barcode.FORMAT_PDF417) {
                continue;
            }

            DlScanResult result = null;
            Barcode.DriverLicense license = barcode.getDriverLicense();
            String raw = barcode.getRawValue();
            if (raw == null) {
                raw = barcode.getDisplayValue();
            }

            if (license != null) {
                result = DlScanResult.fromMlKit(license, raw);
            } else if (raw != null) {
                AamvaParser.Result parsed = AamvaParser.parse(raw);
                if (parsed != null) {
                    result = DlScanResult.fromAamva(parsed, raw);
                }
            }

            if (result != null && hasUsableIdentity(result)) {
                deliverResult(result);
                return;
            }
        }
    }

    private static boolean hasUsableIdentity(@NonNull DlScanResult result) {
        return !(isBlank(result.firstName)
                && isBlank(result.lastName)
                && isBlank(result.birthdate));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void deliverResult(@NonNull DlScanResult result) {
        if (!handled.compareAndSet(false, true)) {
            return;
        }
        runOnUiThread(() -> {
            hintView.setText(R.string.scan_success);
            Intent data = new Intent();
            result.writeToIntent(data);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
            cameraExecutor = null;
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
            barcodeScanner = null;
        }
    }
}
