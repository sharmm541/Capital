package com.Taviak.capital.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private static AuthManager instance;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth firebaseAuth;

    private AuthManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    // ОСНОВНОЙ МЕТОД ПРОВЕРКИ АВТОРИЗАЦИИ
    public boolean isUserLoggedIn() {
        // Проверяем и Firebase, и SharedPreferences
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        boolean isLoggedInPrefs = sharedPreferences.getBoolean(KEY_LOGGED_IN, false);

        // Если в Firebase нет пользователя, но в prefs есть - выходим
        if (firebaseUser == null && isLoggedInPrefs) {
            logout();
            return false;
        }

        // Если в Firebase есть пользователь, но в prefs нет - синхронизируем
        if (firebaseUser != null && !isLoggedInPrefs) {
            setLoggedIn(true);
            setUserEmail(firebaseUser.getEmail());
            setUserId(firebaseUser.getUid());
            if (firebaseUser.getDisplayName() != null) {
                setUserName(firebaseUser.getDisplayName());
            }
            return true;
        }

        return firebaseUser != null && isLoggedInPrefs;
    }

    public void setLoggedIn(boolean loggedIn) {
        sharedPreferences.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    // УДАЛИТЕ этот метод, чтобы избежать путаницы
    // public boolean isLoggedIn() {
    //     return isUserLoggedIn();
    // }

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

    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }

    // ОБНОВЛЕННЫЙ МЕТОД LOGIN
    public void login(String email, String userId) {
        setLoggedIn(true);
        setUserEmail(email);
        setUserId(userId);
    }

    // ОБНОВЛЕННЫЙ МЕТОД LOGOUT
    public void logout() {
        // Выход из Firebase
        firebaseAuth.signOut();

        // Очистка SharedPreferences
        sharedPreferences.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .putString(KEY_USER_ID, "")
                .putString(KEY_USER_EMAIL, "")
                .putString(KEY_USER_NAME, "")
                .apply();
    }

    // Дополнительный метод для полной очистки
    public void clearAuthData() {
        logout();
    }
}