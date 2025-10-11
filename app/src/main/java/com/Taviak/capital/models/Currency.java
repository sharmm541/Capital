package com.Taviak.capital.models;

public class Currency {
    private String code;
    private String name;
    private double rate;
    private double change;
    private boolean positiveChange;

    public Currency(String code, String name, double rate, double change) {
        this.code = code;
        this.name = name;
        this.rate = rate;
        this.change = change;
        this.positiveChange = change >= 0;
    }

    // Геттеры
    public String getCode() { return code; }
    public String getName() { return name; }
    public double getRate() { return rate; }
    public double getChange() { return change; }
    public boolean isPositiveChange() { return positiveChange; }
}