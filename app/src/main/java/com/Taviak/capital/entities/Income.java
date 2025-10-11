package com.Taviak.capital.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.Taviak.capital.converters.DateConverter;
import java.util.Date;

@Entity(tableName = "incomes")
@TypeConverters(DateConverter.class)
public class Income {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private double amount;
    private String category;
    private String description;
    private Date date;
    private String type;

    public Income(double amount, String category, String description, Date date, String type) {
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
        this.type = type;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}