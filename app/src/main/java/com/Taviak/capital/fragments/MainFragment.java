package com.Taviak.capital.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Taviak.capital.MainActivity;
import com.Taviak.capital.R;
import com.Taviak.capital.adapters.TransactionsAdapter;
import com.Taviak.capital.models.Transaction;
import com.Taviak.capital.viewmodels.TransactionViewModel;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

    private TextView balanceAmount, balanceChange;
    private RecyclerView recentTransactionsRecyclerView;
    private TransactionsAdapter adapter;
    private TransactionViewModel transactionViewModel;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupClickListeners(view);

        return view;
    }

    private void initViews(View view) {
        balanceAmount = view.findViewById(R.id.balanceAmount);
        balanceChange = view.findViewById(R.id.balanceChange);
        recentTransactionsRecyclerView = view.findViewById(R.id.recentTransactionsRecyclerView);
    }

    private void setupViewModel() {
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // Наблюдаем за балансом из базы данных
        transactionViewModel.getTotalBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                updateBalanceUI(balance);
            } else {
                updateBalanceUI(0.0);
            }
        });

        // Наблюдаем за последними транзакциями (максимум 5)
        transactionViewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                updateRecentTransactions(transactions);
            } else {
                adapter.updateTransactions(new ArrayList<>());
            }
        });
    }

    private void updateBalanceUI(Double balance) {
        // Форматируем сумму с пробелами между тысячами
        String formattedBalance = String.format("%,d ₽", balance.intValue())
                .replace(',', ' ');
        balanceAmount.setText(formattedBalance);

        // Обновляем текст изменения баланса на основе реальных данных
        if (balance > 0) {
            balanceChange.setText("Положительный баланс");
            balanceChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_success));
        } else if (balance < 0) {
            balanceChange.setText("Отрицательный баланс");
            balanceChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_main));
        } else {
            balanceChange.setText("Баланс нулевой");
            balanceChange.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main_secondary));
        }
    }

    private void updateRecentTransactions(List<Transaction> transactions) {
        if (transactions != null && !transactions.isEmpty()) {
            // Берем последние 5 транзакций
            List<Transaction> recentTransactions = transactions.size() > 5 ?
                    transactions.subList(0, 5) : transactions;
            adapter.updateTransactions(recentTransactions);
        } else {
            adapter.updateTransactions(new ArrayList<>());
        }
    }

    private void setupRecyclerView() {
        adapter = new TransactionsAdapter(new ArrayList<>());
        recentTransactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentTransactionsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners(View view) {
        // Обработчики кликов для кнопок быстрого доступа
        view.findViewById(R.id.addIncomeButton).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new IncomeFragment());
            }
        });

        view.findViewById(R.id.addExpenseButton).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new ExpensesFragment());
            }
        });

        view.findViewById(R.id.goalsButton).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new GoalsFragment());
            }
        });

        // Добавляем кнопку для курса валют, если она есть в layout
        View currencyButton = view.findViewById(R.id.currencyButton);
        if (currencyButton != null) {
            currencyButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showCurrencyFragment();
                }
            });
        }
    }
}