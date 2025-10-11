package com.Taviak.capital.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.Taviak.capital.R;
import com.Taviak.capital.models.Transaction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private List<Transaction> transactions;

    public TransactionsAdapter(List<Transaction> transactions) {
        this.transactions = transactions != null ? new ArrayList<>(transactions) : new ArrayList<>();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions != null ? new ArrayList<>(newTransactions) : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ДОБАВЬТЕ ЭТОТ МЕТОД
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        holder.typeText.setText(transaction.getType());

        // Разный цвет для доходов и расходов
        if (transaction.isIncome()) {
            holder.amountText.setText(String.format("+%.0f ₽", transaction.getAmount()));
            holder.amountText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_success));
        } else {
            holder.amountText.setText(String.format("-%.0f ₽", transaction.getAmount()));
            holder.amountText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error_main));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        holder.dateText.setText(sdf.format(transaction.getDate()));

        // ДОБАВЬТЕ ЭТОТ БЛОК ДЛЯ ОТОБРАЖЕНИЯ ОПИСАНИЯ
        String description = transaction.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            holder.descriptionText.setVisibility(View.VISIBLE);
            holder.descriptionText.setText(description);
        } else {
            holder.descriptionText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView typeText, amountText, dateText, descriptionText; // ДОБАВЬТЕ descriptionText

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.transactionCategory);
            amountText = itemView.findViewById(R.id.transactionAmount);
            dateText = itemView.findViewById(R.id.transactionDate);
            descriptionText = itemView.findViewById(R.id.transactionDescription); // ДОБАВЬТЕ ЭТУ СТРОКУ
        }
    }
}