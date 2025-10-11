package com.Taviak.capital.managers;

import android.content.Context;
import com.Taviak.capital.database.AppDatabase;
import com.Taviak.capital.database.GoalDao;
import com.Taviak.capital.models.Goal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoalManager {
    private GoalDao goalDao;
    private ExecutorService executor;

    public GoalManager(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.goalDao = database.goalDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // Создание цели
    public void createGoal(Goal goal, GoalCallback callback) {
        executor.execute(() -> {
            try {
                goalDao.insert(goal);
                if (callback != null) {
                    callback.onSuccess(goal);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Редактирование цели
    public void updateGoal(Goal goal, GoalCallback callback) {
        executor.execute(() -> {
            try {
                goalDao.update(goal);
                if (callback != null) {
                    callback.onSuccess(goal);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Удаление цели
    public void deleteGoal(int goalId, GoalCallback callback) {
        executor.execute(() -> {
            try {
                Goal goal = goalDao.getGoalById(goalId);
                if (goal != null) {
                    goalDao.delete(goal);
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Цель не найдена");
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Погашение цели (добавление суммы)
    public void addAmountToGoal(int goalId, double amount, GoalCallback callback) {
        executor.execute(() -> {
            try {
                Goal goal = goalDao.getGoalById(goalId);
                if (goal != null) {
                    double newAmount = goal.getCurrentAmount() + amount;
                    goal.setCurrentAmount(newAmount);

                    // Автоматически помечаем как выполненную если достигли цели
                    if (newAmount >= goal.getTargetAmount()) {
                        goal.setCompleted(true);
                        goal.setCurrentAmount(goal.getTargetAmount()); // Не превышаем целевую сумму
                    }

                    goalDao.update(goal);
                    if (callback != null) {
                        callback.onSuccess(goal);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Цель не найдена");
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Отметить как выполненную
    public void markGoalAsCompleted(int goalId, GoalCallback callback) {
        executor.execute(() -> {
            try {
                goalDao.markAsCompleted(goalId);
                Goal goal = goalDao.getGoalById(goalId);
                if (callback != null) {
                    callback.onSuccess(goal);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Получить активные цели
    public List<Goal> getActiveGoals() {
        try {
            return goalDao.getActiveGoals();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // Получить выполненные цели
    public List<Goal> getCompletedGoals() {
        try {
            return goalDao.getCompletedGoals();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // Получить все цели
    public List<Goal> getAllGoals() {
        try {
            return goalDao.getAllGoals();
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    // Получить общий прогресс
    public int getOverallProgress() {
        try {
            Double progress = goalDao.getAverageProgress();
            return progress != null ? (int) Math.round(progress) : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Получить количество выполненных целей
    public int getCompletedGoalsCount() {
        try {
            return goalDao.getCompletedCount();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Получить общее количество целей
    public int getTotalGoalsCount() {
        try {
            return goalDao.getTotalCount();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public interface GoalCallback {
        void onSuccess(Goal goal);
        void onError(String message);
    }
}