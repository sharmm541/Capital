package com.Taviak.capital.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.Taviak.capital.models.Goal;
import java.util.List;

@Dao
public interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY priority DESC, deadline ASC")
    List<Goal> getAllGoals();

    @Query("SELECT * FROM goals WHERE status = 0 ORDER BY priority DESC, deadline ASC")
    List<Goal> getActiveGoals();

    @Query("SELECT * FROM goals WHERE status = 1 ORDER BY priority DESC, deadline ASC")
    List<Goal> getInactiveGoals();

    @Query("SELECT * FROM goals WHERE status = 2 ORDER BY createdAt DESC")
    List<Goal> getClosedGoals();

    @Query("SELECT * FROM goals WHERE completed = 1 ORDER BY createdAt DESC")
    List<Goal> getCompletedGoals();

    @Query("SELECT * FROM goals WHERE id = :id")
    Goal getGoalById(int id);

    // Получить цели с приближающимся дедлайном
    @Query("SELECT * FROM goals WHERE status = 0 AND deadline IS NOT NULL AND deadline <= :thresholdDate AND deadline > :currentDate")
    List<Goal> getGoalsWithApproachingDeadline(long thresholdDate, long currentDate);

    @Insert
    void insert(Goal goal);

    @Update
    void update(Goal goal);

    @Delete
    void delete(Goal goal);

    @Query("DELETE FROM goals WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE goals SET completed = 1 WHERE id = :id")
    void markAsCompleted(int id);

    @Query("UPDATE goals SET currentAmount = :amount WHERE id = :id")
    void updateCurrentAmount(int id, double amount);

    @Query("UPDATE goals SET status = :status WHERE id = :id")
    void updateStatus(int id, int status);

    @Query("SELECT COUNT(*) FROM goals")
    int getTotalCount();

    @Query("SELECT COUNT(*) FROM goals WHERE completed = 1")
    int getCompletedCount();

    @Query("SELECT AVG((currentAmount / targetAmount) * 100) FROM goals WHERE completed = 0")
    Double getAverageProgress();

    @Query("DELETE FROM goals")
    void deleteAllGoals();
}