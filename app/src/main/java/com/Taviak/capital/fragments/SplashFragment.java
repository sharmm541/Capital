package com.Taviak.capital.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.Taviak.capital.AuthActivity;
import com.Taviak.capital.MainActivity;
import com.Taviak.capital.R;
import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.UserDao;
import com.Taviak.capital.entities.User;
import com.Taviak.capital.managers.AuthManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashFragment extends AppCompatActivity {

    private AuthManager authManager;
    private FirebaseAuth mAuth;
    private TextView appNameTextView;
    private TextView greetingText;
    private boolean isNavigationHandled = false;
    private final Handler handler = new Handler();
    private ImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_splash);

        authManager = AuthManager.getInstance(this);
        mAuth = FirebaseAuth.getInstance();
        appNameTextView = findViewById(R.id.appNameTextView);
        greetingText = findViewById(R.id.greetingText);
        profileIcon = findViewById(R.id.profileImage);

        // Сначала устанавливаем приветствие
        if (greetingText != null) {
            greetingText.setText(getTimeBasedGreeting());
        }

        // Загружаем данные пользователя асинхронно
        loadUserData();

        handler.postDelayed(this::navigateToNextScreen, 2000);
    }

    private void loadUserData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                AppDatabase database = AppDatabase.getDatabase(SplashFragment.this);
                UserDao userDao = database.userDao();

                // Получаем текущего пользователя Firebase
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                User currentUser = null;

                if (firebaseUser != null) {
                    // Ищем пользователя в базе по UID
                    currentUser = userDao.getUserByUid(firebaseUser.getUid());

                    // Если пользователь не найден, создаем нового
                    if (currentUser == null) {
                        currentUser = new User();
                        currentUser.setUserid(firebaseUser.getUid());
                        currentUser.setEmail(firebaseUser.getEmail());
                        currentUser.setName(firebaseUser.getDisplayName() != null ?
                                firebaseUser.getDisplayName() : "Пользователь");
                        currentUser.setCreatedAt(System.currentTimeMillis());
                        currentUser.setLoggedin(true);
                        userDao.insert(currentUser);
                    }
                }

                // Подготавливаем данные для UI
                final String userName = (currentUser != null) ? currentUser.getName() : null;
                final byte[] profileImageBytes = (currentUser != null) ? currentUser.getProfileImage() : null;

                handler.post(() -> {
                    // Устанавливаем имя пользователя
                    if (appNameTextView != null) {
                        if (userName != null && !userName.isEmpty()) {
                            appNameTextView.setText(userName);
                        } else {
                            appNameTextView.setText("Пользователь");
                        }
                    }

                    // Устанавливаем фото профиля
                    if (profileIcon != null && profileImageBytes != null && profileImageBytes.length > 0) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(profileImageBytes, 0, profileImageBytes.length);
                            if (bitmap != null) {
                                profileIcon.setImageBitmap(bitmap);
                            } else {
                                // Если декодирование не удалось, используем дефолтную иконку
                                profileIcon.setImageResource(R.drawable.ic_profile);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            profileIcon.setImageResource(R.drawable.ic_profile);
                        }
                    } else {
                        // Если фото нет, используем дефолтную иконку
                        profileIcon.setImageResource(R.drawable.ic_profile);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    if (appNameTextView != null) {
                        appNameTextView.setText("Пользователь");
                    }
                    if (profileIcon != null) {
                        profileIcon.setImageResource(R.drawable.ic_profile);
                    }
                });
            }
        });
    }

    private String getTimeBasedGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) {
            return "Доброе утро!";
        } else if (hour >= 12 && hour < 18) {
            return "Добрый день!";
        } else if (hour >= 18 && hour < 23) {
            return "Добрый вечер!";
        } else {
            return "Доброй ночи!";
        }
    }

    private void navigateToNextScreen() {
        if (isNavigationHandled) {
            return; // Уже обработано
        }
        isNavigationHandled = true;

        Class<?> targetActivity;
        if (authManager.isUserLoggedIn()) {
            targetActivity = MainActivity.class;
        } else {
            targetActivity = AuthActivity.class;
        }

        Intent intent = new Intent(this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        isNavigationHandled = true;
    }
}