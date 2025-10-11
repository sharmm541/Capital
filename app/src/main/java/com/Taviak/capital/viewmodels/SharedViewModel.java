package com.Taviak.capital.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.Taviak.capital.models.Transaction;
import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Double> totalBalance = new MutableLiveData<>(0.0);
    private final MutableLiveData<List<Transaction>> allTransactions = new MutableLiveData<>(new ArrayList<>());

    public LiveData<Double> getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double balance) {
        totalBalance.setValue(balance);
    }

    public void updateBalance(double amount, boolean isIncome) {
        Double current = totalBalance.getValue();
        if (current != null) {
            double newBalance = isIncome ? current + amount : current - amount;
            totalBalance.setValue(newBalance);
        } else {
            totalBalance.setValue(isIncome ? amount : -amount);
        }
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }

    public void setAllTransactions(List<Transaction> transactions) {
        allTransactions.setValue(transactions != null ? transactions : new ArrayList<>());
    }

    public void addTransaction(Transaction transaction) {
        List<Transaction> currentTransactions = allTransactions.getValue();
        if (currentTransactions != null) {
            List<Transaction> newTransactions = new ArrayList<>(currentTransactions);
            newTransactions.add(transaction);
            allTransactions.setValue(newTransactions);

            // Обновляем баланс
            updateBalance(transaction.getAmount(), transaction.isIncome());
        }
    }

    public void removeTransaction(Transaction transaction) {
        List<Transaction> currentTransactions = allTransactions.getValue();
        if (currentTransactions != null) {
            List<Transaction> newTransactions = new ArrayList<>(currentTransactions);
            if (newTransactions.remove(transaction)) {
                allTransactions.setValue(newTransactions);
                // Отменяем изменение баланса
                updateBalance(-transaction.getAmount(), transaction.isIncome());
            }
        }
    }

    // Вспомогательные методы для получения только доходов или расходов
    public List<Transaction> getIncomes() {
        List<Transaction> all = allTransactions.getValue();
        if (all == null) return new ArrayList<>();

        List<Transaction> incomes = new ArrayList<>();
        for (Transaction transaction : all) {
            if (transaction.isIncome()) {
                incomes.add(transaction);
            }
        }
        return incomes;
    }

    public List<Transaction> getExpenses() {
        List<Transaction> all = allTransactions.getValue();
        if (all == null) return new ArrayList<>();

        List<Transaction> expenses = new ArrayList<>();
        for (Transaction transaction : all) {
            if (!transaction.isIncome()) {
                expenses.add(transaction);
            }
        }
        return expenses;
    }
}