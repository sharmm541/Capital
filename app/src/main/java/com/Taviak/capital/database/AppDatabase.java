package com.Taviak.capital.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import com.Taviak.capital.converters.DateConverter;
import com.Taviak.capital.entities.User;
import com.Taviak.capital.entities.Income;
import com.Taviak.capital.models.Transaction;
import com.Taviak.capital.models.Goal;

@Database(
        entities = {User.class, Income.class, Transaction.class, Goal.class}, // Goal.class вместо GoalDao.class
        version = 2, // Увеличьте версию
        exportSchema = false
)
@TypeConverters(DateConverter.class) // Добавьте конвертер здесь
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract TransactionDao transactionDao();
    public abstract UserDao userDao();
    public abstract IncomeDao incomeDao();
    public abstract GoalDao goalDao();

    public static synchronized AppDatabase getDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "capital-database"
                    )
                    .fallbackToDestructiveMigration() // Пересоздаст базу при изменениях
                    .allowMainThreadQueries() // Для упрощения разработки
                    .build();
        }
        return instance;
    }
}