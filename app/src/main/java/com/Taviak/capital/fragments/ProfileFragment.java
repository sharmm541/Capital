package com.Taviak.capital.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.Taviak.capital.AuthActivity;
import com.Taviak.capital.R;
import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.TransactionDao;
import com.Taviak.capital.database.UserDao;
import com.Taviak.capital.entities.User;
import com.Taviak.capital.models.Transaction;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {
    private EditText userNameEditText;
    private ImageView profileImage;
    private TextView userEmail, registrationDateText, totalTransactionsText, totalBalanceText;
    private MaterialButton saveNameButton;
    private User currentUser;
    private UserDao userDao;
    private FirebaseAuth mAuth;
    private static final int PICK_IMAGE_REQUEST = 1;
    private byte[] selectedImageBytes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initDatabase();
        initFirebase();
        loadUserData();
        setupClickListeners();
    }

    private void initViews(View view) {
        userNameEditText = view.findViewById(R.id.userNameEditText);
        profileImage = view.findViewById(R.id.profileImage);
        userEmail = view.findViewById(R.id.userEmail);
        registrationDateText = view.findViewById(R.id.registrationDateText);
        totalTransactionsText = view.findViewById(R.id.totalTransactionsText);
        totalBalanceText = view.findViewById(R.id.totalBalanceText);
        saveNameButton = view.findViewById(R.id.saveNameButton);

        profileImage.setOnClickListener(v -> openImagePicker());
        view.findViewById(R.id.changePasswordButton).setOnClickListener(v -> changePassword());
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
    }

    private void saveUserName() {
        String newName = userNameEditText.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(requireContext(), "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser != null && !newName.equals(currentUser.getName())) {
            new Thread(() -> {
                currentUser.setName(newName);
                userDao.update(currentUser);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Имя сохранено", Toast.LENGTH_SHORT).show();
                    hideKeyboard();
                });
            }).start();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(userNameEditText.getWindowToken(), 0);
        }
    }

    private void initDatabase() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        userDao = db.userDao();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            Log.d("ProfileFragment", "Firebase user found: " + firebaseUser.getUid());
            new Thread(() -> {
                try {
                    // Проверяем существование таблицы и данные
                    currentUser = userDao.getUserByUid(firebaseUser.getUid());
                    Log.d("ProfileFragment", "User from DB: " + (currentUser != null ? currentUser.getUserid() : "null"));

                    if (currentUser == null) {
                        Log.d("ProfileFragment", "Creating new user in local DB");
                        currentUser = new User();
                        currentUser.setUserid(firebaseUser.getUid());
                        currentUser.setEmail(firebaseUser.getEmail());
                        currentUser.setName(firebaseUser.getDisplayName() != null ?
                                firebaseUser.getDisplayName() : "Пользователь");
                        currentUser.setCreatedAt(System.currentTimeMillis());
                        currentUser.setLoggedin(true);

                        try {
                            userDao.insert(currentUser);
                            Log.d("ProfileFragment", "New user created successfully: " + firebaseUser.getUid());
                        } catch (Exception insertError) {
                            Log.e("ProfileFragment", "Error inserting user: " + insertError.getMessage(), insertError);
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Ошибка создания пользователя", Toast.LENGTH_SHORT).show()
                            );
                            return;
                        }
                    } else {
                        Log.d("ProfileFragment", "Updating existing user");
                        currentUser.setLoggedin(true);
                        userDao.update(currentUser);
                        Log.d("ProfileFragment", "Existing user updated: " + firebaseUser.getUid());
                    }

                    requireActivity().runOnUiThread(() -> {
                        updateUI();
                        //Toast.makeText(requireContext(), "Данные загружены", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    Log.e("ProfileFragment", "Error loading user data: " + e.getMessage(), e);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                        // Показываем базовую информацию из Firebase
                        showBasicFirebaseData(firebaseUser);
                    });
                }
            }).start();
        } else {
            Log.e("ProfileFragment", "No Firebase user found");
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            redirectToAuth();
        }
    }

    private void showBasicFirebaseData(FirebaseUser firebaseUser) {
        try {
            userEmail.setText(firebaseUser.getEmail());
            userNameEditText.setText(firebaseUser.getDisplayName() != null ?
                    firebaseUser.getDisplayName() : "Пользователь");
            registrationDateText.setText("Сегодня");

            totalTransactionsText.setText("0");
            totalBalanceText.setText("0 ₽");
            totalBalanceText.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error showing basic data: " + e.getMessage(), e);
        }
    }

    private void redirectToAuth() {
        try {
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error redirecting to auth: " + e.getMessage(), e);
        }
    }

    private void updateUI() {
        if (currentUser != null) {
            userNameEditText.setText(currentUser.getName());
            userEmail.setText(currentUser.getEmail());

            String registrationDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(new Date(currentUser.getCreatedAt()));
            registrationDateText.setText(registrationDate);

            if (currentUser.getProfileImage() != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        currentUser.getProfileImage(), 0, currentUser.getProfileImage().length);
                profileImage.setImageBitmap(bitmap);
            }

            updateStatistics();
        }
    }

    private void updateStatistics() {
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getDatabase(requireContext());
                TransactionDao transactionDao = database.transactionDao();

                // Используем getAllTransactionsDirect() вместо getAllTransactions()
                List<Transaction> allTransactions = transactionDao.getAllTransactionsDirect();

                Log.d("Statistics", "Found transactions: " + allTransactions.size());

                // Подсчитываем общее количество операций
                int totalOperations = allTransactions.size();

                // Рассчитываем общий баланс
                double balance = 0.0;

                // Суммируем транзакции (доходы положительные, расходы отрицательные)
                for (Transaction transaction : allTransactions) {
                    if (transaction.isIncome()) {
                        balance += transaction.getAmount(); // Доходы добавляем
                    } else {
                        balance -= transaction.getAmount(); // Расходы вычитаем
                    }
                    Log.d("Statistics", "Transaction: " + transaction.getAmount() + ", isIncome: " + transaction.isIncome() + ", Category: " + transaction.getCategory());
                }

                Log.d("Statistics", "Total balance: " + balance + ", Total operations: " + totalOperations);

                // Создаем final копии для использования в лямбде
                final int finalTotalOperations = totalOperations;
                final String formattedBalance = formatCurrency(balance);
                final boolean isPositiveBalance = balance >= 0;

                // Обновляем UI в главном потоке
                requireActivity().runOnUiThread(() -> {
                    totalTransactionsText.setText(String.valueOf(finalTotalOperations));
                    totalBalanceText.setText(formattedBalance);

                    if (isPositiveBalance) {
                        totalBalanceText.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
                    } else {
                        totalBalanceText.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_red));
                    }

                    Log.d("Statistics", "UI updated - Operations: " + finalTotalOperations + ", Balance: " + formattedBalance);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Statistics", "Error: " + e.getMessage());
                requireActivity().runOnUiThread(() -> {
                    totalTransactionsText.setText("0");
                    totalBalanceText.setText("0 ₽");
                    totalBalanceText.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green));
                });
            }
        }).start();
    }

    private String formatCurrency(double amount) {
        try {
            DecimalFormat formatter = new DecimalFormat("#,##0.##");
            String formatted = formatter.format(Math.abs(amount));

            if (amount >= 0) {
                return formatted + " ₽";
            } else {
                return "-" + formatted + " ₽";
            }
        } catch (Exception e) {
            return String.valueOf(amount) + " ₽";
        }
    }

    private void setupClickListeners() {
        saveNameButton.setOnClickListener(v -> saveUserName());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                profileImage.setImageBitmap(bitmap);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
                selectedImageBytes = stream.toByteArray();

                saveUserPhoto();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveUserPhoto() {
        if (selectedImageBytes != null && currentUser != null) {
            new Thread(() -> {
                try {
                    currentUser.setProfileImage(selectedImageBytes);
                    userDao.update(currentUser);

                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Фото сохранено", Toast.LENGTH_SHORT).show();

                        if (profileImage != null) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                            if (bitmap != null) {
                                profileImage.setImageBitmap(bitmap);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Ошибка сохранения фото", Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        } else if (selectedImageBytes == null) {
            Toast.makeText(requireContext(), "Сначала выберите фото", Toast.LENGTH_SHORT).show();
        }
    }

    private void changePassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Ссылка для смены пароля отправлена на email", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "Ошибка отправки email", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void logout() {
        FirebaseUser currentFirebaseUser = mAuth.getCurrentUser();
        String currentUserId = currentFirebaseUser != null ? currentFirebaseUser.getUid() : null;

        new Thread(() -> {
            try {
                User currentUser = userDao.getUserByUid(currentUserId);
                if (currentUser != null) {
                    currentUser.setLoggedin(false);
                    userDao.update(currentUser);
                    Log.d("ProfileFragment", "User logged out successfully: " + currentUserId);
                }
            } catch (Exception e) {
                Log.e("ProfileFragment", "Error during logout: " + e.getMessage(), e);
            }
        }).start();

        mAuth.signOut();

        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}