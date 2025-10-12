package com.Taviak.capital;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.UserDao;
import com.Taviak.capital.entities.User;
import com.Taviak.capital.managers.AuthManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class SplashActivity extends AppCompatActivity {

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
        setContentView(R.layout.fragment_splash); // используем тот же layout

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
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getDatabase(SplashActivity.this);
                UserDao userDao = database.userDao();

                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                User currentUser = null;

                if (firebaseUser != null) {
                    currentUser = userDao.getUserByUid(firebaseUser.getUid());

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

                final String userName = (currentUser != null) ? currentUser.getName() : null;
                final byte[] profileImageBytes = (currentUser != null) ? currentUser.getProfileImage() : null;

                runOnUiThread(() -> {
                    if (appNameTextView != null) {
                        appNameTextView.setText(userName != null ? userName : "Пользователь");
                    }

                    if (profileIcon != null) {
                        if (profileImageBytes != null && profileImageBytes.length > 0) {
                            try {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(profileImageBytes, 0, profileImageBytes.length);
                                if (bitmap != null) {
                                    RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(
                                            getResources(), bitmap);
                                    roundedDrawable.setCircular(true);
                                    roundedDrawable.setAntiAlias(true);
                                    profileIcon.setImageDrawable(roundedDrawable);
                                } else {
                                    setDefaultProfileImage();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                setDefaultProfileImage();
                            }
                        } else {
                            setDefaultProfileImage();
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (appNameTextView != null) appNameTextView.setText("Пользователь");
                    setDefaultProfileImage();
                });
            }
        }).start();
    }

    private void setDefaultProfileImage() {
        if (profileIcon != null) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile);
            if (bitmap != null) {
                RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                roundedDrawable.setCircular(true);
                roundedDrawable.setAntiAlias(true);
                profileIcon.setImageDrawable(roundedDrawable);
            } else {
                profileIcon.setImageResource(R.drawable.ic_profile);
            }
        }
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
            return;
        }
        isNavigationHandled = true;

        // ВАЖНО: Используем обновленный метод проверки
        boolean isLoggedIn = authManager.isUserLoggedIn();

        Class<?> targetActivity;
        if (isLoggedIn) {
            targetActivity = MainActivity.class;
        } else {
            targetActivity = AuthActivity.class;
        }

        Intent intent = new Intent(this, targetActivity);
        // Очищаем стек полностью
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