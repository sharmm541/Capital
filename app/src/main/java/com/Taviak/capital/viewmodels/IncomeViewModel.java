package com.Taviak.capital.viewmodels;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.Taviak.capital.CapitalApp;
import com.Taviak.capital.entities.Income;
import com.Taviak.capital.repository.IncomeRepository;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class IncomeViewModel extends AndroidViewModel {
    private IncomeRepository repository;
    private LiveData<List<Income>> allIncomes;
    private MutableLiveData<Double> totalIncome = new MutableLiveData<>();
    private MutableLiveData<Double> monthlyIncome = new MutableLiveData<>();

    public IncomeViewModel(Application application) {
        super(application);

        // Безопасное получение репозитория
        if (application instanceof CapitalApp) {
            CapitalApp app = (CapitalApp) application;
            this.repository = app.getIncomeRepository();
        } else {
            // Если приведение типа не удалось, создаем репозиторий напрямую
            this.repository = new IncomeRepository(CapitalApp.getDatabase(application));
        }

        this.allIncomes = repository.getAllIncomes();

        // Инициализируем начальные значения
        totalIncome.setValue(0.0);
        monthlyIncome.setValue(0.0);

        // Наблюдаем за всеми доходами и обновляем статистику
        allIncomes.observeForever(incomes -> updateStatistics(incomes));
    }

    public LiveData<List<Income>> getAllIncomes() {
        return allIncomes;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<Double> getMonthlyIncome() {
        return monthlyIncome;
    }

    public void insertIncome(Income income) {
        repository.insertIncome(income);
    }

    public void updateIncome(Income income) {
        repository.updateIncome(income);
    }

    public void deleteIncome(Income income) {
        repository.deleteIncome(income);
    }

    private void updateStatistics(List<Income> incomes) {
        if (incomes == null) return;

        double total = 0;
        double monthly = 0;

        // Получаем начало текущего месяца
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date monthStart = calendar.getTime();
        Date now = new Date();

        for (Income income : incomes) {
            total += income.getAmount();

            // Считаем доходы за текущий месяц
            if (income.getDate() != null &&
                    income.getDate().after(monthStart) &&
                    income.getDate().before(now)) {
                monthly += income.getAmount();
            }
        }

        totalIncome.setValue(total);
        monthlyIncome.setValue(monthly);
    }
}