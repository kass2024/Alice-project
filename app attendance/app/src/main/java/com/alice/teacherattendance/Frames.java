package com.alice.teacherattendance;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public final class Frames {
    private Frames() {}

    public static Bitmap rgba(ImageProxy image) {
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int row = y * rowStride;
            for (int x = 0; x < width; x++) {
                int index = row + x * pixelStride;
                int red = buffer.get(index) & 255;
                int green = buffer.get(index + 1) & 255;
                int blue = buffer.get(index + 2) & 255;
                int alpha = pixelStride > 3 ? (buffer.get(index + 3) & 255) : 255;
                pixels[y * width + x] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        int degrees = image.getImageInfo().getRotationDegrees();
        if (degrees == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap turned = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        if (turned != bitmap) {
            bitmap.recycle();
        }
        return turned;
    }
}
