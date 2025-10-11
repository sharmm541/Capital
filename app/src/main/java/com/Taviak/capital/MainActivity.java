package com.Taviak.capital;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.Taviak.capital.fragments.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String CHANNEL_ID = "capital_app_channel";
    private static final int ALARM_REQUEST_CODE = 2001;
    private static final String KEY_FIRST_RUN = "first_run";

    private BottomNavigationView bottomNavigation;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private SharedPreferences sharedPreferences;
    private AlertDialog currentDialog;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // Сохраняем текущий фрагмент
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof SettingsFragment) {
            outState.putBoolean("is_settings_open", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Делаем полноэкранный режим
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        applyThemeOnCreate();
        setContentView(R.layout.activity_main);

        initSharedPreferences();
        setupNotificationChannel();
        initViews();
        setupBottomNavigation();
        setupNavigationDrawer();

        if (savedInstanceState != null && savedInstanceState.getBoolean("is_settings_open", false)) {
            loadFragment(new SettingsFragment());
            clearBottomNavigationSelection();
        } else {
            showMainFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Проверяем первый запуск с задержкой, когда активность полностью создана
        new Handler().postDelayed(() -> {
            checkFirstRunAndRequestPermissions();
        }, 1000);

        // При возвращении в приложение проверяем настройки уведомлений
        checkAndScheduleNotifications();
        applyBackgroundToFragments();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Закрываем диалог при паузе активности
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }

    private void applyThemeOnCreate() {
        SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean darkThemeEnabled = prefs.getBoolean("dark_theme_enabled", false);

        if (darkThemeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void checkFirstRunAndRequestPermissions() {
        boolean firstRun = sharedPreferences.getBoolean(KEY_FIRST_RUN, true);

        if (firstRun) {
            // Показываем диалог с запросом разрешений при первом запуске
            showFirstRunPermissionDialog();
        } else {
            // При последующих запусках просто проверяем и планируем уведомления
            checkAndScheduleNotifications();
        }
    }

    private void showFirstRunPermissionDialog() {
        try {
            // Закрываем предыдущий диалог если есть
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Разрешение уведомлений");
            builder.setMessage("Для работы напоминаний о финансовых целях приложению нужны уведомления. Разрешите их в настройках системы для лучшего опыта использования.");

            builder.setPositiveButton("Разрешить уведомления", (dialog, which) -> {
                markFirstRunCompleted();
                openNotificationSettings();
            });

            builder.setNegativeButton("Позже", (dialog, which) -> {
                markFirstRunCompleted();
                Toast.makeText(this, "Вы можете включить уведомления позже в настройках приложения", Toast.LENGTH_LONG).show();
                checkAndScheduleNotifications();
            });

            // Важные настройки для предотвращения автоматического закрытия
            builder.setCancelable(false);

            currentDialog = builder.create();
            currentDialog.setCanceledOnTouchOutside(false);

            // Показываем диалог с небольшой задержкой
            new Handler().postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    currentDialog.show();
                }
            }, 500);

        } catch (Exception e) {
            Log.e("MainActivity", "Error showing first run dialog", e);
        }
    }

    private void markFirstRunCompleted() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_FIRST_RUN, false);
        editor.apply();
    }

    private void openNotificationSettings() {
        try {
            Intent intent = new Intent();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
            }

            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Откройте настройки приложения вручную", Toast.LENGTH_LONG).show();
            }
        }

        // Планируем уведомления после открытия настроек
        checkAndScheduleNotifications();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Финансовые напоминания";
            String description = "Уведомления о финансовых целях и бюджете";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 100, 200});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkAndScheduleNotifications() {
        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);

        if (notificationsEnabled) {
            scheduleRandomNotifications();
        } else {
            cancelScheduledNotifications();
        }
    }

    // Публичные методы для управления уведомлениями
    public void scheduleRandomNotifications() {
        cancelScheduledNotifications();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Random random = new Random();
        long firstDelay = getRandomDelayInMillis(random);

        Intent notificationIntent = new Intent(this, NotificationReceiver.class);
        notificationIntent.setAction("SHOW_FINANCE_NOTIFICATION");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long firstTriggerTime = System.currentTimeMillis() + firstDelay;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, firstTriggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, firstTriggerTime, pendingIntent);
        }
    }

    public void cancelScheduledNotifications() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private long getRandomDelayInMillis(Random random) {
        int hours = 2 + random.nextInt(5);
        return hours * 60 * 60 * 1000L;
    }

    private void applyBackgroundToFragments() {
        SharedPreferences prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean darkThemeEnabled = prefs.getBoolean("dark_theme_enabled", false);

        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            if (darkThemeEnabled) {
                fragmentContainer.setBackgroundResource(R.drawable.background_dark_gradient);
            } else {
                fragmentContainer.setBackgroundResource(R.drawable.gradient_background_light);
            }
        }

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && currentFragment.getView() != null) {
            if (darkThemeEnabled) {
                currentFragment.getView().setBackgroundResource(R.drawable.background_dark_gradient);
            } else {
                currentFragment.getView().setBackgroundResource(R.drawable.gradient_background_light);
            }
        }
    }

    // Остальные методы навигации без изменений
    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
    }


    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                fragment = new MainFragment();
            } else if (itemId == R.id.nav_income) {
                fragment = new IncomeFragment();
            } else if (itemId == R.id.nav_expenses) {
                fragment = new ExpensesFragment();
            } else if (itemId == R.id.nav_goals) {
                fragment = new GoalsFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                drawerLayout.close();
                return true;
            }

            return false;
        });
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                fragment = new MainFragment();
                bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
            } else if (itemId == R.id.nav_income) {
                fragment = new IncomeFragment();
                bottomNavigation.setSelectedItemId(R.id.nav_income);
            } else if (itemId == R.id.nav_expenses) {
                fragment = new ExpensesFragment();
                bottomNavigation.setSelectedItemId(R.id.nav_expenses);
            } else if (itemId == R.id.nav_goals) {
                fragment = new GoalsFragment();
                bottomNavigation.setSelectedItemId(R.id.nav_goals);
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
                bottomNavigation.setSelectedItemId(R.id.nav_profile);
            }
            else if (itemId == getResources().getIdentifier("nav_currency", "id", getPackageName())) {
                fragment = new CurrencyFragment();
                clearBottomNavigationSelection();
            } else if (itemId == getResources().getIdentifier("nav_settings", "id", getPackageName())) {
                fragment = new SettingsFragment();
                clearBottomNavigationSelection();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }

            drawerLayout.close();
            return true;
        });
    }

    private void clearBottomNavigationSelection() {
        bottomNavigation.setSelectedItemId(-1);
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        // Применяем фон после загрузки фрагмента
        new Handler().postDelayed(() -> applyBackgroundToFragments(), 100);
    }

    public void showMainFragment() {
        bottomNavigation.setVisibility(View.VISIBLE);
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MainFragment())
                .commit();

        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
    }

    public void showCurrencyFragment() {
        loadFragment(new CurrencyFragment());
        clearBottomNavigationSelection();
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof MainFragment) {
            super.onBackPressed();
        } else {
            showMainFragment();
        }
    }
}