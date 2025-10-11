package com.Taviak.capital;

import android.app.Application;
import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.repository.IncomeRepository;
import com.google.firebase.FirebaseApp;

public class CapitalApp extends Application {
    private IncomeRepository incomeRepository;
    private static CapitalApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Инициализация Firebase
        FirebaseApp.initializeApp(this);
        // Инициализация базы данных и репозитория
        AppDatabase database = AppDatabase.getDatabase(this);
        incomeRepository = new IncomeRepository(database);
    }

    public IncomeRepository getIncomeRepository() {
        return incomeRepository;
    }

    // Статический метод для получения базы данных
    public static AppDatabase getDatabase(Application application) {
        return AppDatabase.getDatabase(application);
    }

    // Статический метод для получения экземпляра приложения
    public static CapitalApp getInstance() {
        return instance;
    }
}