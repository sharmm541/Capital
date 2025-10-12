package com.Taviak.capital;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.Taviak.capital.managers.AuthManager;
import com.Taviak.capital.viewmodels.UserViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister, btnLogin;
    private ImageButton btnTogglePassword, btnToggleConfirmPassword; // Добавьте эти поля
    private ProgressDialog progressDialog;
    private AuthManager authManager;
    private UserViewModel userViewModel;
    private FirebaseAuth firebaseAuth;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = AuthManager.getInstance(this);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        firebaseAuth = FirebaseAuth.getInstance();
        initViews();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);

        // Инициализация кнопок показа пароля
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);

        btnLogin.setOnClickListener(V -> navigateToAuth());
        btnRegister.setOnClickListener(v -> attemptRegistration());

        setupPasswordToggles(); // Добавьте этот вызов
    }

    private void setupPasswordToggles() {
        // Для основного пароля
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        // Для подтверждения пароля
        if (btnToggleConfirmPassword != null) {
            btnToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());
        }
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Скрыть пароль
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            btnTogglePassword.setContentDescription("Показать пароль");
        } else {
            // Показать пароль
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            btnTogglePassword.setContentDescription("Скрыть пароль");
        }

        // Перемещаем курсор в конец текста
        etPassword.setSelection(etPassword.getText().length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            // Скрыть пароль
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off);
            btnToggleConfirmPassword.setContentDescription("Показать пароль");
        } else {
            // Показать пароль
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility);
            btnToggleConfirmPassword.setContentDescription("Скрыть пароль");
        }

        // Перемещаем курсор в конец текста
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        isConfirmPasswordVisible = !isConfirmPasswordVisible;
    }

    // ... остальные методы без изменений
    private void attemptRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (validateInput(name, email, password, confirmPassword)) {
            performRegistration(name, email, password);
        }
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        if (name.isEmpty()) {
            etName.setError("Введите имя и фамилию");
            return false;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Введите пароль");
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Пароль должен содержать минимум 6 символов");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Пароли не совпадают");
            return false;
        }

        return true;
    }

    private void performRegistration(String name, String email, String password) {
        showProgress("Регистрация...");

        // Создаем пользователя в Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Регистрация в Firebase успешна
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Обновляем профиль пользователя с именем
                            updateUserProfile(firebaseUser, name, email);
                        } else {
                            hideProgress();
                            Toast.makeText(RegisterActivity.this, "Ошибка создания пользователя", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Ошибка регистрации в Firebase
                        hideProgress();
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        handleRegistrationError(task.getException());
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String name, String email) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Профиль успешно обновлен
                        handleSuccessfulRegistration(user, name, email);
                    } else {
                        // Профиль не обновлен, но пользователь создан
                        handleSuccessfulRegistration(user, name, email);
                    }
                });
    }

    private void handleSuccessfulRegistration(FirebaseUser user, String name, String email) {
        // Сохраняем пользователя в Room Database
        userViewModel.registerUser(user.getUid(), email, name);

        // Сохраняем в AuthManager
        authManager.setLoggedIn(true);
        authManager.setUserEmail(email);
        authManager.setUserId(user.getUid());
        authManager.setUserName(name);

        hideProgress();

        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();

        // Переходим на MainActivity
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void handleRegistrationError(Exception exception) {
        if (exception != null) {
            String error = exception.getMessage();
            if (error != null) {
                if (error.contains("email address is already in use")) {
                    etEmail.setError("Этот email уже используется");
                } else if (error.contains("password is too weak")) {
                    etPassword.setError("Пароль слишком слабый");
                } else if (error.contains("invalid email")) {
                    etEmail.setError("Неверный формат email");
                }
            }
        }
    }

    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) {
            return "Произошла неизвестная ошибка";
        }

        String error = exception.getMessage();
        if (error == null) {
            return "Ошибка регистрации";
        }

        if (error.contains("email address is already in use")) {
            return "Пользователь с таким email уже существует";
        } else if (error.contains("password is too weak")) {
            return "Пароль слишком слабый. Используйте более сложный пароль";
        } else if (error.contains("invalid email")) {
            return "Неверный формат email адреса";
        } else if (error.contains("network error")) {
            return "Проверьте подключение к интернету";
        } else if (error.contains("too many requests")) {
            return "Слишком много попыток регистрации. Попробуйте позже";
        } else {
            return "Ошибка регистрации: " + error;
        }
    }

    private void navigateToAuth() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
    }

    private void showProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideProgress();
    }
}