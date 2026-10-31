package com.alice.teacherattendance;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Api {
    private Api() {}

    public static final class Teacher {
        public final long id;
        public final String fullName;
        public final String department;
        public final boolean hasFace;

        public Teacher(long id, String fullName, String department, boolean hasFace) {
            this.id = id;
            this.fullName = fullName;
            this.department = department == null ? "" : department;
            this.hasFace = hasFace;
        }

        @Override
        public String toString() {
            return fullName + (hasFace ? " · enrolled" : " · no face");
        }
    }

    public static void health(String server) throws Exception {
        JSONObject json = call(server, null, "GET", "/api/health", null);
        if (!"ok".equals(json.optString("status"))) {
            throw new Exception("That address answered, but it is not the school API.");
        }
    }

    public static JSONObject login(String server, String username, String password) throws Exception {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        return call(server, null, "POST", "/api/auth/login", body.toString());
    }

    public static List<Teacher> teachers(Context context) throws Exception {
        JSONObject json = call(Prefs.server(context), Prefs.token(context), "GET", "/api/attendance/sheet", null);
        JSONArray rows = json.getJSONArray("rows");
        List<Teacher> list = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject teacher = rows.getJSONObject(i).getJSONObject("teacher");
            list.add(new Teacher(
                    teacher.getLong("id"),
                    teacher.getString("fullName"),
                    teacher.optString("department", ""),
                    teacher.optBoolean("hasFace")));
        }
        return list;
    }

    public static void enroll(Context context, long teacherId, JSONArray descriptor) throws Exception {
        JSONObject body = new JSONObject();
        body.put("descriptor", descriptor);
        call(Prefs.server(context), Prefs.token(context), "POST", "/api/teachers/" + teacherId + "/face", body.toString());
    }

    public static void removeFace(Context context, long teacherId) throws Exception {
        call(Prefs.server(context), Prefs.token(context), "DELETE", "/api/teachers/" + teacherId + "/face", null);
    }

    public static JSONObject verify(Context context, JSONArray descriptor) throws Exception {
        JSONObject body = new JSONObject();
        body.put("descriptor", descriptor);
        body.put("action", "AUTO");
        return call(Prefs.server(context), Prefs.token(context), "POST", "/api/attendance/face", body.toString());
    }

    private static JSONObject call(String server, String token, String method, String path, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(clean(server) + path).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
        }
        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String text = read(stream);
        if (code >= 400) {
            String message = text;
            try {
                message = new JSONObject(text).optString("error", text);
            } catch (Exception ignored) {
                // The server sometimes returns plain text.
            }
            if (message == null || message.isEmpty()) {
                message = "The school server rejected the request (" + code + ").";
            }
            throw new Exception(message);
        }
        if (text.isEmpty()) {
            return new JSONObject();
        }
        return new JSONObject(text);
    }

    private static String clean(String server) throws Exception {
        if (server == null || server.trim().isEmpty()) {
            throw new Exception("Type the laptop address, for example http://192.168.1.20:8080");
        }
        String value = server.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new Exception("The address must start with http:// and use port 8080.");
        }
        return value;
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        return text.toString();
    }
}
