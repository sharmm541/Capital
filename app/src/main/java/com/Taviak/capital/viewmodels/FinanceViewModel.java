package com.Taviak.capital.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.TransactionDao;
import com.Taviak.capital.models.Transaction;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinanceViewModel extends AndroidViewModel {

    private TransactionDao transactionDao;
    private ExecutorService executorService;
    private MutableLiveData<ChartData> chartData = new MutableLiveData<>();

    public FinanceViewModel(@NonNull Application application) {
        super(application);
        transactionDao = AppDatabase.getDatabase(application).transactionDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static class ChartData {
        public float income;
        public float expenses;
        public List<Category> categories;

        public ChartData(float income, float expenses, List<Category> categories) {
            this.income = income;
            this.expenses = expenses;
            this.categories = categories;
        }
    }

    public static class Category {
        public String name;
        public float amount;

        public Category(String name, float amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    public LiveData<ChartData> getChartData(String period) {
        loadChartData(period);
        return chartData;
    }

    private void loadChartData(String period) {
        executorService.execute(() -> {
            try {
                List<Transaction> transactions = transactionDao.getAllTransactionsDirect();
                if (transactions == null || transactions.isEmpty()) {
                    chartData.postValue(new ChartData(0, 0, new ArrayList<>()));
                    return;
                }

                float income = 0;
                float expenses = 0;
                List<Category> categories = new ArrayList<>();

                Calendar cal = Calendar.getInstance();
                Calendar now = Calendar.getInstance();

                for (Transaction transaction : transactions) {
                    cal.setTime(transaction.getDate());

                    if (isInPeriod(transaction.getDate(), period, now)) {
                        if (transaction.isIncome()) {
                            income += transaction.getAmount();
                        } else {
                            expenses += transaction.getAmount();

                            // Добавляем в категории
                            String categoryName = transaction.getCategory();
                            if (categoryName == null || categoryName.isEmpty()) {
                                categoryName = "Без категории";
                            }

                            boolean found = false;
                            for (Category cat : categories) {
                                if (cat.name.equals(categoryName)) {
                                    cat.amount += transaction.getAmount();
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                categories.add(new Category(categoryName, (float) transaction.getAmount()));
                            }
                        }
                    }
                }

                // Сортируем категории по убыванию
                categories.sort((c1, c2) -> Float.compare(c2.amount, c1.amount));

                chartData.postValue(new ChartData(income, expenses, categories));

            } catch (Exception e) {
                chartData.postValue(new ChartData(0, 0, new ArrayList<>()));
            }
        });
    }

    private boolean isInPeriod(java.util.Date date, String period, Calendar now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        switch (period) {
            case "today":
                return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
            case "week":
                return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR);
            case "month":
                return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH);
            case "year":
                return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR);
            default:
                return false;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}