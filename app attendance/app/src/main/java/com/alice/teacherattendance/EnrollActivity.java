package com.alice.teacherattendance;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class EnrollActivity extends AppCompatActivity {
    private LinearLayout staffList;
    private TextView hint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll);
        staffList = findViewById(R.id.staffList);
        hint = findViewById(R.id.enrollHint);
        findViewById(R.id.back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        hint.setText("Loading staff…");
        staffList.removeAllViews();
        new Thread(() -> {
            try {
                List<Api.Teacher> teachers = Api.teachers(this);
                runOnUiThread(() -> show(teachers));
            } catch (Exception ex) {
                runOnUiThread(() -> hint.setText(ex.getMessage()));
            }
        }).start();
    }

    private void show(List<Api.Teacher> teachers) {
        staffList.removeAllViews();
        if (teachers.isEmpty()) {
            hint.setText("No registered staff were found.");
            return;
        }
        int enrolled = 0;
        for (Api.Teacher teacher : teachers) {
            if (teacher.hasFace) enrolled++;
        }
        hint.setText(teachers.size() + " staff · " + enrolled + " faces saved");
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Api.Teacher teacher : teachers) {
            LinearLayout card = (LinearLayout) inflater.inflate(R.layout.item_teacher, staffList, false);
            TextView initials = card.findViewById(R.id.initials);
            TextView name = card.findViewById(R.id.name);
            TextView department = card.findViewById(R.id.department);
            TextView pill = card.findViewById(R.id.pill);
            MaterialButton take = card.findViewById(R.id.take);
            MaterialButton remove = card.findViewById(R.id.remove);
            initials.setText(initialsOf(teacher.fullName));
            name.setText(teacher.fullName);
            department.setText(teacher.department.isEmpty() ? "Staff" : teacher.department);
            if (teacher.hasFace) {
                pill.setText("Enrolled");
                pill.setBackgroundResource(R.drawable.bg_pill);
                pill.setTextColor(getColor(R.color.forest));
                remove.setVisibility(android.view.View.VISIBLE);
            } else {
                pill.setText("No face");
                pill.setBackgroundResource(R.drawable.bg_pill_gold);
                pill.setTextColor(getColor(R.color.gold));
                remove.setVisibility(android.view.View.GONE);
            }
            take.setOnClickListener(v -> openCamera(teacher));
            remove.setOnClickListener(v -> confirmRemove(teacher));
            staffList.addView(card);
        }
    }

    private void openCamera(Api.Teacher teacher) {
        Intent intent = new Intent(this, CaptureActivity.class);
        intent.putExtra("teacherId", teacher.id);
        intent.putExtra("teacherName", teacher.fullName);
        startActivity(intent);
    }

    private void confirmRemove(Api.Teacher teacher) {
        new AlertDialog.Builder(this)
                .setTitle("Remove face")
                .setMessage("Remove the saved face for " + teacher.fullName + "? They will need a new photo before the gate can recognise them.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) -> remove(teacher))
                .show();
    }

    private void remove(Api.Teacher teacher) {
        hint.setText("Removing " + teacher.fullName + "…");
        new Thread(() -> {
            try {
                Api.removeFace(this, teacher.id);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Face removed for " + teacher.fullName, Toast.LENGTH_SHORT).show();
                    load();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> hint.setText(ex.getMessage()));
            }
        }).start();
    }

    private String initialsOf(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
