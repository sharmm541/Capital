package com.Taviak.capital.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.Taviak.capital.models.Transaction;
import com.Taviak.capital.repository.TransactionRepository;

import java.util.List;

public class TransactionViewModel extends AndroidViewModel {

    private TransactionRepository repository;
    private LiveData<List<Transaction>> allTransactions;
    private LiveData<List<Transaction>> allIncomes;
    private LiveData<List<Transaction>> allExpenses;
    private LiveData<Double> totalBalance;
    private LiveData<Double> totalIncome;
    private LiveData<Double> totalExpenses;

    public TransactionViewModel(Application application) {
        super(application);
        repository = new TransactionRepository(application);
        allTransactions = repository.getAllTransactions();
        allIncomes = repository.getAllIncomes();
        allExpenses = repository.getAllExpenses();
        totalBalance = repository.getTotalBalance();
        totalIncome = repository.getTotalIncome();
        totalExpenses = repository.getTotalExpenses();
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void delete(Transaction transaction) {
        repository.delete(transaction);
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public LiveData<List<Transaction>> getAllIncomes() {
        return allIncomes;
    }

    public LiveData<List<Transaction>> getAllExpenses() {
        return allExpenses;
    }

    public LiveData<Double> getTotalBalance() {
        return totalBalance;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getTotalExpenses() {
        return totalExpenses;
    }
}