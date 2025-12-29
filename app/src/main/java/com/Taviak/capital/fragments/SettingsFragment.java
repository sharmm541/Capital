package com.Taviak.capital.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.Taviak.capital.R;
import com.Taviak.capital.managers.DataManager;
import com.Taviak.capital.managers.FirebaseBackupManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_THEME = "dark_theme_enabled";
    private static final String TAG = "SettingsFragment";

    private SharedPreferences sharedPreferences;
    private SwitchCompat notificationsSwitch;
    private SwitchCompat darkThemeSwitch;
    private MaterialButton clearDataButton;
    private MaterialButton deleteProfileButton;
    private MaterialButton backupButton;
    private MaterialButton restoreButton;
    private MaterialButton exportButton;
    private FirebaseBackupManager backupManager;
    private TextView versionText;
    private DataManager dataManager;

    private boolean isInitialLoad = true;
    private boolean isThemeChanging = false;
    private boolean isFirestoreAvailable = false;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupSharedPreferences();
        setupClickListeners();
        loadSettings();
        setupFirebaseListeners();
    }

    private void initViews(View view) {
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        darkThemeSwitch = view.findViewById(R.id.darkThemeSwitch);
        clearDataButton = view.findViewById(R.id.clearDataButton);
        deleteProfileButton = view.findViewById(R.id.deleteProfileButton);
        versionText = view.findViewById(R.id.versionText);
        backupButton = view.findViewById(R.id.backupButton);
        restoreButton = view.findViewById(R.id.restoreButton);
        exportButton = view.findViewById(R.id.exportButton);

        backupManager = new FirebaseBackupManager(requireContext());
        dataManager = new DataManager(requireContext());
        setAppVersion();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Показываем/скрываем кнопки в зависимости от авторизации
        if (currentUser == null) {
            deleteProfileButton.setVisibility(View.GONE);
            backupButton.setVisibility(View.GONE);
            restoreButton.setVisibility(View.GONE);
            exportButton.setVisibility(View.GONE);
            Log.d(TAG, "Пользователь не авторизован, скрываем Firebase-кнопки");
        } else {
            Log.d(TAG, "Пользователь авторизован: " + currentUser.getEmail());
            // Проверяем соединение с Firestore
            checkFirebaseConnection();
        }
    }

    private void setupSharedPreferences() {
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void setupClickListeners() {
        // Уведомления
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitialLoad) return;

            saveSetting(KEY_NOTIFICATIONS, isChecked);
            updateNotificationsState(isChecked);

            if (isChecked) {
                Toast.makeText(requireContext(), "Уведомления включены", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Уведомления выключены", Toast.LENGTH_SHORT).show();
            }
        });

        // Темная тема
        darkThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitialLoad || isThemeChanging) return;

            isThemeChanging = true;
            saveSetting(KEY_DARK_THEME, isChecked);

            new android.os.Handler().postDelayed(() -> {
                applyThemeSilent(isChecked);
                isThemeChanging = false;

                if (isChecked) {
                    Toast.makeText(requireContext(), "Темная тема включена", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Светлая тема включена", Toast.LENGTH_SHORT).show();
                }
            }, 50);
        });

        // Очистка данных
        clearDataButton.setOnClickListener(v -> showClearDataDialog());

        // Удаление профиля
        deleteProfileButton.setOnClickListener(v -> showDeleteProfileDialog());

        // Создание резервной копии
        backupButton.setOnClickListener(v -> {
            if (isFirestoreAvailable) {
                showBackupDialog();
            } else {
                Toast.makeText(requireContext(),
                        "Нет соединения с облаком. Проверьте интернет и повторите попытку.",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Восстановление из резервной копии
        restoreButton.setOnClickListener(v -> {
            if (isFirestoreAvailable) {
                showBackupSelectionDialog();
            } else {
                Toast.makeText(requireContext(),
                        "Нет соединения с облаком. Проверьте интернет и повторите попытку.",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Экспорт данных
        exportButton.setOnClickListener(v -> {
            if (isFirestoreAvailable) {
                exportData();
            } else {
                Toast.makeText(requireContext(),
                        "Нет соединения с облаком. Проверьте интернет и повторите попытку.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupFirebaseListeners() {
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null && isFirestoreAvailable) {
                requireActivity().runOnUiThread(() -> {
                    backupButton.setEnabled(true);
                    restoreButton.setEnabled(true);
                    exportButton.setEnabled(true);
                });
            }
        });
    }

    private void showBackupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Создание резервной копии");
        builder.setMessage("Создать резервную копию ваших данных в облаке? Все данные будут сохранены безопасно.");

        builder.setPositiveButton("Создать", (dialog, which) -> {
            backupManager.createBackup();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showBackupSelectionDialog() {
        backupManager.listBackups(new FirebaseBackupManager.BackupListCallback() {
            @Override
            public void onSuccess(List<FirebaseBackupManager.BackupInfo> backups) {
                if (backups.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "Резервные копии не найдены. Сначала создайте резервную копию.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String[] backupItems = new String[backups.size()];
                for (int i = 0; i < backups.size(); i++) {
                    FirebaseBackupManager.BackupInfo info = backups.get(i);
                    backupItems[i] = String.format(Locale.getDefault(),
                            "%s\nТранзакций: %d | Целей: %d\n%s",
                            info.getFormattedDate(),
                            info.transactionCount,
                            info.goalCount,
                            info.deviceInfo != null ? info.deviceInfo : "");
                }

                requireActivity().runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
                    builder.setTitle("Выберите резервную копию для восстановления");
                    builder.setItems(backupItems, (dialog, which) -> {
                        FirebaseBackupManager.BackupInfo selectedBackup = backups.get(which);
                        showRestoreConfirmationDialog(selectedBackup);
                    });
                    builder.setNegativeButton("Отмена", null);
                    builder.show();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Ошибка загрузки резервных копий: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showRestoreConfirmationDialog(FirebaseBackupManager.BackupInfo backup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Подтверждение восстановления");
        builder.setMessage(String.format(Locale.getDefault(),
                "Восстановить данные из резервной копии от %s?\n\n" +
                        "Транзакций: %d\n" +
                        "Целей: %d\n\n" +
                        "ВНИМАНИЕ: Все текущие данные будут заменены!",
                backup.getFormattedDate(),
                backup.transactionCount,
                backup.goalCount));

        builder.setPositiveButton("Восстановить", (dialog, which) -> {
            backupManager.restoreFromBackup(backup.id);
        });

        builder.setNeutralButton("Удалить копию", (dialog, which) -> {
            showDeleteBackupDialog(backup);
        });

        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteBackupDialog(FirebaseBackupManager.BackupInfo backup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Удаление резервной копии");
        builder.setMessage("Удалить резервную копию от " + backup.getFormattedDate() + "?");

        builder.setPositiveButton("Удалить", (dialog, which) -> {
            backupManager.deleteBackup(backup.id);
        });

        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void exportData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Экспорт данных");
        builder.setMessage("Экспортировать все данные в отдельные коллекции Firestore? Это полезно для аналитики.");

        builder.setPositiveButton("Экспортировать", (dialog, which) -> {
            backupManager.exportToSeparateCollections();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void checkFirebaseConnection() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Используем document() вместо get() для быстрой проверки
        firestore.collection("metadata").document("connection")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Firestore connection: SUCCESS");
                    isFirestoreAvailable = true;
                    updateUIForFirestore(true);

                    // Создаем тестовый документ если его нет
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("lastChecked", new Date());
                        data.put("appName", "Capital Finance");
                        firestore.collection("metadata").document("connection")
                                .set(data, SetOptions.merge());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore connection: FAILED", e);
                    isFirestoreAvailable = false;
                    updateUIForFirestore(false);

                    // Анализируем тип ошибки
                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                        switch (firestoreException.getCode()) {
                            case PERMISSION_DENIED:
                                Toast.makeText(requireContext(),
                                        "Ошибка доступа к данным. Проверьте правила Firestore.",
                                        Toast.LENGTH_LONG).show();
                                break;
                            case UNAVAILABLE:
                            case DEADLINE_EXCEEDED:
                                Toast.makeText(requireContext(),
                                        "Нет соединения с интернетом. Проверьте подключение.",
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(requireContext(),
                                        "Ошибка соединения: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnCompleteListener(task -> {
                });
    }

    private void updateUIForFirestore(boolean isAvailable) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (backupButton != null) {
                backupButton.setEnabled(isAvailable);
                backupButton.setAlpha(isAvailable ? 1.0f : 0.5f);
            }
            if (restoreButton != null) {
                restoreButton.setEnabled(isAvailable);
                restoreButton.setAlpha(isAvailable ? 1.0f : 0.5f);
            }
            if (exportButton != null) {
                exportButton.setEnabled(isAvailable);
                exportButton.setAlpha(isAvailable ? 1.0f : 0.5f);
            }
        });
    }

    private void loadSettings() {
        isInitialLoad = true;

        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
        boolean darkThemeEnabled = sharedPreferences.getBoolean(KEY_DARK_THEME, false);

        notificationsSwitch.setChecked(notificationsEnabled);
        darkThemeSwitch.setChecked(darkThemeEnabled);

        updateNotificationsState(notificationsEnabled);
        isInitialLoad = false;
    }

    private void saveSetting(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void updateNotificationsState(boolean enabled) {
        if (getActivity() instanceof com.Taviak.capital.MainActivity) {
            com.Taviak.capital.MainActivity mainActivity = (com.Taviak.capital.MainActivity) getActivity();
            if (enabled) {
                mainActivity.scheduleRandomNotifications();
                Log.d(TAG, "Уведомления включены");
            } else {
                mainActivity.cancelScheduledNotifications();
                Log.d(TAG, "Уведомления выключены");
            }
        }
    }

    private void setAppVersion() {
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;
            versionText.setText("Версия: " + versionName);
        } catch (Exception e) {
            versionText.setText("Версия: 1.0.0");
        }
    }

    private void showClearDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Очистка данных");
        builder.setMessage("Вы уверены, что хотите удалить все данные? Это действие нельзя отменить. Будут удалены все цели, транзакции и настройки.");

        builder.setPositiveButton("Очистить", (dialog, which) -> clearAllData());
        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.status_error));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
        }
    }

    private void showDeleteProfileDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Удаление профиля");
        builder.setMessage("ВНИМАНИЕ! Это действие нельзя отменить.\n\nБудут безвозвратно удалены:\n• Ваш профиль\n• Все финансовые данные\n• История транзакций\n• Настройки приложения\n\nВы уверены, что хотите удалить профиль?");

        builder.setPositiveButton("УДАЛИТЬ ПРОФИЛЬ", (dialog, which) -> deleteUserProfile());
        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.status_error));
            positiveButton.setAllCaps(false);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
        }
    }

    private void clearAllData() {
        Log.d(TAG, "Начало очистки данных");

        boolean success = dataManager.clearAllData();
        if (success) {
            Toast.makeText(requireContext(), "Все данные очищены", Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            applyDefaultSettings();
            loadSettings();
            Log.d(TAG, "Очистка данных завершена успешно");
        } else {
            Toast.makeText(requireContext(), "Ошибка при очистке данных", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка при очистке данных");
        }
    }

    private void applyDefaultSettings() {
        saveSetting(KEY_NOTIFICATIONS, true);
        saveSetting(KEY_DARK_THEME, false);
        updateNotificationsState(true);
        applyThemeSilent(false);
        Log.d(TAG, "Применены настройки по умолчанию");
    }

    private void deleteUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(requireContext(), "Удаление профиля...", Toast.LENGTH_SHORT).show();

        currentUser.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dataManager.clearAllData();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        Toast.makeText(requireContext(), "Профиль успешно удален", Toast.LENGTH_SHORT).show();
                        redirectToAuthActivity();
                    } else {
                        String errorMessage = "Ошибка при удалении профиля: " +
                                (task.getException() != null ? task.getException().getMessage() : "Неизвестная ошибка");
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMessage = "Ошибка при удалении профиля: " + e.getMessage();
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private void redirectToAuthActivity() {
        try {
            Intent intent = new Intent(requireContext(), com.Taviak.capital.AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    private void applyThemeSilent(boolean darkThemeEnabled) {
        if (darkThemeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Log.d(TAG, "Применена темная тема");
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Log.d(TAG, "Применена светлая тема");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSettings();

        // Проверяем соединение при возвращении на экран
        if (mAuth.getCurrentUser() != null) {
            checkFirebaseConnection();
        }
    }
}