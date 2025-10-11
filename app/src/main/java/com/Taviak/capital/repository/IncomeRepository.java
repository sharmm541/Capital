package com.Taviak.capital.repository;

import androidx.lifecycle.LiveData;
import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.IncomeDao;
import com.Taviak.capital.entities.Income;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IncomeRepository {
    private IncomeDao incomeDao;
    private ExecutorService executorService;

    public IncomeRepository(AppDatabase database) {
        this.incomeDao = database.incomeDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertIncome(Income income) {
        executorService.execute(() -> incomeDao.insert(income));
    }

    public void updateIncome(Income income) {
        executorService.execute(() -> incomeDao.update(income));
    }

    public void deleteIncome(Income income) {
        executorService.execute(() -> incomeDao.delete(income));
    }

    public LiveData<List<Income>> getAllIncomes() {
        return incomeDao.getAllIncomes();
    }

    // Конвертируем Date в Long для Room
    public LiveData<List<Income>> getIncomesByDateRange(Date startDate, Date endDate) {
        return incomeDao.getIncomesByDateRange(
                startDate != null ? startDate.getTime() : 0,
                endDate != null ? endDate.getTime() : System.currentTimeMillis()
        );
    }

    // Конвертируем Date в Long для Room
    public LiveData<Double> getTotalIncomeForPeriod(Date startDate, Date endDate) {
        return incomeDao.getTotalIncomeForPeriod(
                startDate != null ? startDate.getTime() : 0,
                endDate != null ? endDate.getTime() : System.currentTimeMillis()
        );
    }

    public LiveData<List<IncomeDao.CategorySum>> getIncomeByCategories() {
        return incomeDao.getIncomeByCategories();
    }
}