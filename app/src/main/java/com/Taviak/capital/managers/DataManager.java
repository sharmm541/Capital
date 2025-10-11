package com.Taviak.capital.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.Taviak.capital.database.AppDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class DataManager {

    private Context context;
    private SharedPreferences sharedPreferences;
    private AppDatabase appDatabase;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    public DataManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences("app_data", Context.MODE_PRIVATE);
        this.appDatabase = AppDatabase.getDatabase(context);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public boolean clearAllData() {
        try {
            // 1. Очищаем SharedPreferences
            clearSharedPreferences();

            // 2. Очищаем Room Database
            clearRoomDatabase();

            // 3. Очищаем файлы
            clearAppFiles();

            // 4. Очищаем кэш
            clearCache();

            // 5. Очищаем внешние файлы (если есть)
            clearExternalFiles();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void clearSharedPreferences() {
        try {
            // Очищаем основные SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Очищаем другие возможные SharedPreferences
            String[] prefsNames = {"app_settings", "user_prefs", "goal_data", "transaction_data"};
            for (String prefName : prefsNames) {
                SharedPreferences otherPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                otherPrefs.edit().clear().apply();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearRoomDatabase() {
        try {
            if (appDatabase != null) {
                // Очищаем все таблицы через DAO
                new Thread(() -> {
                    try {
                        appDatabase.transactionDao().deleteAllTransactions();
                        appDatabase.goalDao().deleteAllGoals();
                        appDatabase.incomeDao().deleteAllIncomes();
                        appDatabase.userDao().deleteAllUsers();

                        // Закрываем базу данных
                        appDatabase.close();

                        // Удаляем файл базы данных Room
                        String[] databaseList = context.databaseList();
                        for (String databaseName : databaseList) {
                            if (databaseName.contains("capital-database")) {
                                context.deleteDatabase(databaseName);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearAppFiles() {
        try {
            // Очищаем внутреннюю директорию files
            File filesDir = context.getFilesDir();
            if (filesDir != null && filesDir.exists()) {
                deleteRecursive(filesDir);
                // Восстанавливаем базовую структуру
                filesDir.mkdirs();
            }

            // Очищаем директорию для backup файлов
            File backupDir = new File(context.getFilesDir(), "backups");
            if (backupDir.exists()) {
                deleteRecursive(backupDir);
            }

            // Очищаем директорию для экспорта
            File exportDir = new File(context.getFilesDir(), "exports");
            if (exportDir.exists()) {
                deleteRecursive(exportDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearCache() {
        try {
            // Очищаем внутренний кэш
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir);
                // Восстанавливаем директорию
                cacheDir.mkdirs();
            }

            // Очищаем внешний кэш (если доступен)
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteRecursive(externalCacheDir);
                externalCacheDir.mkdirs();
            }

            // Очищаем кэш WebView (если используется)
            try {
                Context webViewContext = context.createPackageContext("com.google.android.webview", 0);
                File webViewCache = webViewContext.getCacheDir();
                if (webViewCache != null && webViewCache.exists()) {
                    deleteRecursive(webViewCache);
                }
            } catch (Exception e) {
                // WebView может быть не установлен - игнорируем
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearExternalFiles() {
        try {
            // Очищаем файлы во внешнем хранилище (если разрешено)
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File externalFilesDir = context.getExternalFilesDir(null);
                if (externalFilesDir != null && externalFilesDir.exists()) {
                    deleteRecursive(externalFilesDir);
                    externalFilesDir.mkdirs();
                }

                // Очищаем публичные директории приложения
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS + "/" + context.getPackageName());
                if (downloadsDir.exists()) {
                    deleteRecursive(downloadsDir);
                }

                File documentsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS + "/" + context.getPackageName());
                if (documentsDir.exists()) {
                    deleteRecursive(documentsDir);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Удаление аккаунта Firebase
     */
    public void deleteFirebaseAccount(OnAccountDeleteListener listener) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            listener.onError("Пользователь не авторизован");
            return;
        }

        // Сначала удаляем данные из Firestore
        deleteUserDataFromFirestore(currentUser.getUid(), new OnFirestoreDeleteListener() {
            @Override
            public void onSuccess() {
                // Затем удаляем аккаунт из Firebase Auth
                currentUser.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Выходим из аккаунта
                                firebaseAuth.signOut();
                                // Очищаем локальные данные
                                clearAllData();
                                listener.onSuccess();
                            } else {
                                listener.onError("Ошибка удаления аккаунта: " + task.getException().getMessage());
                            }
                        });
            }

            @Override
            public void onError(String error) {
                listener.onError("Ошибка удаления данных: " + error);
            }
        });
    }

    /**
     * Удаление данных пользователя из Firestore
     */
    private void deleteUserDataFromFirestore(String userId, OnFirestoreDeleteListener listener) {
        // Удаляем все коллекции пользователя
        firestore.collection("users").document(userId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Удаляем транзакции пользователя
                        firestore.collection("transactions")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnCompleteListener(transactionTask -> {
                                    if (transactionTask.isSuccessful()) {
                                        for (var document : transactionTask.getResult()) {
                                            document.getReference().delete();
                                        }

                                        // Удаляем цели пользователя
                                        firestore.collection("goals")
                                                .whereEqualTo("userId", userId)
                                                .get()
                                                .addOnCompleteListener(goalTask -> {
                                                    if (goalTask.isSuccessful()) {
                                                        for (var document : goalTask.getResult()) {
                                                            document.getReference().delete();
                                                        }
                                                        listener.onSuccess();
                                                    } else {
                                                        listener.onError("Ошибка удаления целей");
                                                    }
                                                });
                                    } else {
                                        listener.onError("Ошибка удаления транзакций");
                                    }
                                });
                    } else {
                        listener.onError("Ошибка удаления пользователя из Firestore");
                    }
                });
    }

    /**
     * Рекурсивное удаление файлов и директорий
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) {
            return;
        }

        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }

        // Не удаляем саму корневую директорию, только её содержимое
        if (!fileOrDirectory.equals(context.getFilesDir()) &&
                !fileOrDirectory.equals(context.getCacheDir()) &&
                !fileOrDirectory.equals(context.getExternalCacheDir())) {
            fileOrDirectory.delete();
        } else {
            // Для корневых директорий только очищаем содержимое
            if (fileOrDirectory.isDirectory()) {
                File[] files = fileOrDirectory.listFiles();
                if (files != null) {
                    for (File child : files) {
                        if (!child.isDirectory() ||
                                !child.getName().equals("lib") &&  // Сохраняем нативные библиотеки
                                        !child.getName().equals("code_cache")) { // Сохраняем кэш кода
                            deleteRecursive(child);
                        }
                    }
                }
            }
        }
    }

    /**
     * Получает размер всех данных приложения в байтах
     */
    public long getAppDataSize() {
        long totalSize = 0;

        try {
            // Размер SharedPreferences (приблизительно)
            totalSize += getSharedPreferencesSize();

            // Размер базы данных Room
            totalSize += getRoomDatabaseSize();

            // Размер файлов
            totalSize += getDirectorySize(context.getFilesDir());

            // Размер кэша
            totalSize += getDirectorySize(context.getCacheDir());

            // Размер внешнего кэша
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null) {
                totalSize += getDirectorySize(externalCacheDir);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalSize;
    }

    private long getSharedPreferencesSize() {
        try {
            File prefsDir = new File(context.getFilesDir().getParent() + "/shared_prefs");
            if (prefsDir.exists()) {
                return getDirectorySize(prefsDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private long getRoomDatabaseSize() {
        long size = 0;
        try {
            String[] databaseList = context.databaseList();
            for (String databaseName : databaseList) {
                if (databaseName.contains("capital-database")) {
                    File dbFile = context.getDatabasePath(databaseName);
                    if (dbFile.exists()) {
                        size += dbFile.length();
                    }

                    // Также учитываем журнальные файлы Room
                    File[] journalFiles = new File[]{
                            new File(dbFile.getPath() + "-wal"),
                            new File(dbFile.getPath() + "-shm")
                    };

                    for (File journalFile : journalFiles) {
                        if (journalFile.exists()) {
                            size += journalFile.length();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    }
                }
            }
        }
        return size;
    }

    /**
     * Форматирует размер в читаемый вид
     */
    public String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // Дополнительные методы для управления данными
    public void saveData(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getData(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void removeData(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    /**
     * Проверяет, есть ли данные для очистки
     */
    public boolean hasDataToClear() {
        return getAppDataSize() > 0;
    }

    /**
     * Проверяет, авторизован ли пользователь в Firebase
     */
    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Интерфейсы для колбэков
    public interface OnAccountDeleteListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnFirestoreDeleteListener {
        void onSuccess();
        void onError(String error);
    }
}