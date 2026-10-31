package com.alice.teacherattendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureActivity extends AppCompatActivity {
    private static final int CAMERA = 22;

    private PreviewView preview;
    private TextView status;
    private final LiveFrame frames = new LiveFrame();
    private volatile FaceEngine engine;
    private ExecutorService executor;
    private boolean busy;
    private long teacherId;
    private String teacherName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        preview = findViewById(R.id.preview);
        status = findViewById(R.id.status);
        TextView title = findViewById(R.id.title);
        TextView phase = findViewById(R.id.phase);
        teacherId = getIntent().getLongExtra("teacherId", -1);
        teacherName = getIntent().getStringExtra("teacherName");
        title.setText(teacherName == null ? "Staff" : teacherName);
        phase.setText("ENROLL");
        status.setText("Centre the face in the ring, then save.");
        executor = Executors.newSingleThreadExecutor();
        findViewById(R.id.close).setOnClickListener(v -> finish());
        findViewById(R.id.saveFace).setOnClickListener(v -> save());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            open();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == CAMERA && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            open();
        } else {
            status.setText("The camera permission is required.");
        }
    }

    private void open() {
        startCamera();
        executor.execute(() -> {
            try {
                engine = new FaceEngine(this);
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText(ex.getMessage()));
            }
        });
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider provider = ProcessCameraProvider.getInstance(this).get();
                Preview previewUse = new Preview.Builder().build();
                previewUse.setSurfaceProvider(preview.getSurfaceProvider());
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                analysis.setAnalyzer(executor, image -> {
                    try {
                        frames.offer(Frames.rgba(image));
                    } catch (Exception ignored) {
                        // Skip a bad frame.
                    } finally {
                        image.close();
                    }
                });
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, previewUse, analysis);
            } catch (Exception ex) {
                status.setText("Camera failed: " + ex.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void save() {
        if (busy || engine == null) {
            status.setText("Wait until the camera is ready.");
            return;
        }
        Bitmap frame = frames.copy();
        if (frame == null) {
            status.setText("Wait until the camera is ready.");
            return;
        }
        busy = true;
        status.setText("Reading the face…");
        executor.execute(() -> {
            try {
                float[] vector = engine.embed(frame);
                JSONArray descriptor = new JSONArray();
                for (float value : vector) descriptor.put((double) value);
                Api.enroll(this, teacherId, descriptor);
                runOnUiThread(() -> {
                    status.setText("Face saved for " + teacherName + ".");
                    finish();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    busy = false;
                    status.setText(ex.getMessage() == null ? "The face could not be read." : ex.getMessage());
                });
            } finally {
                frame.recycle();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) engine.close();
        frames.clear();
        if (executor != null) executor.shutdown();
    }
}
