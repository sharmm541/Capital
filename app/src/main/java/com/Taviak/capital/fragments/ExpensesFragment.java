package com.Taviak.capital.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Taviak.capital.R;
import com.Taviak.capital.adapters.TransactionsAdapter;
import com.Taviak.capital.managers.TransactionManager;
import com.Taviak.capital.models.Transaction;
import com.Taviak.capital.viewmodels.TransactionViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ExpensesFragment extends Fragment {

    private EditText amountInput, descriptionInput;
    private Spinner typeInput, categoryInput;
    private Button addButton, cancelButton;
    private TextView totalExpenses, budgetAmount, remainingBudget, budgetProgressText;
    private ProgressBar budgetProgressBar;
    private RecyclerView expensesRecyclerView;
    private TransactionsAdapter adapter;
    private TransactionManager transactionManager;
    private TransactionViewModel transactionViewModel;

    private List<String> expenseTypes;
    private List<String> expenseCategories;

    public ExpensesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        expenseTypes = Arrays.asList("Выберите тип", "Продукты", "Транспорт", "Развлечения", "Жилье", "Здоровье", "Одежда", "Прочее");
        expenseCategories = Arrays.asList("Выберите категорию", "Обязательные", "Необязательные", "Экстренные","Прочее");

        transactionManager = new TransactionManager();
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupListeners(view); // Передаем view в метод
        updateUI();

        return view;
    }

    private void initViews(View view) {
        // Поля ввода
        amountInput = view.findViewById(R.id.amountInput);
        typeInput = view.findViewById(R.id.typeInput);
        categoryInput = view.findViewById(R.id.categoryInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);

        // Кнопки
        addButton = view.findViewById(R.id.addExpenseButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        // RecyclerView
        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView);

        totalExpenses = view.findViewById(R.id.totalExpenses);
        budgetAmount = view.findViewById(R.id.budgetAmount);

        setupSpinners();
    }

    private void setupSpinners() {
        // Настройка Spinner для типа расхода
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, expenseTypes) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }

                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }
        };
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeInput.setAdapter(typeAdapter);

        // Настройка Spinner для категории
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, expenseCategories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }

                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }
        };
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryInput.setAdapter(categoryAdapter);
    }

    private void setupViewModel() {
        // Наблюдаем за расходами из базы данных
        transactionViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                adapter.updateTransactions(expenses);
                // ДОБАВЬТЕ ЭТИ СТРОКИ - обновляем статистику при изменении данных
                updateTotalExpenses(calculateTotalExpenses(expenses));
                updateMonthlyExpenses(expenses);
            }
        });

        // Наблюдаем за общими расходами
        transactionViewModel.getTotalExpenses().observe(getViewLifecycleOwner(), total -> {
            if (total != null && totalExpenses != null) {
                totalExpenses.setText(String.format("%.0f ₽", total));

            }
        });
    }

    private void setupRecyclerView() {
        adapter = new TransactionsAdapter(new ArrayList<>());
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expensesRecyclerView.setAdapter(adapter);
    }

    private void setupListeners(View view) {
        addButton.setOnClickListener(v -> addExpense());
        cancelButton.setOnClickListener(v -> undoLastExpense());

        // Кнопка изменения бюджета - ищем в переданном view
    }

    private void updateUI() {
        // Обновляем UI с данными из TransactionManager
        double budget = transactionManager.getBudget();
        if (budgetAmount != null) {
            budgetAmount.setText(String.format("%.0f ₽", budget));
        }
    }
    private double calculateTotalExpenses(List<Transaction> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }

        double total = 0;
        for (Transaction expense : expenses) {
            total += expense.getAmount();
        }
        return total;
    }

    // Расчет расходов за месяц
    private double calculateMonthlyExpenses(List<Transaction> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }

        Calendar currentMonth = Calendar.getInstance();
        double monthlyTotal = 0;

        for (Transaction expense : expenses) {
            if (isDateInCurrentMonth(expense.getDate(), currentMonth)) {
                monthlyTotal += expense.getAmount();
            }
        }

        return monthlyTotal;
    }

    // Проверка даты (общий метод)
    private boolean isDateInCurrentMonth(Date date, Calendar currentMonth) {
        if (date == null) return false;

        Calendar expenseDate = Calendar.getInstance();
        expenseDate.setTime(date);

        return expenseDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                expenseDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH);
    }

    // Обновление UI для общих расходов
    private void updateTotalExpenses(Double total) {
        if (totalExpenses != null) {
            totalExpenses.setText(formatCurrency(total));
        }
    }

    // Обновление UI для расходов за месяц
    private void updateMonthlyExpenses(List<Transaction> expenses) {
        if (budgetAmount != null) {
            double monthlyTotal = calculateMonthlyExpenses(expenses);
            budgetAmount.setText(formatCurrency(monthlyTotal));
        }
    }

    // Метод форматирования валюты
    private String formatCurrency(double amount) {
        return String.format("%.0f ₽", amount);
    }

    private boolean validateInput() {
        boolean isValid = true;

        String amountStr = amountInput.getText().toString().trim();
        String type = typeInput.getSelectedItem() != null ? typeInput.getSelectedItem().toString().trim() : "";
        String category = categoryInput.getSelectedItem() != null ? categoryInput.getSelectedItem().toString().trim() : "";

        // Проверка суммы
        if (amountStr.isEmpty()) {
            amountInput.setError("Введите сумму");
            isValid = false;
        } else {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    amountInput.setError("Сумма должна быть больше 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                amountInput.setError("Некорректная сумма");
                isValid = false;
            }
        }

        // Проверка типа расхода
        if (type.isEmpty() || "Выберите тип".equals(type)) {
            showMessage("Ошибка", "Выберите тип расхода");
            isValid = false;
        }

        // Проверка категории
        if (category.isEmpty() || "Выберите категорию".equals(category)) {
            showMessage("Ошибка", "Выберите категорию");
            isValid = false;
        }

        return isValid;
    }

    private void addExpense() {
        if (!validateInput()) {
            return;
        }

        try {
            String amountStr = amountInput.getText().toString().trim();
            String type = typeInput.getSelectedItem().toString().trim();
            String category = categoryInput.getSelectedItem().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            double amount = Double.parseDouble(amountStr);
            Date currentDate = new Date();

            // Создаем Transaction с isIncome = false
            Transaction expense = new Transaction(amount, category, description, currentDate, type, false);
            transactionViewModel.insert(expense);

            clearInputFields();
            showMessage("Успех", "Расход добавлен");

        } catch (NumberFormatException e) {
            amountInput.setError("Некорректная сумма");
        }
    }

    private void undoLastExpense() {
        // Получаем текущие расходы из ViewModel
        List<Transaction> currentExpenses = transactionViewModel.getAllExpenses().getValue();

        if (currentExpenses != null && !currentExpenses.isEmpty()) {
            // Находим самый последний расход
            Transaction lastExpense = findLatestExpense(currentExpenses);
            if (lastExpense != null) {
                // Удаляем только конкретный расход по его ID
                transactionViewModel.delete(lastExpense);
                showMessage("Расход удален", String.format("Удален расход: %s - %.0f ₽",
                        lastExpense.getType(), lastExpense.getAmount()));
            } else {
                showMessage("Ошибка", "Не удалось найти последний расход");
            }
        } else {
            showMessage("Нет записей", "Нет расходов для удаления");
        }
    }

    private Transaction findLatestExpense(List<Transaction> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return null;
        }
        Transaction latestExpense = expenses.get(0);
        for (Transaction expense : expenses) {
            if (expense.getDate().after(latestExpense.getDate())) {
                latestExpense = expense;
            }
        }
        return latestExpense;
    }

    private void clearInputFields() {
        if (amountInput != null) amountInput.setText("");
        if (descriptionInput != null) descriptionInput.setText("");

        if (typeInput != null) typeInput.setSelection(0);
        if (categoryInput != null) categoryInput.setSelection(0);

        if (amountInput != null) amountInput.setError(null);
    }
    private void showMessage(String title, String message) {
        try {
            if (getContext() != null) {
                new AlertDialog.Builder(getContext())
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}