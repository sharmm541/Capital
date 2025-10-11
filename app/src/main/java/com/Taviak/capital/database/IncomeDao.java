package com.Taviak.capital.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.Taviak.capital.entities.Income;
import java.util.List;

@Dao
public interface IncomeDao {

    @Insert
    void insert(Income income);

    @Update
    void update(Income income);

    @Delete
    void delete(Income income);

    @Query("DELETE FROM incomes")
    void deleteAllIncomes();

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    LiveData<List<Income>> getAllIncomes();

    // Используем Long вместо Date для параметров
    @Query("SELECT * FROM incomes WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Income>> getIncomesByDateRange(Long startDate, Long endDate);

    // Используем Long вместо Date для параметров
    @Query("SELECT SUM(amount) FROM incomes WHERE date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalIncomeForPeriod(Long startDate, Long endDate);

    @Query("SELECT category, SUM(amount) as total FROM incomes GROUP BY category")
    LiveData<List<CategorySum>> getIncomeByCategories();

    static class CategorySum {
        public String category;
        public double total;
    }
}