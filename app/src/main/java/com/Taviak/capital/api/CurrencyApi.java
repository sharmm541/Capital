package com.Taviak.capital.api;

import com.Taviak.capital.models.CurrencyResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CurrencyApi {
    // Используем API, который поддерживает RUB как базовую валюту
    @GET("latest")
    Call<CurrencyResponse> getExchangeRates(@Query("base") String baseCurrency);
}