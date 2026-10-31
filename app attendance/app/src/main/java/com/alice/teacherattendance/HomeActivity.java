package com.alice.teacherattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        TextView hello = findViewById(R.id.hello);
        TextView serverLine = findViewById(R.id.serverLine);
        hello.setText(Prefs.name(this));
        serverLine.setText(Prefs.server(this));
        findViewById(R.id.enroll).setOnClickListener(v -> startActivity(new Intent(this, EnrollActivity.class)));
        findViewById(R.id.verify).setOnClickListener(v -> startActivity(new Intent(this, VerifyActivity.class)));
        findViewById(R.id.logout).setOnClickListener(v -> {
            Prefs.clear(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        if (!"ADMIN".equals(Prefs.role(this)) && !"TEACHER".equals(Prefs.role(this))) {
            findViewById(R.id.enroll).setEnabled(false);
            findViewById(R.id.enroll).setAlpha(0.45f);
        }
    }
}
