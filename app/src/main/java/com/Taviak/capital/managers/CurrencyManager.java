package com.Taviak.capital.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.Taviak.capital.api.CurrencyApi;
import com.Taviak.capital.api.CurrencyApiService;
import com.Taviak.capital.models.Currency;
import com.Taviak.capital.models.CurrencyResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.*;

public class CurrencyManager {
    private Context context;
    private CurrencyApi api;
    private List<Currency> currencies;
    private Map<String, Double> exchangeRates;
    private SharedPreferences prefs;

    public CurrencyManager(Context context) {
        this.context = context;
        this.api = CurrencyApiService.getClient().create(CurrencyApi.class);
        this.prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE);
        this.currencies = new ArrayList<>();
        this.exchangeRates = new HashMap<>();

        initializeFallbackRates();
        loadCachedRates();
    }

    public void fetchExchangeRates(CurrencyCallback callback) {
        // Используем RUB как базовую валюту для API ЦБ РФ
        api.getExchangeRates("RUB").enqueue(new Callback<CurrencyResponse>() {
            @Override
            public void onResponse(Call<CurrencyResponse> call, Response<CurrencyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CurrencyResponse currencyResponse = response.body();
                    Map<String, Double> rates = currencyResponse.getRates();

                    if (rates != null && !rates.isEmpty()) {
                        System.out.println("API данные получены: " + rates.size() + " валют");
                        updateExchangeRates(rates);
                        cacheRates();
                        if (callback != null) callback.onSuccess(currencies);
                    } else {
                        System.out.println("API вернул пустые данные");
                        useFallbackRates();
                        if (callback != null) callback.onError("Нет данных о курсах");
                    }
                } else {
                    System.out.println("Ошибка API: " + response.code());
                    useFallbackRates();
                    if (callback != null) callback.onSuccess(currencies); // Все равно успех с fallback
                }
            }

            @Override
            public void onFailure(Call<CurrencyResponse> call, Throwable t) {
                System.out.println("Ошибка сети: " + t.getMessage());
                useFallbackRates();
                if (callback != null) callback.onSuccess(currencies); // Все равно успех с fallback
            }
        });
    }

    private void updateExchangeRates(Map<String, Double> rates) {
        exchangeRates.clear();
        exchangeRates.putAll(rates);
        exchangeRates.put("RUB", 1.0); // RUB как базовая валюта

        System.out.println("Обновление курсов. Базовая валюта: RUB");
        System.out.println("Доступные валюты: " + rates.keySet());

        // Обновляем курсы для отображения
        updateDisplayCurrencies();
    }

    private void updateDisplayCurrencies() {
        currencies.clear();

        // Получаем прямые курсы к RUB
        double usdRate = getDirectRate("USD");
        double eurRate = getDirectRate("EUR");
        double cnyRate = getDirectRate("CNY");

        // Рассчитываем изменения (просто примерные значения)
        double usdChange = calculateChange("USD", usdRate);
        double eurChange = calculateChange("EUR", eurRate);
        double cnyChange = calculateChange("CNY", cnyRate);

        // Создаем объекты Currency для отображения
        currencies.add(new Currency("USD", "Доллар США", usdRate, usdChange));
        currencies.add(new Currency("EUR", "Евро", eurRate, eurChange));
        currencies.add(new Currency("CNY", "Китайский юань", cnyRate, cnyChange));

        System.out.println("Курсы для отображения:");
        System.out.println("USD: " + usdRate + " RUB");
        System.out.println("EUR: " + eurRate + " RUB");
        System.out.println("CNY: " + cnyRate + " RUB");
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

    private double calculateChange(String currency, double currentRate) {
        // Получаем предыдущий курс из кэша
        double previousRate = prefs.getFloat(currency + "_rate", 0f);

        if (previousRate > 0) {
            // Рассчитываем процент изменения
            return ((currentRate - previousRate) / previousRate) * 100;
        }

        // Если нет предыдущих данных, возвращаем случайное небольшое изменение
        Random random = new Random();
        return (random.nextDouble() * 2) - 1; // от -1% до +1%
    }

    private void initializeFallbackRates() {
        // Fallback курсы на случай недоступности API (курсы к RUB)
        exchangeRates.put("USD", 90.5);
        exchangeRates.put("EUR", 98.2);
        exchangeRates.put("RUB", 1.0);
        exchangeRates.put("CNY", 12.5);
        exchangeRates.put("GBP", 115.0);
        exchangeRates.put("JPY", 0.6);
    }

    private void useFallbackRates() {
        System.out.println("Используем fallback курсы");
        updateDisplayCurrencies();
    }

    private void cacheRates() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Currency currency : currencies) {
            editor.putFloat(currency.getCode() + "_rate", (float) currency.getRate());
        }
        editor.putLong("last_update", System.currentTimeMillis());
        editor.apply();
        System.out.println("Курсы сохранены в кэш");
    }

    private void loadCachedRates() {
        long lastUpdate = prefs.getLong("last_update", 0);
        long currentTime = System.currentTimeMillis();

        // Если данные устарели (больше 2 часов), не загружаем из кэша
        if (currentTime - lastUpdate > 7200000) {
            System.out.println("Кэш устарел");
            return;
        }

        String[] codes = {"USD", "EUR", "CNY"};
        String[] names = {"Доллар США", "Евро", "Китайский юань"};

        for (int i = 0; i < codes.length; i++) {
            float rate = prefs.getFloat(codes[i] + "_rate", 0f);
            if (rate > 0) {
                currencies.add(new Currency(codes[i], names[i], rate, 0));
                System.out.println("Загружен из кэша: " + codes[i] + " = " + rate);
            }
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

        // Правильная формула конвертации через RUB:
        // amount (fromCurrency) → RUB → toCurrency
        double amountInRub = amount * fromRate; // Конвертируем в RUB
        double result = amountInRub / toRate;   // Конвертируем из RUB в целевую валюту

        System.out.println("Конвертация: " + amount + " " + fromCurrency +
                " -> " + amountInRub + " RUB -> " + result + " " + toCurrency);

        return Math.round(result * 100.0) / 100.0; // Округляем до 2 знаков
    }

    public interface CurrencyCallback {
        void onSuccess(List<Currency> currencies);
        void onError(String message);
    }
}