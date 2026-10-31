package com.alice.teacherattendance;

import android.graphics.Bitmap;

/** Keeps only the newest camera frame. The reader takes ownership and recycles it. */
public final class LiveFrame {
    private Bitmap latest;

    public synchronized void offer(Bitmap frame) {
        if (latest != null && latest != frame) {
            latest.recycle();
        }
        latest = frame;
    }

    public synchronized Bitmap take() {
        Bitmap frame = latest;
        latest = null;
        return frame;
    }

    public synchronized Bitmap copy() {
        if (latest == null) return null;
        return latest.copy(Bitmap.Config.ARGB_8888, false);
    }

    public synchronized void clear() {
        if (latest != null) {
            latest.recycle();
            latest = null;
        }
    }
}
