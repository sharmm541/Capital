package com.Taviak.capital.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREFS_NAME = "CapitalSettings";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_CURRENCY = "currency";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Уведомления
    public boolean isNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATIONS, enabled);
        editor.apply();
    }

    // Темная тема
    public boolean isDarkThemeEnabled() {
        return sharedPreferences.getBoolean(KEY_DARK_THEME, false);
    }

    public void setDarkThemeEnabled(boolean enabled) {
        editor.putBoolean(KEY_DARK_THEME, enabled);
        editor.apply();
    }

    // Валюта
    public String getCurrency() {
        return sharedPreferences.getString(KEY_CURRENCY, "RUB");
    }

    public void setCurrency(String currency) {
        editor.putString(KEY_CURRENCY, currency);
        editor.apply();
    }

    // Очистка всех данных
    public void clearAllData() {
        editor.clear();
        editor.apply();
    }
}