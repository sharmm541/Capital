package com.Taviak.capital.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Taviak.capital.R;
import com.Taviak.capital.adapters.GoalsAdapter;
import com.Taviak.capital.managers.GoalManager;
import com.Taviak.capital.models.Goal;
import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class GoalsFragment extends Fragment implements GoalsAdapter.OnGoalActionListener {

    private TextView overallProgress, completedGoals;
    private ProgressBar progressBar;
    private RecyclerView activeGoalsRecyclerView, completedGoalsRecyclerView;
    private GoalsAdapter activeGoalsAdapter, completedGoalsAdapter;
    private GoalManager goalManager;
    private MaterialButton addGoalButton;
    private DecimalFormat amountFormat = new DecimalFormat("#,##0.00");

    public GoalsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goals, container, false);

        initViews(view);
        setupGoalManager();
        setupRecyclerViews();
        updateUI();

        return view;
    }

    private void initViews(View view) {
        overallProgress = view.findViewById(R.id.overallProgress);
        completedGoals = view.findViewById(R.id.completedGoals);
        progressBar = view.findViewById(R.id.progressBar);
        activeGoalsRecyclerView = view.findViewById(R.id.activeGoalsRecyclerView);
        completedGoalsRecyclerView = view.findViewById(R.id.completedGoalsRecyclerView);

        // Кнопка создания цели
        addGoalButton = view.findViewById(R.id.addGoalButton);
        addGoalButton.setOnClickListener(v -> showCreateGoalDialog());
    }

    private void setupGoalManager() {
        goalManager = new GoalManager(requireContext());
    }

    private void setupRecyclerViews() {
        // Активные цели
        activeGoalsAdapter = new GoalsAdapter(goalManager.getActiveGoals());
        activeGoalsAdapter.setOnGoalActionListener(this);
        activeGoalsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        activeGoalsRecyclerView.setAdapter(activeGoalsAdapter);

        // Завершенные цели
        completedGoalsAdapter = new GoalsAdapter(goalManager.getCompletedGoals());
        completedGoalsAdapter.setOnGoalActionListener(this);
        completedGoalsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        completedGoalsRecyclerView.setAdapter(completedGoalsAdapter);
    }

    private void updateUI() {
        int totalProgress = goalManager.getOverallProgress();
        int completedCount = goalManager.getCompletedGoalsCount();
        int totalCount = goalManager.getTotalGoalsCount();

        overallProgress.setText(totalProgress + "%");
        completedGoals.setText("Выполнено: " + completedCount + "/" + totalCount + " целей");
        progressBar.setProgress(totalProgress);

        activeGoalsAdapter.updateGoals(goalManager.getActiveGoals());
        completedGoalsAdapter.updateGoals(goalManager.getCompletedGoals());
    }

    // Диалог создания цели
    private void showCreateGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Новая цель");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_goal_edit, null);
        builder.setView(dialogView);

        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        EditText targetAmountInput = dialogView.findViewById(R.id.targetAmountInput);
        EditText deadlineInput = dialogView.findViewById(R.id.deadlineInput);

        // Настраиваем DatePicker для поля даты
        setupDatePicker(deadlineInput);

        builder.setPositiveButton("Создать", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String amountStr = targetAmountInput.getText().toString().trim();
            String deadlineStr = deadlineInput.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Заполните обязательные поля", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Заменяем запятые на точки для корректного парсинга
                String normalizedAmount = amountStr.replace(',', '.');
                double targetAmount = Double.parseDouble(normalizedAmount);

                if (targetAmount <= 0) {
                    Toast.makeText(requireContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                Goal goal = new Goal();
                goal.setTitle(title);
                goal.setDescription(description);
                goal.setTargetAmount(targetAmount);

                // Обработка даты дедлайна
                if (!TextUtils.isEmpty(deadlineStr)) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        dateFormat.setLenient(false); // Строгая проверка формата
                        Date deadline = dateFormat.parse(deadlineStr);

                        // Проверяем что дата не в прошлом
                        if (deadline.before(new Date())) {
                            Toast.makeText(requireContext(), "Дата не может быть в прошлом", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        goal.setDeadline(deadline);
                    } catch (ParseException e) {
                        Toast.makeText(requireContext(), "Неверный формат даты. Используйте дд.мм.гггг", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                goalManager.createGoal(goal, new GoalManager.GoalCallback() {
                    @Override
                    public void onSuccess(Goal goal) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Цель создана", Toast.LENGTH_SHORT).show();
                            updateUI();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Ошибка: " + message, Toast.LENGTH_SHORT).show());
                    }
                });

            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Неверный формат суммы. Используйте числа (например: 1000 или 1000.50)", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Отмена", null);

        // Создаем и настраиваем диалог
        AlertDialog dialog = builder.create();
        dialog.show();

        // Настраиваем кнопки
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.accent_blue));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
        }
    }

    // Реализация методов интерфейса OnGoalActionListener
    @Override
    public void onEditGoal(Goal goal) {
        showEditGoalDialog(goal);
    }

    @Override
    public void onDeleteGoal(Goal goal) {
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setTitle("Удаление цели")
                .setMessage("Вы уверены, что хотите удалить цель \"" + goal.getTitle() + "\"?")
                .setPositiveButton("Удалить", (dialogInterface, which) -> {
                    goalManager.deleteGoal(goal.getId(), new GoalManager.GoalCallback() {
                        @Override
                        public void onSuccess(Goal goal) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Цель удалена", Toast.LENGTH_SHORT).show();
                                updateUI();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Ошибка: " + message, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .create();

        dialog.show();

        // Настраиваем кнопки
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.error_main));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
        }
    }

    @Override
    public void onAddAmount(Goal goal) {
        showAddAmountDialog(goal);
    }

    @Override
    public void onGoalClick(Goal goal) {
        showGoalDetailsDialog(goal);
    }

    private void showEditGoalDialog(Goal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Редактировать цель");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_goal_edit, null);
        builder.setView(dialogView);

        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        EditText targetAmountInput = dialogView.findViewById(R.id.targetAmountInput);
        EditText deadlineInput = dialogView.findViewById(R.id.deadlineInput);

        // Настраиваем DatePicker для поля даты
        setupDatePicker(deadlineInput);

        // Заполняем поля текущими данными цели
        titleInput.setText(goal.getTitle());
        descriptionInput.setText(goal.getDescription());
        targetAmountInput.setText(String.valueOf(goal.getTargetAmount()));

        if (goal.getDeadline() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            deadlineInput.setText(dateFormat.format(goal.getDeadline()));
        }

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String amountStr = targetAmountInput.getText().toString().trim();
            String deadlineStr = deadlineInput.getText().toString().trim();

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Заполните обязательные поля", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Заменяем запятые на точки для корректного парсинга
                String normalizedAmount = amountStr.replace(',', '.');
                double targetAmount = Double.parseDouble(normalizedAmount);

                if (targetAmount <= 0) {
                    Toast.makeText(requireContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                goal.setTitle(title);
                goal.setDescription(description);
                goal.setTargetAmount(targetAmount);

                // Обработка даты дедлайна
                if (!TextUtils.isEmpty(deadlineStr)) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        dateFormat.setLenient(false); // Строгая проверка формата
                        Date deadline = dateFormat.parse(deadlineStr);

                        // Проверяем что дата не в прошлом
                        if (deadline.before(new Date())) {
                            Toast.makeText(requireContext(), "Дата не может быть в прошлом", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        goal.setDeadline(deadline);
                    } catch (ParseException e) {
                        Toast.makeText(requireContext(), "Неверный формат даты. Используйте дд.мм.гггг", Toast.LENGTH_LONG).show();
                        return;
                    }
                } else {
                    goal.setDeadline(null); // Сбрасываем дедлайн если поле пустое
                }

                goalManager.updateGoal(goal, new GoalManager.GoalCallback() {
                    @Override
                    public void onSuccess(Goal goal) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Цель обновлена", Toast.LENGTH_SHORT).show();
                            updateUI();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Ошибка: " + message, Toast.LENGTH_SHORT).show());
                    }
                });

            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Неверный формат суммы. Используйте числа (например: 1000 или 1000.50)", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Отмена", null);

        // Создаем и настраиваем диалог
        AlertDialog dialog = builder.create();
        dialog.show();

        // Настраиваем кнопки
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.accent_blue));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
        }
    }

    private void showAddAmountDialog(Goal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Добавить сумму");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_amount, null);
        builder.setView(dialogView);

        TextView progressInfo = dialogView.findViewById(R.id.progressInfo);
        EditText amountInput = dialogView.findViewById(R.id.amountInput);

        // Устанавливаем информацию о текущем прогрессе
        progressInfo.setText(String.format("Текущий прогресс: %.2f / %.2f ₽ (%d%%)",
                goal.getCurrentAmount(), goal.getTargetAmount(), goal.getProgress()));

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String amountStr = amountInput.getText().toString().trim();
            if (!TextUtils.isEmpty(amountStr)) {
                try {
                    // Заменяем запятые на точки для корректного парсинга
                    String normalizedAmount = amountStr.replace(',', '.');
                    double amount = Double.parseDouble(normalizedAmount);

                    if (amount <= 0) {
                        Toast.makeText(requireContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    goalManager.addAmountToGoal(goal.getId(), amount, new GoalManager.GoalCallback() {
                        @Override
                        public void onSuccess(Goal updatedGoal) {
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Сумма добавлена", Toast.LENGTH_SHORT).show();
                                updateUI();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Ошибка: " + message, Toast.LENGTH_SHORT).show());
                        }
                    });
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Неверный формат суммы. Используйте числа (например: 1000 или 1000.50)", Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.setNegativeButton("Отмена", null);

        // Создаем и настраиваем диалог
        AlertDialog dialog = builder.create();
        dialog.show();

        // Настраиваем кнопки
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.accent_blue));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
        }
    }

    private void showGoalDetailsDialog(Goal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);
        builder.setTitle("Информация о цели");

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_goal_info, null);

        TextView titleText = view.findViewById(R.id.titleText);
        TextView descriptionText = view.findViewById(R.id.descriptionText);
        TextView amountText = view.findViewById(R.id.amountText);
        TextView progressText = view.findViewById(R.id.progressText);
        TextView createdAtText = view.findViewById(R.id.createdAtText);
        TextView deadlineText = view.findViewById(R.id.deadlineText);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        // Заполняем данные
        titleText.setText(goal.getTitle());
        descriptionText.setText(goal.getDescription());
        amountText.setText(String.format("%.2f / %.2f ₽",
                goal.getCurrentAmount(), goal.getTargetAmount()));
        progressText.setText(goal.getProgress() + "%");
        progressBar.setProgress(goal.getProgress());

        // Дата создания
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        createdAtText.setText(dateFormat.format(goal.getCreatedAt()));

        // Дедлайн
        if (goal.getDeadline() != null) {
            deadlineText.setText(dateFormat.format(goal.getDeadline()));
        } else {
            deadlineText.setText("Без срока");
        }

        builder.setView(view);
        builder.setPositiveButton("OK", null);

        if (!goal.isCompleted()) {
            builder.setNeutralButton("Выполнить", (dialog, which) -> {
                goalManager.markGoalAsCompleted(goal.getId(), new GoalManager.GoalCallback() {
                    @Override
                    public void onSuccess(Goal goal) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Цель выполнена!", Toast.LENGTH_SHORT).show();
                            updateUI();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Ошибка: " + message, Toast.LENGTH_SHORT).show());
                    }
                });
            });
        }

        // Создаем и настраиваем диалог
        AlertDialog dialog = builder.create();
        dialog.show();

        // Настраиваем кнопки
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.accent_blue));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
        }
        if (neutralButton != null) {
            neutralButton.setTextColor(getResources().getColor(R.color.status_success));
        }
    }

    private void setupDatePicker(EditText deadlineInput) {
        deadlineInput.setFocusable(false);
        deadlineInput.setOnClickListener(v -> showDatePickerDialog(deadlineInput));

        // Также обрабатываем долгое нажатие для очистки
        deadlineInput.setOnLongClickListener(v -> {
            deadlineInput.setText("");
            return true;
        });
    }

    private void showDatePickerDialog(EditText deadlineInput) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                R.style.DatePickerDialogTheme,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Форматируем дату в нужный формат
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(selectedDate.getTime());
                    deadlineInput.setText(formattedDate);
                },
                year, month, day
        );

        // Устанавливаем минимальную дату - сегодня
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();

        // Настраиваем кнопки DatePickerDialog
        Button positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE);
        Button negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.accent_blue));
            // Можно также изменить текст кнопки, если нужно
            // positiveButton.setText("Выбрать");
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.text_main_secondary));
            // Можно также изменить текст кнопки, если нужно
            // negativeButton.setText("Отмена");
        }
    }
}