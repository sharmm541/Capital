package com.Taviak.capital.fragments;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Taviak.capital.R;
import com.Taviak.capital.adapters.IncomeAdapter;
import com.Taviak.capital.models.Transaction;
import com.Taviak.capital.viewmodels.TransactionViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class IncomeFragment extends Fragment {

    // UI элементы
    private EditText amountInput, descriptionInput;
    private Spinner typeInput, categoryInput;
    private Button addButton, cancelButton;
    private RecyclerView incomesRecyclerView;

    // TextView для статистики
    private TextView totalBalance, totalIncome, monthlyIncome, balanceAmount, balanceChange;

    // Данные
    private List<Transaction> currentTransactions;
    private List<String> incomeTypes;
    private List<String> incomeCategories;

    // ViewModel
    private TransactionViewModel transactionViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        incomeTypes = Arrays.asList("Выберите тип", "Зарплата", "Фриланс", "Инвестиции", "Подарок", "Прочее");
        incomeCategories = Arrays.asList("Выберите категорию", "Основной доход", "Дополнительный доход", "Пассивный доход", "Прочее");

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        initViews(view);
        setupObservers();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        // Основные поля ввода
        amountInput = view.findViewById(R.id.amountInput);
        typeInput = view.findViewById(R.id.typeInput);
        categoryInput = view.findViewById(R.id.categoryInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);

        // Кнопки
        addButton = view.findViewById(R.id.addIncomeButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        // RecyclerView
        incomesRecyclerView = view.findViewById(R.id.incomesRecyclerView);
        if (incomesRecyclerView != null) {
            incomesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        totalIncome = view.findViewById(R.id.totalIncome);
        monthlyIncome = view.findViewById(R.id.monthlyIncome);

        setupSpinners();
        updateCancelButtonState();
    }

    private void setupSpinners() {
        // Настройка Spinner для типа дохода
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, incomeTypes) {
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
                android.R.layout.simple_spinner_item, incomeCategories) {
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

    private void setupObservers() {
        // Наблюдаем за общим балансом из базы данных
        transactionViewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                importTotalBalance(balance);
            }
        });

        // Наблюдаем за общими доходами
        transactionViewModel.getTotalIncome().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                updateTotalIncome(total);
            }
        });

        // Наблюдаем за доходами для расчета месячных и отображения списка
        transactionViewModel.getAllIncomes().observe(getViewLifecycleOwner(), incomes -> {
            if (incomes != null) {
                currentTransactions = incomes;
                updateCancelButtonState();
                updateMonthlyIncome(incomes);
                updateRecyclerView(incomes);
            }
        });
    }

    private void setupListeners() {
        addButton.setOnClickListener(v -> addIncome());
        cancelButton.setOnClickListener(v -> undoLastIncome());
    }

    private void importTotalBalance(Double balance) {
        if (totalBalance != null) {
            String formattedBalance = formatCurrency(balance);
            totalBalance.setText(formattedBalance);
        }

        if (balanceAmount != null) {
            String formattedBalance = formatCurrency(balance);
            balanceAmount.setText(formattedBalance);
        }
    }

    private void updateTotalIncome(Double total) {
        if (totalIncome != null) {
            totalIncome.setText(formatCurrency(total));
        }
    }

    private void updateMonthlyIncome(List<Transaction> incomes) {
        if (monthlyIncome != null) {
            double monthlyTotal = calculateMonthlyIncome(incomes);
            monthlyIncome.setText(formatCurrency(monthlyTotal));
        }
    }

    private void updateRecyclerView(List<Transaction> incomes) {
        if (incomesRecyclerView != null) {
            if (!incomes.isEmpty()) {
                List<Transaction> sortedIncomes = new ArrayList<>(incomes);
                Collections.sort(sortedIncomes, (i1, i2) -> i2.getDate().compareTo(i1.getDate()));

                IncomeAdapter adapter = new IncomeAdapter(sortedIncomes);
                incomesRecyclerView.setAdapter(adapter);
            } else {
                incomesRecyclerView.setAdapter(null);
            }
        }
    }

    private double calculateMonthlyIncome(List<Transaction> incomes) {
        if (incomes == null || incomes.isEmpty()) {
            return 0.0;
        }

        Calendar currentMonth = Calendar.getInstance();
        double monthlyTotal = 0;

        for (Transaction income : incomes) {
            if (isDateInCurrentMonth(income.getDate(), currentMonth)) {
                monthlyTotal += income.getAmount();
            }
        }

        return monthlyTotal;
    }

    private boolean isDateInCurrentMonth(Date date, Calendar currentMonth) {
        if (date == null) return false;

        Calendar incomeDate = Calendar.getInstance();
        incomeDate.setTime(date);

        return incomeDate.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                incomeDate.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH);
    }

    private String formatCurrency(double amount) {
        return String.format("%,d ₽", (int) amount).replace(',', ' ');
    }

    private void updateCancelButtonState() {
        if (cancelButton != null) {
            boolean hasIncomes = currentTransactions != null && !currentTransactions.isEmpty();
            cancelButton.setEnabled(hasIncomes);
            cancelButton.setAlpha(hasIncomes ? 1.0f : 0.5f);
        }
    }

    private void addIncome() {
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

            Transaction income = new Transaction(amount, category, description, currentDate, type, true);
            transactionViewModel.insert(income);

            clearInputFields();
            showMessage("Успех", "Доход добавлен");

        } catch (NumberFormatException e) {
            amountInput.setError("Некорректная сумма");
        }
    }

    private void undoLastIncome() {
        if (currentTransactions != null && !currentTransactions.isEmpty()) {
            Transaction lastIncome = findLatestTransaction();
            if (lastIncome != null) {
                transactionViewModel.delete(lastIncome);
                showMessage("Доход удален", String.format("Удален доход: %s - %.0f ₽",
                        lastIncome.getType(), lastIncome.getAmount()));
            }
        }
    }

    private Transaction findLatestTransaction() {
        if (currentTransactions == null || currentTransactions.isEmpty()) {
            return null;
        }
        Transaction latestTransaction = currentTransactions.get(0);
        for (Transaction transaction : currentTransactions) {
            if (transaction.getDate().after(latestTransaction.getDate())) {
                latestTransaction = transaction;
            }
        }
        return latestTransaction;
    }

    private void clearInputFields() {
        if (amountInput != null) amountInput.setText("");
        if (descriptionInput != null) descriptionInput.setText("");

        if (typeInput != null) typeInput.setSelection(0);
        if (categoryInput != null) categoryInput.setSelection(0);

        if (amountInput != null) amountInput.setError(null);
    }

    private boolean validateInput() {
        // Ваш существующий код валидации
        return true;
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