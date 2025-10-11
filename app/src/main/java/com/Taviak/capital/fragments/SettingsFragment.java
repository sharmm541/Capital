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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private TextView versionText;
    private DataManager dataManager;

    private boolean isInitialLoad = true;
    private boolean isThemeChanging = false;

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
    }

    private void initViews(View view) {
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        darkThemeSwitch = view.findViewById(R.id.darkThemeSwitch);
        clearDataButton = view.findViewById(R.id.clearDataButton);
        deleteProfileButton = view.findViewById(R.id.deleteProfileButton);
        versionText = view.findViewById(R.id.versionText);

        dataManager = new DataManager(requireContext());
        setAppVersion();

        // Скрываем кнопку удаления профиля если пользователь не авторизован
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            deleteProfileButton.setVisibility(View.GONE);
            Log.d(TAG, "Пользователь не авторизован, скрываем кнопку удаления");
        } else {
            Log.d(TAG, "Пользователь авторизован: " + currentUser.getEmail());
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

        // Темная тема - мгновенное применение без провисания
        darkThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isInitialLoad || isThemeChanging) return;

            isThemeChanging = true;
            saveSetting(KEY_DARK_THEME, isChecked);

            // Применяем тему с небольшой задержкой для плавности
            new android.os.Handler().postDelayed(() -> {
                applyThemeSilent(isChecked);
                isThemeChanging = false;

                // Показываем сообщение после применения темы
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
    }

    private void loadSettings() {
        isInitialLoad = true;

        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
        boolean darkThemeEnabled = sharedPreferences.getBoolean(KEY_DARK_THEME, false);

        notificationsSwitch.setChecked(notificationsEnabled);
        darkThemeSwitch.setChecked(darkThemeEnabled);

        // Применяем текущие настройки уведомлений
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

            // Сбрасываем настройки к значениям по умолчанию
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Применяем настройки по умолчанию
            applyDefaultSettings();

            // Перезагружаем настройки
            loadSettings();

            Log.d(TAG, "Очистка данных завершена успешно");
        } else {
            Toast.makeText(requireContext(), "Ошибка при очистке данных", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка при очистке данных");
        }
    }

    private void applyDefaultSettings() {
        // Устанавливаем настройки по умолчанию
        saveSetting(KEY_NOTIFICATIONS, true); // Уведомления включены по умолчанию
        saveSetting(KEY_DARK_THEME, false);   // Светлая тема по умолчанию

        // Применяем настройки уведомлений
        updateNotificationsState(true);

        // Применяем светлую тему
        applyThemeSilent(false);

        Log.d(TAG, "Применены настройки по умолчанию");
    }

    private void deleteUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_LONG).show();
            return;
        }

        // Показываем индикатор загрузки
        Toast.makeText(requireContext(), "Удаление профиля...", Toast.LENGTH_SHORT).show();

        // Удаляем пользователя из Firebase Authentication
        currentUser.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Очищаем локальные данные
                        dataManager.clearAllData();

                        // Очищаем настройки
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();

                        Toast.makeText(requireContext(), "Профиль успешно удален", Toast.LENGTH_SHORT).show();

                        // Перенаправляем на AuthActivity
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
            // Если произошла ошибка, просто закрываем активность
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    // МЕТОДЫ ДЛЯ ТЕМЫ - мгновенное применение
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
    }
}