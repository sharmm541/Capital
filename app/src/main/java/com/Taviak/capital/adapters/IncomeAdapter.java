package com.Taviak.capital.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.Taviak.capital.R;
import com.Taviak.capital.models.Transaction;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IncomeAdapter extends RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder> {

    private List<Transaction> incomes;

    public IncomeAdapter(List<Transaction> incomes) {
        this.incomes = incomes != null ? incomes : new java.util.ArrayList<>();
    }

    @NonNull
    @Override
    public IncomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_income, parent, false);
        return new IncomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncomeViewHolder holder, int position) {
        Transaction income = incomes.get(position);

        holder.incomeType.setText(income.getType());
        holder.incomeCategory.setText(income.getCategory());
        holder.incomeAmount.setText(String.format("%.0f ₽", income.getAmount()));

        // Описание (если есть)
        if (income.getDescription() != null && !income.getDescription().isEmpty()) {
            holder.incomeDescription.setText(income.getDescription());
            holder.incomeDescription.setVisibility(View.VISIBLE);
        } else {
            holder.incomeDescription.setVisibility(View.GONE);
        }

        // Дата
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        holder.incomeDate.setText(sdf.format(income.getDate()));
    }

    @Override
    public int getItemCount() {
        return incomes.size();
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        TextView incomeType, incomeCategory, incomeDescription, incomeAmount, incomeDate;

        public IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            incomeType = itemView.findViewById(R.id.incomeType);
            incomeCategory = itemView.findViewById(R.id.incomeCategory);
            incomeDescription = itemView.findViewById(R.id.incomeDescription);
            incomeAmount = itemView.findViewById(R.id.incomeAmount);
            incomeDate = itemView.findViewById(R.id.incomeDate);
        }
    }
}