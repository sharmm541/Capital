package com.Taviak.capital.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class CurrencyResponse {
    @SerializedName("base")
    private String base;

    @SerializedName("rates")
    private Map<String, Double> rates;

    @SerializedName("date")
    private String date;

    public String getBase() { return base; }
    public Map<String, Double> getRates() { return rates; }
    public String getDate() { return date; }
}