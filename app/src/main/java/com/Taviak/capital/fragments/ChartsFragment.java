package com.Taviak.capital.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.Taviak.capital.R;
import com.Taviak.capital.viewmodels.FinanceViewModel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChartsFragment extends Fragment {

    private FinanceViewModel financeViewModel;
    private Spinner periodSpinner;
    private ProgressBar incomeBar, expenseBar;
    private TextView incomeValueText, expenseValueText;
    private LinearLayout categoriesContainer;
    private TextView totalIncomeText, totalExpenseText, balanceText, financialTipsText;
    private String selectedPeriod = "month";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_charts, container, false);

        initViews(view);
        setupPeriodSpinner();
        loadData();

        return view;
    }

    private void initViews(View view) {
        periodSpinner = view.findViewById(R.id.periodSpinner);
        incomeValueText = view.findViewById(R.id.incomeValueText);
        expenseValueText = view.findViewById(R.id.expenseValueText);
        categoriesContainer = view.findViewById(R.id.categoriesContainer);
        totalIncomeText = view.findViewById(R.id.totalIncomeText);
        totalExpenseText = view.findViewById(R.id.totalExpenseText);
        balanceText = view.findViewById(R.id.balanceText);
        financialTipsText = view.findViewById(R.id.financialTipsText);
    }

    private void setupPeriodSpinner() {
        String[] periods = {"За сегодня", "За неделю", "За месяц", "За год"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);
        periodSpinner.setSelection(2);

        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPeriod = position == 0 ? "today" :
                        position == 1 ? "week" :
                                position == 2 ? "month" : "year";
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadData() {
        if (financeViewModel == null) {
            financeViewModel = new ViewModelProvider(requireActivity()).get(FinanceViewModel.class);
        }

        financeViewModel.getChartData(selectedPeriod).observe(getViewLifecycleOwner(), chartData -> {
            if (chartData != null) {
                updateUI(chartData);
            }
        });
    }

    private void updateUI(FinanceViewModel.ChartData data) {
        // Обновляем статистику
        DecimalFormat formatter = new DecimalFormat("#,###₽");
        totalIncomeText.setText(formatter.format(data.income));
        totalExpenseText.setText(formatter.format(data.expenses));

        float balance = data.income - data.expenses;
        balanceText.setText(formatter.format(balance));
        balanceText.setTextColor(balance >= 0 ?
                getResources().getColor(R.color.accent_green) :
                getResources().getColor(R.color.status_error));

        // Обновляем графики
        updateBarChart(data.income, data.expenses);
        updateCategoryChart(data.categories);
        updateTips(data.income, data.expenses, data.categories);
    }

    private void updateBarChart(float income, float expenses) {
        View incomeBar = getView().findViewById(R.id.incomeBar);
        View expenseBar = getView().findViewById(R.id.expenseBar);

        if (income == 0 && expenses == 0) {
            ViewGroup.LayoutParams incomeParams = incomeBar.getLayoutParams();
            ViewGroup.LayoutParams expenseParams = expenseBar.getLayoutParams();
            incomeParams.height = 4;
            expenseParams.height = 4;
            incomeBar.setLayoutParams(incomeParams);
            expenseBar.setLayoutParams(expenseParams);

            incomeValueText.setText("0₽");
            expenseValueText.setText("0₽");
            return;
        }

        // Фиксированная максимальная высота (60% от доступного пространства)
        int maxHeightPx = 180;

        float max = Math.max(income, expenses);
        if (max == 0) max = 1;

        // Рассчитываем высоту столбцов
        int incomeHeight = Math.max((int)((income / max) * maxHeightPx), 8);
        int expenseHeight = Math.max((int)((expenses / max) * maxHeightPx), 8);

        // Ограничиваем максимальную высоту
        incomeHeight = Math.min(incomeHeight, maxHeightPx);
        expenseHeight = Math.min(expenseHeight, maxHeightPx);

        // Устанавливаем высоту столбцов
        ViewGroup.LayoutParams incomeParams = incomeBar.getLayoutParams();
        ViewGroup.LayoutParams expenseParams = expenseBar.getLayoutParams();
        incomeParams.height = incomeHeight;
        expenseParams.height = expenseHeight;

        incomeBar.setLayoutParams(incomeParams);
        expenseBar.setLayoutParams(expenseParams);

        incomeValueText.setText(formatMoney(income));
        expenseValueText.setText(formatMoney(expenses));
    }

    private void updateCategoryChart(List<FinanceViewModel.Category> categories) {
        categoriesContainer.removeAllViews();

        if (categories.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("Нет данных по категориям");
            emptyText.setTextColor(getResources().getColor(R.color.text_main_secondary));
            emptyText.setTextSize(14f);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 80, 0, 80);
            categoriesContainer.addView(emptyText);
            return;
        }

        float total = 0;
        for (FinanceViewModel.Category cat : categories) total += cat.amount;

        String[] colors = {"#2196F3", "#FF9800", "#9C27B0", "#009688", "#FFEB3B", "#795548", "#607D8B", "#E91E63"};

        for (int i = 0; i < categories.size() && i < 8; i++) {
            FinanceViewModel.Category cat = categories.get(i);
            float percent = total > 0 ? (cat.amount / total) * 100 : 0;

            // Создаем layout для категории
            LinearLayout categoryLayout = new LinearLayout(requireContext());
            categoryLayout.setOrientation(LinearLayout.HORIZONTAL);
            categoryLayout.setPadding(16, 12, 16, 12);
            categoryLayout.setBackgroundResource(R.drawable.category_item_background);
            categoryLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            if (i > 0) {
                ((LinearLayout.LayoutParams) categoryLayout.getLayoutParams()).topMargin = 8;
            }

            // Цветной индикатор
            View colorView = new View(requireContext());
            colorView.setBackgroundColor(Color.parseColor(colors[i % colors.length]));
            LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(16, 16);
            colorParams.rightMargin = 12;
            colorParams.gravity = android.view.Gravity.CENTER_VERTICAL;
            colorView.setLayoutParams(colorParams);
            colorView.setBackground(createCircleDrawable(Color.parseColor(colors[i % colors.length])));

            // Название категории
            TextView nameText = new TextView(requireContext());
            nameText.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));
            nameText.setText(cat.name);
            nameText.setTextColor(getResources().getColor(R.color.text_main_primary));
            nameText.setTextSize(14f);
            nameText.setTypeface(getResources().getFont(R.font.inter_regular));

            // Сумма и процент
            TextView amountText = new TextView(requireContext());
            amountText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            amountText.setText(formatMoney(cat.amount) + " (" +
                    String.format(Locale.getDefault(), "%.1f%%", percent) + ")");
            amountText.setTextColor(getResources().getColor(R.color.text_main_secondary));
            amountText.setTextSize(12f);
            amountText.setTypeface(getResources().getFont(R.font.inter_regular));

            categoryLayout.addView(colorView);
            categoryLayout.addView(nameText);
            categoryLayout.addView(amountText);

            categoriesContainer.addView(categoryLayout);
        }
    }

    private android.graphics.drawable.GradientDrawable createCircleDrawable(int color) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(16, 16);
        return drawable;
    }

    private void updateTips(float income, float expenses, List<FinanceViewModel.Category> categories) {
        List<String> tips = new ArrayList<>();

        if (income > 0 && expenses > 0) {
            float ratio = (expenses / income) * 100;
            if (ratio > 80) tips.add("⚠️ Тратите " + String.format(Locale.getDefault(), "%.0f", ratio) + "% доходов");
            else if (ratio < 50) tips.add("✅ Тратите только " + String.format(Locale.getDefault(), "%.0f", ratio) + "% доходов");
        }

        if (!categories.isEmpty()) {
            FinanceViewModel.Category main = categories.get(0);
            float percent = (main.amount / expenses) * 100;
            if (percent > 40) tips.add("🎯 Основные траты: " + main.name + " (" + String.format(Locale.getDefault(), "%.0f", percent) + "%)");
        }

        if (tips.isEmpty()) tips.add("💡 Добавляйте операции для анализа");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < tips.size(); i++) {
            if (i > 0) result.append("\n\n");
            result.append("• ").append(tips.get(i));
        }
        financialTipsText.setText(result.toString());
    }

    private String formatMoney(float amount) {
        return amount == 0 ? "0 ₽" : String.format(Locale.getDefault(), "%.0f₽", amount);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}