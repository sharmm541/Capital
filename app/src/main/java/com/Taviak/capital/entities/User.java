package com.Taviak.capital.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "userId") // ОСТАВЛЯЕМ в верхнем регистре
    public String userid;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "createdAt")
    public long createdAt;

    @ColumnInfo(name = "isLoggedIn")
    public boolean isLoggedin;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private byte[] profileImage;

    // Конструктор по умолчанию
    public User() {
    }

    @Ignore
    public User(String userid, String email, String name, long createdAt, boolean isLoggedin) {
        this.userid = userid;
        this.email = email;
        this.name = name;
        this.createdAt = createdAt;
        this.isLoggedin = isLoggedin;
    }

    // Getters and Setters
    public String getUserid() {
        return userid;
    }

    public void setUserid(@NonNull String userid) {
        this.userid = userid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isLoggedin() {
        return isLoggedin;
    }

    public void setLoggedin(boolean loggedin) {
        isLoggedin = loggedin;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }

    @Override
    public String toString() {
        return "User{" +
                "userid='" + userid + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", isLoggedin=" + isLoggedin +
                '}';
    }
}