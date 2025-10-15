package com.Taviak.capital;

import android.app.Application;
import com.Taviak.capital.database.AppDatabase;
import com.google.firebase.FirebaseApp;

public class CapitalApp extends Application {
    private static CapitalApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Инициализация Firebase
        FirebaseApp.initializeApp(this);
    }

    // Статический метод для получения экземпляра приложения
    public static CapitalApp getInstance() {
        return instance;
    }

    // Метод для получения базы данных
    public AppDatabase getDatabase() {
        return AppDatabase.getDatabase(this);
    }
}