package com.alice.teacherattendance;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "attendance";

    private Prefs() {}

    public static void save(Context context, String server, String token, String name, String role) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putString("server", trim(server))
                .putString("token", token)
                .putString("name", name)
                .putString("role", role)
                .apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static String server(Context context) {
        return prefs(context).getString("server", "");
    }

    public static String token(Context context) {
        return prefs(context).getString("token", "");
    }

    public static String name(Context context) {
        return prefs(context).getString("name", "");
    }

    public static String role(Context context) {
        return prefs(context).getString("role", "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static String trim(String server) {
        if (server == null) return "";
        String value = server.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
