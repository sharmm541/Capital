package com.Taviak.capital.managers;

import com.Taviak.capital.models.Transaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransactionManager {
    private List<Transaction> transactions;
    private double monthlyBudget;
    public TransactionManager() {
        transactions = new ArrayList<>();
        monthlyBudget = 0; // Устанавливаем бюджет по умолчанию
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Используем правильный конструктор Transaction
        // Конструктор: amount, category, description, date, type, isIncome
        transactions.add(new Transaction(50000.0, "Зарплата", "Основная работа", new Date(), "Зарплата", true));
        transactions.add(new Transaction(15000.0, "Продукты", "Супермаркет", new Date(), "Питание", false));
        transactions.add(new Transaction(5000.0, "Транспорт", "Проездной", new Date(), "Транспорт", false));
        transactions.add(new Transaction(10000.0, "Фриланс", "Дополнительный доход", new Date(), "Фриланс", true));
        transactions.add(new Transaction(8000.0, "Развлечения", "Кино", new Date(), "Развлечения", false));
    }

    public List<Transaction> getRecentTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction> getIncomes() {
        List<Transaction> incomes = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.isIncome()) {
                incomes.add(transaction);
            }
        }
        return incomes;
    }

    public List<Transaction> getExpenses() {
        List<Transaction> expenses = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (!transaction.isIncome()) {
                expenses.add(transaction);
            }
        }
        return expenses;
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
    }

    public double getTotalBalance() {
        double balance = 0;
        for (Transaction transaction : transactions) {
            if (transaction.isIncome()) {
                balance += transaction.getAmount();
            } else {
                balance -= transaction.getAmount();
            }
        }
        return balance;
    }

    public double getTotalIncome() {
        double total = 0;
        for (Transaction transaction : transactions) {
            if (transaction.isIncome()) {
                total += transaction.getAmount();
            }
        }
        return total;
    }
    public double getBudget() {
        return monthlyBudget;
    }

    public void setBudget(double budget) {
        this.monthlyBudget = budget;
    }

    // Метод для получения оставшегося бюджета
    public double getRemainingBudget() {
        double totalExpenses = getTotalExpenses();
        return monthlyBudget - totalExpenses;
    }

    // Метод для получения процента использованного бюджета
    public double getBudgetUsagePercentage() {
        if (monthlyBudget == 0) return 0;
        double totalExpenses = getTotalExpenses();
        return (totalExpenses / monthlyBudget) * 100;
    }

    // Метод для проверки превышения бюджета
    public boolean isBudgetExceeded() {
        return getTotalExpenses() > monthlyBudget;
    }
    public double getTotalExpenses() {
        double total = 0;
        for (Transaction transaction : transactions) {
            if (!transaction.isIncome()) {
                total += transaction.getAmount();
            }
        }
        return total;
    }
}