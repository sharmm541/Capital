package com.Taviak.capital.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.Taviak.capital.R;
import com.Taviak.capital.models.Goal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {

    private List<Goal> goals;
    private OnGoalActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private DecimalFormat amountFormat = new DecimalFormat("#,##0.00");

    public GoalsAdapter(List<Goal> goals) {
        this.goals = goals;
    }

    public void setOnGoalActionListener(OnGoalActionListener listener) {
        this.listener = listener;
    }

    public void updateGoals(List<Goal> goals) {
        this.goals = goals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goals.get(position);
        holder.bind(goal);
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    class GoalViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText, amountText, progressText, deadlineText;
        private ProgressBar progressBar;
        private ImageButton editButton, deleteButton, addAmountButton;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.goalTitle);
            amountText = itemView.findViewById(R.id.goalAmount);
            progressText = itemView.findViewById(R.id.goalProgress);
            deadlineText = itemView.findViewById(R.id.goalDeadline);
            progressBar = itemView.findViewById(R.id.goalProgressBar);
            editButton = itemView.findViewById(R.id.editGoalButton);
            deleteButton = itemView.findViewById(R.id.deleteGoalButton);
            addAmountButton = itemView.findViewById(R.id.addAmountButton);
        }

        public void bind(Goal goal) {
            titleText.setText(goal.getTitle());
            amountText.setText(String.format("%s / %s ₽",
                    amountFormat.format(goal.getCurrentAmount()),
                    amountFormat.format(goal.getTargetAmount())));

            int progress = goal.getProgress();
            progressText.setText(progress + "%");
            progressBar.setProgress(progress);

            if (goal.getDeadline() != null) {
                deadlineText.setText("До " + dateFormat.format(goal.getDeadline()));
            } else {
                deadlineText.setText("Без срока");
            }

            // Настройка видимости кнопок в зависимости от статуса цели
            if (goal.isCompleted()) {
                addAmountButton.setVisibility(View.GONE);
                editButton.setVisibility(View.GONE);
                progressText.setTextColor(itemView.getContext().getColor(R.color.status_success));
            } else {
                addAmountButton.setVisibility(View.VISIBLE);
                editButton.setVisibility(View.VISIBLE);
                progressText.setTextColor(itemView.getContext().getColor(R.color.accent_blue));
            }

            // Обработчики кликов
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditGoal(goal);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteGoal(goal);
                }
            });

            addAmountButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddAmount(goal);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGoalClick(goal);
                }
            });
        }
    }

    public interface OnGoalActionListener {
        void onEditGoal(Goal goal);
        void onDeleteGoal(Goal goal);
        void onAddAmount(Goal goal);
        void onGoalClick(Goal goal);
    }
}