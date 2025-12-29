package com.Taviak.capital.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.Taviak.capital.converters.DateConverter;

import java.util.Date;

@Entity(tableName = "goals")
@TypeConverters(DateConverter.class)
public class Goal {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String description;
    private double targetAmount;
    private double currentAmount;
    private Date deadline;
    private Date createdAt;
    private boolean completed;
    private String category;
    private int priority; // 1 - низкий, 2 - средний, 3 - высокий
    private int status; // 0 - активная, 1 - неактивная, 2 - закрытая
    private Date completedAt; // дата завершения цели

    public Goal() {
        this.createdAt = new Date();
        this.completed = false;
        this.currentAmount = 0;
        this.priority = 2;
        this.status = 0; // По умолчанию активная
        this.completedAt = null;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    // Расчет прогресса в процентах
    public int getProgress() {
        if (targetAmount == 0) return 0;
        return (int) ((currentAmount / targetAmount) * 100);
    }

    // Осталось собрать
    public double getRemainingAmount() {
        return targetAmount - currentAmount;
    }

    // Проверка на скорое завершение (менее 3 дней)
    public boolean isDeadlineApproaching() {
        if (deadline == null) return false;

        long currentTime = System.currentTimeMillis();
        long deadlineTime = deadline.getTime();
        long threeDays = 3 * 24 * 60 * 60 * 1000L;

        return (deadlineTime - currentTime) <= threeDays && (deadlineTime - currentTime) > 0;
    }

    // Проверка на просроченность
    public boolean isOverdue() {
        if (deadline == null) return false;
        return deadline.getTime() < System.currentTimeMillis();
    }
}