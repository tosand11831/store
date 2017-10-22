package com.sander.store.currency;

import org.springframework.data.annotation.Id;

/**
 * Describes a currency and its rate compared to EURO
 */
public class CurrencyRate {

    @Id
    private String languageIso;
    private double rate;

    public CurrencyRate(String languageIso, double rate) {
        this.languageIso = languageIso;
        this.rate = rate;
    }

    public String getLanguageIso() {
        return languageIso;
    }

    public double getRate() {
        return rate;
    }
}
