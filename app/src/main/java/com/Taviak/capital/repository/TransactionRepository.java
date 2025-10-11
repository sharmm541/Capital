package com.Taviak.capital.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.TransactionDao;
import com.Taviak.capital.models.Transaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private TransactionDao transactionDao;
    private ExecutorService executorService;

    public TransactionRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        transactionDao = database.transactionDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Transaction transaction) {
        executorService.execute(() -> transactionDao.insert(transaction));
    }

    public void update(Transaction transaction) {
        executorService.execute(() -> transactionDao.update(transaction));
    }

    public void delete(Transaction transaction) {
        executorService.execute(() -> transactionDao.delete(transaction));
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<List<Transaction>> getAllIncomes() {
        return transactionDao.getAllIncomes();
    }

    public LiveData<List<Transaction>> getAllExpenses() {
        return transactionDao.getAllExpenses();
    }

    public LiveData<List<Transaction>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(limit);
    }

    public LiveData<Double> getTotalIncome() {
        return transactionDao.getTotalIncome();
    }

    public LiveData<Double> getTotalExpenses() {
        return transactionDao.getTotalExpenses();
    }

    public LiveData<Double> getTotalBalance() {
        return transactionDao.getTotalBalance();
    }
}