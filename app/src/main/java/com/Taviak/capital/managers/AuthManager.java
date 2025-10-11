package com.Taviak.capital.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private static AuthManager instance;
    private SharedPreferences sharedPreferences;

    private AuthManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public void setLoggedIn(boolean loggedIn) {
        sharedPreferences.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    // ДОБАВЛЯЕМ ЭТОТ МЕТОД (синоним для isLoggedIn)
    public boolean isUserLoggedIn() {
        return isLoggedIn();
    }

    public void setUserId(String userId) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    public void setUserEmail(String email) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, "");
    }

    public void setUserName(String name) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    // ДОБАВЛЯЕМ МЕТОД login
    public void login(String email) {
        setLoggedIn(true);
        setUserEmail(email);
        // Генерируем временный ID
        setUserId("user_" + System.currentTimeMillis());
    }

    public void logout() {
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .putString(KEY_USER_ID, "")
                .putString(KEY_USER_EMAIL, "")
                .putString(KEY_USER_NAME, "")
                .apply();
    }
}