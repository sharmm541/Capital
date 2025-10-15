package com.Taviak.capital.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import com.Taviak.capital.converters.DateConverter;
import com.Taviak.capital.entities.User;
import com.Taviak.capital.models.Transaction;
import com.Taviak.capital.models.Goal;

@Database(
        entities = {User.class, Transaction.class, Goal.class},
        version = 2,
        exportSchema = false
)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract TransactionDao transactionDao();
    public abstract UserDao userDao();
    public abstract GoalDao goalDao();

    // Убрали incomeDao()

    public static synchronized AppDatabase getDatabase(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "capital-database"
                    )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}