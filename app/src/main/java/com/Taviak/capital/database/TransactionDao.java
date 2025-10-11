package com.Taviak.capital.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.Taviak.capital.models.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions")
    void deleteAllTransactions();

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE isIncome = 1 ORDER BY date DESC")
    LiveData<List<Transaction>> getAllIncomes();

    @Query("SELECT * FROM transactions WHERE isIncome = 0 ORDER BY date DESC")
    LiveData<List<Transaction>> getAllExpenses();

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    LiveData<List<Transaction>> getRecentTransactions(int limit);

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    LiveData<Double> getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    LiveData<Double> getTotalExpenses();

    @Query("SELECT SUM(CASE WHEN isIncome = 1 THEN amount ELSE -amount END) FROM transactions")
    LiveData<Double> getTotalBalance();
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<Transaction> getAllTransactionsDirect();
}