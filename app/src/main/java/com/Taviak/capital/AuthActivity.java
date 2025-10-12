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

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private ImageButton btnTogglePassword;
    private ProgressDialog progressDialog;
    private AuthManager authManager;
    private FirebaseAuth firebaseAuth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        authManager = AuthManager.getInstance(this);
        firebaseAuth = FirebaseAuth.getInstance();
        initViews();

        // Проверяем, не залогинен ли пользователь уже
        checkCurrentUser();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        setupListeners();
        setupPasswordToggle();
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void setupPasswordToggle() {
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
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

    private void checkCurrentUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            handleSuccessfulLogin(currentUser);
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (validateInput(email, password)) {
            performLogin(email, password);
        }
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            isValid = false;
        } else {
            etEmail.setError(null);
        }

        if (password.isEmpty()) {
            etPassword.setError("Введите пароль");
            isValid = false;
        } else if (password.length() < 6) {
            etPassword.setError("Пароль должен содержать минимум 6 символов");
            isValid = false;
        } else {
            etPassword.setError(null);
        }

        return isValid;
    }

    private void performLogin(String email, String password) {
        showProgress("Вход...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideProgress();

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            handleSuccessfulLogin(user);
                        }
                    } else {
                        String errorMessage = getErrorMessage(task.getException());
                        Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        etPassword.setError("Неверный email или пароль");
                    }
                });
    }

    private void handleSuccessfulLogin(FirebaseUser user) {
        authManager.setLoggedIn(true);
        authManager.setUserEmail(user.getEmail());
        authManager.setUserId(user.getUid());

        String userName = getUserName(user);
        authManager.setUserName(userName);

        saveUserToDatabase(user.getUid(), user.getEmail(), userName);

        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private String getUserName(FirebaseUser user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        } else {
            return extractNameFromEmail(user.getEmail());
        }
    }

    private String extractNameFromEmail(String email) {
        if (email == null) return "Пользователь";

        try {
            String namePart = email.split("@")[0];
            return namePart.substring(0, 1).toUpperCase() + namePart.substring(1);
        } catch (Exception e) {
            return "Пользователь";
        }
    }

    private void saveUserToDatabase(String userId, String email, String name) {
        UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.registerUser(userId, email, name);
    }

    private String getErrorMessage(Exception exception) {
        if (exception == null) {
            return "Произошла неизвестная ошибка";
        }

        String error = exception.getMessage();
        if (error == null) {
            return "Ошибка аутентификации";
        }

        if (error.contains("password is invalid")) {
            return "Неверный пароль";
        } else if (error.contains("no user record")) {
            return "Пользователь с таким email не найден";
        } else if (error.contains("network error")) {
            return "Проверьте подключение к интернету";
        } else if (error.contains("too many requests")) {
            return "Слишком много попыток входа. Попробуйте позже";
        } else {
            return "Ошибка входа: " + error;
        }
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
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