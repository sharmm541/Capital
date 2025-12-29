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

    public Goal getGoalAt(int position) {
        if (position >= 0 && position < goals.size()) {
            return goals.get(position);
        }
        return null;
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
        private TextView titleText, amountText, progressText, deadlineText, statusText;
        private ProgressBar progressBar;
        private ImageButton editButton, deleteButton, addAmountButton;
        private View deadlineWarning, cardView;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.goalTitle);
            amountText = itemView.findViewById(R.id.goalAmount);
            progressText = itemView.findViewById(R.id.goalProgress);
            deadlineText = itemView.findViewById(R.id.goalDeadline);
            statusText = itemView.findViewById(R.id.goalStatus);
            progressBar = itemView.findViewById(R.id.goalProgressBar);
            editButton = itemView.findViewById(R.id.editGoalButton);
            deleteButton = itemView.findViewById(R.id.deleteGoalButton);
            addAmountButton = itemView.findViewById(R.id.addAmountButton);
            deadlineWarning = itemView.findViewById(R.id.deadlineWarning);
            cardView = itemView.findViewById(R.id.cardView);
        }

        public void bind(Goal goal) {
            // Устанавливаем базовые данные
            titleText.setText(goal.getTitle());
            amountText.setText(String.format("%s / %s ₽",
                    amountFormat.format(goal.getCurrentAmount()),
                    amountFormat.format(goal.getTargetAmount())));

            int progress = goal.getProgress();
            progressText.setText(progress + "%");
            progressBar.setProgress(progress);

            // Отображаем дедлайн с предупреждением
            if (goal.getDeadline() != null) {
                deadlineText.setText("До " + dateFormat.format(goal.getDeadline()));

                // Показываем предупреждение если дедлайн приближается
                if (goal.isDeadlineApproaching()) {
                    deadlineWarning.setVisibility(View.VISIBLE);
                    deadlineText.setTextColor(itemView.getContext().getColor(R.color.status_warning));
                } else if (goal.isOverdue()) {
                    deadlineWarning.setVisibility(View.VISIBLE);
                    deadlineText.setTextColor(itemView.getContext().getColor(R.color.status_error));
                    deadlineText.setText("Просрочено: " + dateFormat.format(goal.getDeadline()));
                } else {
                    deadlineWarning.setVisibility(View.GONE);
                    deadlineText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
                }
            } else {
                deadlineText.setText("Без срока");
                deadlineWarning.setVisibility(View.GONE);
                deadlineText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
            }

            // Настраиваем внешний вид в зависимости от статуса
            switch (goal.getStatus()) {
                case 0: // Активная
                    statusText.setText("Активная");
                    statusText.setTextColor(itemView.getContext().getColor(R.color.status_success));
                    setActiveAppearance();
                    break;

                case 1: // Неактивная
                    statusText.setText("Неактивная");
                    statusText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
                    setInactiveAppearance();
                    break;

                case 2: // Закрытая
                    statusText.setText("Закрытая");
                    statusText.setTextColor(itemView.getContext().getColor(R.color.status_success));
                    setClosedAppearance();
                    break;
            }

            setupClickListeners(goal);
        }

        private void setActiveAppearance() {
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            addAmountButton.setVisibility(View.VISIBLE);

            titleText.setTextColor(itemView.getContext().getColor(R.color.text_main_primary));
            amountText.setTextColor(itemView.getContext().getColor(R.color.text_main_primary));
            progressText.setTextColor(itemView.getContext().getColor(R.color.text_main_primary));
            cardView.setAlpha(1.0f);
        }

        private void setInactiveAppearance() {
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            addAmountButton.setVisibility(View.GONE);

            titleText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
            amountText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
            progressText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
            cardView.setAlpha(0.8f);
        }

        private void setClosedAppearance() {
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.VISIBLE);
            addAmountButton.setVisibility(View.GONE);

            titleText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
            amountText.setTextColor(itemView.getContext().getColor(R.color.text_main_secondary));
            progressText.setTextColor(itemView.getContext().getColor(R.color.status_success));
            cardView.setAlpha(1.0f);
        }

        private void setupClickListeners(Goal goal) {
            editButton.setOnClickListener(v -> {
                if (listener != null && goal.getStatus() != 2) { // Нельзя редактировать закрытые
                    listener.onEditGoal(goal);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteGoal(goal);
                }
            });

            addAmountButton.setOnClickListener(v -> {
                if (listener != null && goal.getStatus() == 0) { // Только для активных
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