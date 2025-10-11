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

    public Goal() {
        this.createdAt = new Date();
        this.completed = false;
        this.currentAmount = 0;
        this.priority = 2;
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

    // Расчет прогресса в процентах
    public int getProgress() {
        if (targetAmount == 0) return 0;
        return (int) ((currentAmount / targetAmount) * 100);
    }

    // Осталось собрать
    public double getRemainingAmount() {
        return targetAmount - currentAmount;
    }
}