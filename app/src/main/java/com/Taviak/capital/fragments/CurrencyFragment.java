package com.Taviak.capital.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.Taviak.capital.R;
import com.Taviak.capital.managers.CurrencyManager;
import com.Taviak.capital.models.Currency;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CurrencyFragment extends Fragment {

    private TextView usdRate, eurRate, cnyRate;
    private TextView usdChange, eurChange, cnyChange;
    private TextView lastUpdateText;
    private Spinner currencyFromSpinner, currencyToSpinner;
    private TextView conversionResult;
    private EditText amountInput;
    private CurrencyManager currencyManager;

    private String[] currencyNames = {
            "Выберите валюту",  // disabled элемент
            "Российский рубль (RUB)",
            "Доллар США (USD)",
            "Евро (EUR)",
            "Китайский юань (CNY)",
            "Фунт стерлингов (GBP)",
            "Японская иена (JPY)"
    };

    private String[] currencyCodes = {
            "",      // пустой код для первого элемента
            "RUB", "USD", "EUR", "CNY", "GBP", "JPY"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency, container, false);

        initViews(view);
        setupCurrencyManager();
        setupSpinners();

        // Сначала показываем кэшированные данные
        displayCurrencyRates(currencyManager.getCurrencies());
        updateLastUpdateTime();

        // Затем пытаемся обновить из API
        loadCurrencyData();

        return view;
    }

    private void initViews(View view) {
        usdRate = view.findViewById(R.id.usdRate);
        eurRate = view.findViewById(R.id.eurRate);
        cnyRate = view.findViewById(R.id.cnyRate);
        usdChange = view.findViewById(R.id.usdChange);
        eurChange = view.findViewById(R.id.eurChange);
        cnyChange = view.findViewById(R.id.cnyChange);

        currencyFromSpinner = view.findViewById(R.id.currencyFromSpinner);
        currencyToSpinner = view.findViewById(R.id.currencyToSpinner);
        conversionResult = view.findViewById(R.id.conversionResult);
        amountInput = view.findViewById(R.id.amountInput);

        // Добавьте этот TextView в ваш layout
        lastUpdateText = view.findViewById(R.id.lastUpdateText);

        view.findViewById(R.id.convertButton).setOnClickListener(v -> onConvertButtonClick());

        // Кнопка для ручного обновления курсов
        view.findViewById(R.id.convertButton).setOnLongClickListener(v -> {
            refreshRates();
            return true;
        });
    }

    private void setupCurrencyManager() {
        currencyManager = new CurrencyManager(requireContext());
    }

    private void setupSpinners() {
        // Настройка Spinner для валюты "из"
        ArrayAdapter<String> fromAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, currencyNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }

                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }
        };
        fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencyFromSpinner.setAdapter(fromAdapter);

        // Настройка Spinner для валюты "в"
        ArrayAdapter<String> toAdapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_spinner_item, currencyNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.text_main_secondary));
                } else {
                    textView.setTextColor(getResources().getColor(R.color.text_primary));
                }

                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }
        };
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencyToSpinner.setAdapter(toAdapter);

        // Устанавливаем начальные значения (пропускаем первый элемент, так как он disabled)
        currencyFromSpinner.setSelection(1); // USD (второй элемент)
        currencyToSpinner.setSelection(2);   // EUR (третий элемент)

        // Слушатель изменений для автоматической конвертации
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!TextUtils.isEmpty(amountInput.getText().toString()) && position != 0) {
                    onConvertButtonClick();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        currencyFromSpinner.setOnItemSelectedListener(spinnerListener);
        currencyToSpinner.setOnItemSelectedListener(spinnerListener);
    }

    private void loadCurrencyData() {
        currencyManager.fetchExchangeRates(new CurrencyManager.CurrencyCallback() {
            @Override
            public void onSuccess(List<Currency> currencies) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        displayCurrencyRates(currencies);
                        updateLastUpdateTime();
                        Toast.makeText(requireContext(), "Курсы обновлены", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), message + ". Используются кэшированные данные.", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void refreshRates() {
        Toast.makeText(requireContext(), "Обновление курсов...", Toast.LENGTH_SHORT).show();
        loadCurrencyData();
    }

    private void displayCurrencyRates(List<Currency> currencies) {
        if (currencies.isEmpty()) {
            usdRate.setText("—");
            eurRate.setText("—");
            cnyRate.setText("—");
            usdChange.setText("—");
            eurChange.setText("—");
            cnyChange.setText("—");
            return;
        }

        for (Currency currency : currencies) {
            switch (currency.getCode()) {
                case "USD":
                    usdRate.setText(String.format(Locale.US, "%.2f ₽", currency.getRate()));
                    usdChange.setText(String.format(Locale.US, "%.1f%%", currency.getChange()));
                    usdChange.setTextColor(getColor(currency.isPositiveChange() ?
                            R.color.status_success : R.color.status_error));
                    break;
                case "EUR":
                    eurRate.setText(String.format(Locale.US, "%.2f ₽", currency.getRate()));
                    eurChange.setText(String.format(Locale.US, "%.1f%%", currency.getChange()));
                    eurChange.setTextColor(getColor(currency.isPositiveChange() ?
                            R.color.status_success : R.color.status_error));
                    break;
                case "CNY":
                    cnyRate.setText(String.format(Locale.US, "%.2f ₽", currency.getRate()));
                    cnyChange.setText(String.format(Locale.US, "%.1f%%", currency.getChange()));
                    cnyChange.setTextColor(getColor(currency.isPositiveChange() ?
                            R.color.status_success : R.color.status_error));
                    break;
            }
        }
    }

    private void updateLastUpdateTime() {
        if (lastUpdateText != null) {
            String time = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
                    .format(new Date());
            lastUpdateText.setText("Обновлено: " + time);
        }
    }

    private int getColor(int colorRes) {
        return requireContext().getColor(colorRes);
    }

    private void onConvertButtonClick() {
        String amountStr = amountInput.getText().toString();
        if (TextUtils.isEmpty(amountStr)) {
            conversionResult.setText("Введите сумму");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                conversionResult.setText("Сумма должна быть больше 0");
                return;
            }

            int fromPosition = currencyFromSpinner.getSelectedItemPosition();
            int toPosition = currencyToSpinner.getSelectedItemPosition();

            // Проверяем что выбраны не заголовки
            if (fromPosition == 0 || toPosition == 0) {
                conversionResult.setText("Выберите валюты");
                return;
            }

            if (fromPosition == toPosition) {
                conversionResult.setText("Выберите разные валюты");
                return;
            }

            String fromCurrency = currencyCodes[fromPosition];
            String toCurrency = currencyCodes[toPosition];

            double result = currencyManager.convertCurrency(amount, fromCurrency, toCurrency);

            if (result > 0) {
                conversionResult.setText(String.format("%s %s = %s %s",
                        formatNumber(amount), fromCurrency,
                        formatNumber(result), toCurrency));
            } else {
                conversionResult.setText("Ошибка конвертации");
            }

        } catch (NumberFormatException e) {
            conversionResult.setText("Неверный формат числа");
        }
    }

    private String formatNumber(double number) {
        if (number == (long) number) {
            return String.format(Locale.US, "%d", (long) number);
        } else {
            return String.format(Locale.US, "%.2f", number);
        }
    }
}