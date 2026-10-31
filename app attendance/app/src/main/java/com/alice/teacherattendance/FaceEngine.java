package com.alice.teacherattendance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Finds the largest face, then turns it into a number signature with FaceNet.
 * The same numbers are stored when a teacher is enrolled and compared at check-in.
 */
public final class FaceEngine {
    private final Interpreter model;
    private final FaceDetector detector;
    private final int width;
    private final int length;

    public FaceEngine(Context context) throws Exception {
        model = new Interpreter(modelBytes(context));
        int[] input = model.getInputTensor(0).shape();
        int[] output = model.getOutputTensor(0).shape();
        if (input.length < 4 || input[3] != 3) {
            throw new Exception("This face model is not the expected photo format.");
        }
        width = input[1];
        length = output[output.length - 1];
        detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build());
    }

    public float[] embed(Bitmap photo) throws Exception {
        List<Face> faces = Tasks.await(detector.process(InputImage.fromBitmap(photo, 0)));
        Face best = null;
        int bestArea = 0;
        for (Face face : faces) {
            Rect box = face.getBoundingBox();
            int area = Math.max(0, box.width()) * Math.max(0, box.height());
            if (area > bestArea) {
                bestArea = area;
                best = face;
            }
        }
        if (best == null) {
            throw new Exception("No face in the photo. Hold the phone at face height and try again.");
        }
        return run(crop(photo, best.getBoundingBox()));
    }

    public void close() {
        model.close();
        detector.close();
    }

    private float[] run(Bitmap face) {
        Bitmap scaled = Bitmap.createScaledBitmap(face, width, width, true);
        ByteBuffer input = ByteBuffer.allocateDirect(width * width * 3 * 4);
        input.order(ByteOrder.nativeOrder());
        int[] pixels = new int[width * width];
        scaled.getPixels(pixels, 0, width, 0, 0, width, width);
        for (int pixel : pixels) {
            input.putFloat((((pixel >> 16) & 255) - 127.5f) / 128f);
            input.putFloat((((pixel >> 8) & 255) - 127.5f) / 128f);
            input.putFloat(((pixel & 255) - 127.5f) / 128f);
        }
        float[][] output = new float[1][length];
        model.run(input, output);
        float sum = 0;
        for (float value : output[0]) {
            sum += value * value;
        }
        float length = (float) Math.sqrt(sum);
        if (length > 0) {
            for (int i = 0; i < output[0].length; i++) {
                output[0][i] = output[0][i] / length;
            }
        }
        return output[0];
    }

    private Bitmap crop(Bitmap photo, Rect box) {
        int padX = Math.round(box.width() * 0.2f);
        int padY = Math.round(box.height() * 0.2f);
        int left = Math.max(0, box.left - padX);
        int top = Math.max(0, box.top - padY);
        int right = Math.min(photo.getWidth(), box.right + padX);
        int bottom = Math.min(photo.getHeight(), box.bottom + padY);
        return Bitmap.createBitmap(photo, left, top, Math.max(1, right - left), Math.max(1, bottom - top));
    }

    private ByteBuffer modelBytes(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("facenet.tflite")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            ByteBuffer bytes = ByteBuffer.allocateDirect(out.size());
            bytes.order(ByteOrder.nativeOrder());
            bytes.put(out.toByteArray());
            bytes.rewind();
            return bytes;
        } catch (Exception ex) {
            throw new Exception("The face model file is missing from the app.");
        }
    }
}
