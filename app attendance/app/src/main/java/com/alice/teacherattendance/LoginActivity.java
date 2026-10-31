package com.alice.teacherattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Prefs.token(this).isEmpty()) {
            openHome();
            return;
        }
        setContentView(R.layout.activity_login);
        EditText server = findViewById(R.id.server);
        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        TextView message = findViewById(R.id.message);
        Button signIn = findViewById(R.id.signIn);
        Button test = findViewById(R.id.test);
        String saved = Prefs.server(this);
        if (!saved.isEmpty()) {
            server.setText(saved);
        }

        test.setOnClickListener(v -> {
            test.setEnabled(false);
            message.setText("Contacting the laptop…");
            String serverText = server.getText().toString().trim();
            new Thread(() -> {
                try {
                    Api.health(serverText);
                    runOnUiThread(() -> {
                        test.setEnabled(true);
                        message.setText("Connected. The app can reach the school API. Sign in with the same username as the website.");
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        test.setEnabled(true);
                        message.setText(ex.getMessage());
                    });
                }
            }).start();
        });

        signIn.setOnClickListener(v -> {
            signIn.setEnabled(false);
            message.setText("Signing in…");
            String serverText = server.getText().toString().trim();
            String userText = username.getText().toString().trim();
            String passText = password.getText().toString();
            new Thread(() -> {
                try {
                    JSONObject result = Api.login(serverText, userText, passText);
                    JSONObject user = result.getJSONObject("user");
                    Prefs.save(this, serverText, result.getString("token"), user.getString("fullName"), user.getString("role"));
                    runOnUiThread(this::openHome);
                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        signIn.setEnabled(true);
                        message.setText(ex.getMessage());
                    });
                }
            }).start();
        });
    }

    private void openHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
