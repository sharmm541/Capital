package com.Taviak.capital;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.Taviak.capital.managers.GoalManager;
import com.Taviak.capital.models.Goal;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "capital_app_channel";
    private static final int DEADLINE_NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("SHOW_FINANCE_NOTIFICATION".equals(intent.getAction())) {
            showRandomFinanceNotification(context);
            checkDeadlineNotifications(context); // Проверяем дедлайны

            // Проверяем настройки перед планированием следующего уведомления
            SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);

            if (notificationsEnabled) {
                scheduleNextNotification(context);
            }
        } else if ("CHECK_DEADLINES".equals(intent.getAction())) {
            checkDeadlineNotifications(context);
        }
    }

    private void showRandomFinanceNotification(Context context) {
        String[] notifications = {
                "Не забудьте проверить прогресс ваших финансовых целей! 📈",
                "Время обновить бюджет на этот месяц 💰",
                "Как продвигается достижение ваших финансовых целей? 🎯",
                "Проверьте свои траты за последние дни 📊",
                "Не забывайте откладывать на важные цели! 🏆",
                "Время провести финансовый обзор недели 📅",
                "Как ваши сбережения? Проверьте прогресс! 💎",
                "Не забудьте добавить последние транзакции 💳"
        };

        Random random = new Random();
        String message = notifications[random.nextInt(notifications.length)];

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Напоминание о финансах")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(random.nextInt(1000), builder.build());
        }
    }

    private void checkDeadlineNotifications(Context context) {
        GoalManager goalManager = new GoalManager(context);
        List<Goal> approachingGoals = goalManager.getGoalsWithApproachingDeadline();

        for (Goal goal : approachingGoals) {
            showDeadlineNotification(context, goal);
        }

        // Планируем следующую проверку дедлайнов
        scheduleDeadlineCheck(context);
    }

    private void showDeadlineNotification(Context context, Goal goal) {
        String message = String.format("Цель \"%s\" заканчивается %s! Успейте завершить!",
                goal.getTitle(),
                new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(goal.getDeadline()));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("Срок цели подходит к концу!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(DEADLINE_NOTIFICATION_ID + goal.getId(), builder.build());
        }
    }

    private void scheduleDeadlineCheck(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 12); // Проверяем каждые 12 часов

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent deadlineIntent = new Intent(context, NotificationReceiver.class);
        deadlineIntent.setAction("CHECK_DEADLINES");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                2002,
                deadlineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    private void scheduleNextNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        if (currentHour >= 22 || currentHour < 8) {
            scheduleMorningNotification(context);
            return;
        }

        Random random = new Random();
        long nextDelay = getRandomDelayInMillis(random);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.setAction("SHOW_FINANCE_NOTIFICATION");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                2001,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long nextTriggerTime = System.currentTimeMillis() + nextDelay;

        Calendar nextTime = Calendar.getInstance();
        nextTime.setTimeInMillis(nextTriggerTime);
        int nextHour = nextTime.get(Calendar.HOUR_OF_DAY);

        if (nextHour >= 22 || nextHour < 8) {
            scheduleMorningNotification(context);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                );
            }

            SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            prefs.edit().putLong("next_notification_time", nextTriggerTime).apply();
        }
    }

    private void scheduleMorningNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.setAction("SHOW_FINANCE_NOTIFICATION");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                2001,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }

        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        prefs.edit().putLong("next_notification_time", calendar.getTimeInMillis()).apply();
    }

    private long getRandomDelayInMillis(Random random) {
        int hours = 2 + random.nextInt(5);
        return hours * 60 * 60 * 1000L;
    }
}