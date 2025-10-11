package com.Taviak.capital.managers;

public class UserManager {

    private String userName = "Иван Иванов";
    private String userEmail = "user@example.com";
    private String currency = "RUB";
    private boolean notificationsEnabled = true;
    private boolean darkThemeEnabled = false;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    public boolean isDarkThemeEnabled() {
        return darkThemeEnabled;
    }

    public void setDarkThemeEnabled(boolean enabled) {
        this.darkThemeEnabled = enabled;
    }

    public void logout() {
        // Логика выхода из аккаунта
    }
}