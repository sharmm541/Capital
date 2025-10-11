package com.Taviak.capital.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.Taviak.capital.entities.User; // ИЗМЕНИТЬ ИМПОРТ

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // ВАЖНО: REPLACE вместо FAIL
    void insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    LiveData<User> getLoggedInUser();

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    User getLoggedInUserSync();

    @Query("UPDATE users SET isLoggedIn = 0")
    void logoutAllUsers();

    @Query("DELETE FROM users")
    void deleteAllUsers();

    @Query("SELECT name FROM users LIMIT 1")
    String getUserName();

    @Query("SELECT profileImage FROM users WHERE isLoggedIn = 1 LIMIT 1")
    byte[] getProfileImage();
    @Query("SELECT * FROM users WHERE userId = :uid LIMIT 1") // используем userId
    User getUserByUid(String uid);
}