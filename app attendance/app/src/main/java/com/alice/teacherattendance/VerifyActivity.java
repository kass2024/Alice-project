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
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerifyActivity extends AppCompatActivity {
    private static final int CAMERA = 23;

    private PreviewView previewView;
    private TextView phase;
    private TextView name;
    private TextView detail;
    private final LiveFrame frames = new LiveFrame();
    private volatile FaceEngine engine;
    private ExecutorService cameraExecutor;
    private ExecutorService scanExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long quietUntil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);
        previewView = findViewById(R.id.preview);
        phase = findViewById(R.id.phase);
        name = findViewById(R.id.name);
        detail = findViewById(R.id.detail);
        cameraExecutor = Executors.newSingleThreadExecutor();
        scanExecutor = Executors.newSingleThreadExecutor();
        findViewById(R.id.close).setOnClickListener(v -> finish());
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
            show("CAMERA", "Permission needed", "Allow the camera, then open Verify again.", getColor(R.color.danger));
        }
    }

    private void open() {
        startCamera();
        cameraExecutor.execute(() -> {
            try {
                engine = new FaceEngine(this);
                runOnUiThread(() -> show("WATCHING", "Looking for a face", "Hold still inside the ring.", getColor(R.color.gold)));
                watch();
            } catch (Exception ex) {
                runOnUiThread(() -> show("ERROR", "Face model", ex.getMessage(), getColor(R.color.danger)));
            }
        });
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider provider = ProcessCameraProvider.getInstance(this).get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                analysis.setAnalyzer(cameraExecutor, image -> {
                    try {
                        frames.offer(Frames.rgba(image));
                    } catch (Exception ignored) {
                        // Skip a bad frame.
                    } finally {
                        image.close();
                    }
                });
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis);
            } catch (Exception ex) {
                show("ERROR", "Camera", ex.getMessage(), getColor(R.color.danger));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void watch() {
        running.set(true);
        scanExecutor.execute(() -> {
            while (running.get()) {
                try {
                    if (System.currentTimeMillis() < quietUntil || engine == null) {
                        Thread.sleep(250);
                        continue;
                    }
                    Bitmap frame = frames.take();
                    if (frame == null) {
                        Thread.sleep(250);
                        continue;
                    }
                    try {
                        float[] vector = engine.embed(frame);
                        JSONArray descriptor = new JSONArray();
                        for (float value : vector) descriptor.put((double) value);
                        JSONObject result = Api.verify(this, descriptor);
                        runOnUiThread(() -> paint(result));
                        quietUntil = System.currentTimeMillis() + pauseFor(result);
                    } finally {
                        frame.recycle();
                    }
                } catch (InterruptedException stopped) {
                    break;
                } catch (Exception ex) {
                    String message = ex.getMessage() == null ? "" : ex.getMessage();
                    if (message.contains("No face")) {
                        runOnUiThread(() -> show("WATCHING", "Looking for a face", "Hold still inside the ring.", getColor(R.color.gold)));
                        quietUntil = System.currentTimeMillis() + 600;
                    } else if (running.get()) {
                        runOnUiThread(() -> show("NOT RECOGNISED", "Unknown face", message, getColor(R.color.danger)));
                        quietUntil = System.currentTimeMillis() + 1800;
                    }
                }
            }
        });
    }

    private long pauseFor(JSONObject result) {
        String action = result.optString("action");
        if ("CHECK_IN".equals(action) || "CHECK_OUT".equals(action)) return 12000;
        if ("DONE".equals(action) || "ON_LEAVE".equals(action)) return 6000;
        return result.optBoolean("matched") ? 4000 : 1800;
    }

    private void paint(JSONObject result) {
        String action = result.optString("action", "");
        JSONObject teacher = result.optJSONObject("teacher");
        String person = teacher == null ? "Unknown face" : teacher.optString("fullName", "Staff");
        String message = result.optString("message", "");
        if ("CHECK_IN".equals(action)) {
            show("CHECK IN", person, message, getColor(R.color.forest));
        } else if ("CHECK_OUT".equals(action)) {
            show("CHECK OUT", person, message, getColor(R.color.gold));
        } else if ("ON_LEAVE".equals(action)) {
            show("ON LEAVE", person, message, getColor(R.color.gold));
        } else if ("DONE".equals(action)) {
            show("ALREADY RECORDED", person, message, getColor(R.color.muted));
        } else {
            show("NOT RECOGNISED", "Try again", message, getColor(R.color.danger));
        }
    }

    private void show(String label, String person, String message, int color) {
        phase.setText(label);
        phase.setTextColor(color);
        name.setText(person);
        detail.setText(message);
    }

    @Override
    protected void onDestroy() {
        running.set(false);
        super.onDestroy();
        if (engine != null) engine.close();
        frames.clear();
        if (cameraExecutor != null) cameraExecutor.shutdownNow();
        if (scanExecutor != null) scanExecutor.shutdownNow();
    }
}
