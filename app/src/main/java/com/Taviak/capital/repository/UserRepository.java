package com.Taviak.capital.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.UserDao;
import com.Taviak.capital.entities.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    private UserDao userDao;
    private ExecutorService executorService;

    public UserRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        userDao = database.userDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(User user) {
        executorService.execute(() -> userDao.insert(user));
    }

    public void update(User user) {
        executorService.execute(() -> userDao.update(user));
    }

    public LiveData<User> getLoggedInUser() {
        return userDao.getLoggedInUser();
    }

    public User getLoggedInUserSync() {
        return userDao.getLoggedInUserSync();
    }

    public void logoutAllUsers() {
        executorService.execute(() -> userDao.logoutAllUsers());
    }

    public void deleteAllUsers() {
        executorService.execute(() -> userDao.deleteAllUsers());
    }

    // ИСПРАВЛЕННЫЙ МЕТОД - проверяем существование пользователя
    public void registerUser(String userId, String email, String name) {
        executorService.execute(() -> {
            try {
                // Сначала проверяем, существует ли пользователь
                User existingUser = userDao.getUserByUid(userId);

                if (existingUser != null) {
                    // Пользователь уже существует - обновляем его данные
                    existingUser.setEmail(email);
                    existingUser.setName(name);
                    existingUser.setLoggedin(true);
                    userDao.update(existingUser);
                    System.out.println("User updated: " + userId);
                } else {
                    // Пользователя нет - создаем нового
                    userDao.logoutAllUsers(); // Выходим из всех аккаунтов

                    User user = new User();
                    user.setUserid(userId);
                    user.setEmail(email);
                    user.setName(name);
                    user.setCreatedAt(System.currentTimeMillis());
                    user.setLoggedin(true);

                    userDao.insert(user);
                    System.out.println("New user created: " + userId);
                }
            } catch (Exception e) {
                System.err.println("Error in registerUser: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ДОБАВЬТЕ ЭТОТ МЕТОД для получения пользователя по ID
    public User getUserByUidSync(String uid) {
        return userDao.getUserByUid(uid);
    }

    // ДОБАВЬТЕ ЭТОТ МЕТОД для асинхронного получения
    public void getUserByUid(String uid, UserCallback callback) {
        executorService.execute(() -> {
            try {
                User user = userDao.getUserByUid(uid);
                if (callback != null) {
                    callback.onSuccess(user);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(Exception e);
    }
}