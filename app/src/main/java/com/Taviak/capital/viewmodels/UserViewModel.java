package com.Taviak.capital.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.Taviak.capital.entities.User;
import com.Taviak.capital.repository.UserRepository;

public class UserViewModel extends AndroidViewModel {

    private UserRepository repository;
    private LiveData<User> loggedInUser;

    public UserViewModel(Application application) {
        super(application);
        repository = new UserRepository(application);
        loggedInUser = repository.getLoggedInUser();
    }

    public void insert(User user) {
        repository.insert(user);
    }

    public void update(User user) {
        repository.update(user);
    }

    public LiveData<User> getLoggedInUser() {
        return loggedInUser;
    }

    public User getLoggedInUserSync() {
        return repository.getLoggedInUserSync();
    }

    public void logoutAllUsers() {
        repository.logoutAllUsers();
    }

    public void deleteAllUsers() {
        repository.deleteAllUsers();
    }

    public void registerUser(String userId, String email, String name) {
        repository.registerUser(userId, email, name);
    }
}