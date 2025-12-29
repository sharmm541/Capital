package com.Taviak.capital.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.Taviak.capital.models.Currency;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class CurrencyManager {
    private Context context;
    private List<Currency> currencies;
    private Map<String, Double> exchangeRates;
    private Map<String, Double> previousExchangeRates;
    private SharedPreferences prefs;

    public CurrencyManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE);
        this.currencies = new ArrayList<>();
        this.exchangeRates = new HashMap<>();
        this.previousExchangeRates = new HashMap<>();

        initializeFallbackRates();

        // Загружаем кэшированные данные для быстрого отображения
        loadCachedRates();
    }

    public void fetchExchangeRates(CurrencyCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://www.cbr-xml-daily.ru/daily_json.js";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONObject valute = json.getJSONObject("Valute");

                Map<String, Double> newRates = new HashMap<>();
                Map<String, Double> newPreviousRates = new HashMap<>();

                // Добавляем RUB
                newRates.put("RUB", 1.0);
                newPreviousRates.put("RUB", 1.0);

                // Парсим курсы ЦБ РФ (текущие и предыдущие)
                parseCurrencyData(valute, "USD", newRates, newPreviousRates);
                parseCurrencyData(valute, "EUR", newRates, newPreviousRates);
                parseCurrencyData(valute, "CNY", newRates, newPreviousRates);
                parseCurrencyData(valute, "GBP", newRates, newPreviousRates);
                parseCurrencyData(valute, "JPY", newRates, newPreviousRates);

                System.out.println("Данные ЦБ РФ получены: " + newRates.size() + " валют");

                // В главном потоке обновляем UI
                if (context != null) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        // Обновляем текущие курсы
                        updateExchangeRates(newRates);

                        // Сохраняем предыдущие курсы из API
                        previousExchangeRates.clear();
                        previousExchangeRates.putAll(newPreviousRates);

                        // Кэшируем новые данные
                        cacheRates(newRates, newPreviousRates);

                        // Обновляем отображение с правильными изменениями
                        updateDisplayCurrenciesWithChanges(newRates, newPreviousRates);

                        if (callback != null) callback.onSuccess(currencies);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Ошибка получения данных ЦБ РФ: " + e.getMessage());

                if (context != null) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        useFallbackRates();
                        if (callback != null) callback.onError("Нет связи с ЦБ РФ. Используем сохраненные данные.");
                    });
                }
            }
        }).start();
    }

    private void parseCurrencyData(JSONObject valute, String currencyCode,
                                   Map<String, Double> currentRates,
                                   Map<String, Double> previousRates) {
        try {
            if (valute.has(currencyCode)) {
                JSONObject currency = valute.getJSONObject(currencyCode);

                // Текущий курс
                double currentRate = currency.getDouble("Value") / currency.getDouble("Nominal");
                currentRates.put(currencyCode, currentRate);

                // Предыдущий курс
                double previousRate = currency.getDouble("Previous") / currency.getDouble("Nominal");
                previousRates.put(currencyCode, previousRate);

                System.out.println(currencyCode + ": текущий=" + currentRate +
                        " RUB, предыдущий=" + previousRate + " RUB");
            }
        } catch (Exception e) {
            System.out.println("Ошибка парсинга " + currencyCode + ": " + e.getMessage());
        }
    }

    private void updateExchangeRates(Map<String, Double> newRates) {
        exchangeRates.clear();
        exchangeRates.putAll(newRates);

        System.out.println("Обновление курсов ЦБ РФ");
    }

    private void updateDisplayCurrenciesWithChanges(Map<String, Double> currentRates,
                                                    Map<String, Double> previousRates) {
        currencies.clear();

        // Для каждой валюты рассчитываем изменение
        for (Map.Entry<String, Double> entry : currentRates.entrySet()) {
            String currency = entry.getKey();
            double currentRate = entry.getValue();

            // Рассчитываем изменение только для основных валют
            if (currency.equals("USD") || currency.equals("EUR") || currency.equals("CNY")) {
                double previousRate = previousRates.getOrDefault(currency, 0.0);
                double change = calculateChange(currentRate, previousRate);

                String name = getCurrencyName(currency);
                currencies.add(new Currency(currency, name, currentRate, change));

                System.out.println(String.format(Locale.US,
                        "%s: текущий=%.2f ₽, предыдущий=%.2f ₽, изменение=%.2f",
                        currency, currentRate, previousRate, change));
            }
        }
    }

    private double calculateChange(double currentRate, double previousRate) {
        if (previousRate > 0) {
            // Рассчитываем абсолютное изменение курса (в рублях)
            double change = currentRate - previousRate;
            // Округляем до 2 знаков
            return Math.round(change * 100.0) / 100.0;
        }

        return 0.0;
    }

    private void initializeFallbackRates() {
        // Fallback курсы на случай недоступности API
        exchangeRates.put("USD", 90.5);
        exchangeRates.put("EUR", 98.2);
        exchangeRates.put("RUB", 1.0);
        exchangeRates.put("CNY", 12.5);
        exchangeRates.put("GBP", 115.0);
        exchangeRates.put("JPY", 0.6);

        // Fallback предыдущие курсы (такие же как текущие, изменение 0)
        previousExchangeRates.put("USD", 90.5);
        previousExchangeRates.put("EUR", 98.2);
        previousExchangeRates.put("RUB", 1.0);
        previousExchangeRates.put("CNY", 12.5);
        previousExchangeRates.put("GBP", 115.0);
        previousExchangeRates.put("JPY", 0.6);
    }

    private void useFallbackRates() {
        System.out.println("Используем fallback курсы");

        // Создаем копии для отображения
        Map<String, Double> currentRates = new HashMap<>(exchangeRates);
        Map<String, Double> previousRates = new HashMap<>(previousExchangeRates);

        updateDisplayCurrenciesWithChanges(currentRates, previousRates);
    }

    private void cacheRates(Map<String, Double> currentRates, Map<String, Double> previousRates) {
        SharedPreferences.Editor editor = prefs.edit();

        // Сохраняем текущие курсы
        for (Map.Entry<String, Double> entry : currentRates.entrySet()) {
            editor.putFloat("current_" + entry.getKey(), entry.getValue().floatValue());
        }

        // Сохраняем предыдущие курсы
        for (Map.Entry<String, Double> entry : previousRates.entrySet()) {
            editor.putFloat("previous_" + entry.getKey(), entry.getValue().floatValue());
        }

        // Сохраняем время обновления
        editor.putLong("last_update_time", System.currentTimeMillis());

        editor.apply();

        System.out.println("Курсы сохранены в кэш");
    }

    private void loadCachedRates() {
        try {
            long lastUpdateTime = prefs.getLong("last_update_time", 0);
            long currentTime = System.currentTimeMillis();

            // Если данные устарели (больше 2 часов), не загружаем из кэша
            if (currentTime - lastUpdateTime > 7200000) {
                System.out.println("Кэш устарел, время: " + (currentTime - lastUpdateTime) + "мс");
                return;
            }

            System.out.println("Загружаем кэшированные курсы");

            Map<String, Double> cachedCurrentRates = new HashMap<>();
            Map<String, Double> cachedPreviousRates = new HashMap<>();

            String[] currencies = {"USD", "EUR", "CNY", "GBP", "JPY", "RUB"};

            for (String currency : currencies) {
                // Загружаем текущие курсы
                float currentRate = prefs.getFloat("current_" + currency, 0f);
                if (currentRate > 0) {
                    cachedCurrentRates.put(currency, (double) currentRate);
                }

                // Загружаем предыдущие курсы
                float previousRate = prefs.getFloat("previous_" + currency, 0f);
                if (previousRate > 0) {
                    cachedPreviousRates.put(currency, (double) previousRate);
                }
            }

            if (!cachedCurrentRates.isEmpty() && !cachedPreviousRates.isEmpty()) {
                // Используем кэшированные данные для отображения
                exchangeRates.putAll(cachedCurrentRates);
                previousExchangeRates.putAll(cachedPreviousRates);

                updateDisplayCurrenciesWithChanges(cachedCurrentRates, cachedPreviousRates);

                System.out.println("Кэшированные курсы загружены успешно");
                System.out.println("USD: текущий=" + cachedCurrentRates.get("USD") +
                        ", предыдущий=" + cachedPreviousRates.get("USD"));
            } else {
                System.out.println("Нет данных в кэше или данные неполные");
            }
        } catch (Exception e) {
            System.out.println("Ошибка загрузки кэшированных данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getCurrencyName(String code) {
        switch (code) {
            case "USD": return "Доллар США";
            case "EUR": return "Евро";
            case "CNY": return "Китайский юань";
            case "GBP": return "Фунт стерлингов";
            case "JPY": return "Японская иена";
            case "RUB": return "Российский рубль";
            default: return code;
        }
    }

    public List<Currency> getCurrencies() {
        return currencies;
    }

    public double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (exchangeRates.isEmpty()) {
            useFallbackRates();
        }

        // Получаем курсы обеих валют к RUB
        double fromRate = getDirectRate(fromCurrency);
        double toRate = getDirectRate(toCurrency);

        if (fromRate == 0 || toRate == 0) {
            System.out.println("Курс не найден для конвертации: " + fromCurrency + " -> " + toCurrency);
            return 0;
        }

        // Конвертация через RUB
        double amountInRub = amount * fromRate;
        double result = amountInRub / toRate;

        System.out.println(String.format(Locale.US, "Конвертация: %.2f %s -> %.2f RUB -> %.2f %s",
                amount, fromCurrency, amountInRub, result, toCurrency));

        return Math.round(result * 100.0) / 100.0;
    }

    private double getDirectRate(String currency) {
        if (currency.equals("RUB")) {
            return 1.0;
        }

        Double rate = exchangeRates.get(currency);
        if (rate != null) {
            return rate;
        }

        // Fallback курсы если API не вернул данные
        switch (currency) {
            case "USD": return 90.5;
            case "EUR": return 98.2;
            case "CNY": return 12.5;
            case "GBP": return 115.0;
            case "JPY": return 0.6;
            default: return 1.0;
        }
    }

    public interface CurrencyCallback {
        void onSuccess(List<Currency> currencies);
        void onError(String message);
    }
}