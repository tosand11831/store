package com.sander.store.currency;

import com.sander.store.exceptions.CurrencyISONotFoundException;
import com.sander.store.pojo.Product;
import com.sander.store.repository.CurrencyRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JacksonJsonParser;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class loads currency information from http://api.fixer.io/latest and saves it
 * to {@link CurrencyRepository}, also does currency conversion
 */
public class CurrencyConversionProvider {

    public static String CURRENCY_ISO = "currencyIso", VALUE = "value", EUR = "EUR";

    private final Logger LOGGER = LoggerFactory.getLogger(CurrencyConversionProvider.class);
    private final String CURRENCIES_REQUEST = "http://api.fixer.io/latest";
    private final JacksonJsonParser parser = new JacksonJsonParser();
    private final CurrencyRepository currencyRepository;

    public CurrencyConversionProvider(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public boolean updateCurrencies() {

        String json = null;
        URL url = null;

        try {
            url = new URL(CURRENCIES_REQUEST);
            json = IOUtils.toString(url, "UTF-8");
        } catch (IOException ex) {
            LOGGER.error("Could not access " + url, ex);
        }

        if (json == null) {
            if (currencyRepository.findAll().isEmpty())
                LOGGER.warn("CurrencyRate conversion cannot be applied");
            return false;
        }

        try {
            currencyRepository.deleteAll();
            Map<String, Object> data = parser.parseMap(json);
            Map<String, Object> rates = (Map<String, Object>) data.get("rates");
            currencyRepository.save(
                    rates.entrySet().stream().map(
                            e -> new CurrencyRate(e.getKey(), (double)e.getValue())
                    ).collect(Collectors.toList()));
        } catch (Exception ex) {
            LOGGER.error("Could not update currencies", ex);
            return false;
        }

        return true;
    }

    public double convertFromEuroTo(double value, String to) throws CurrencyISONotFoundException {
        if(to.equalsIgnoreCase("EUR"))
            return value;
        CurrencyRate currencyRate = currencyRepository.findOne(to);
        if(currencyRate == null)
            throw new CurrencyISONotFoundException("CurrencyRate ISO not available.");
        return value * currencyRate.getRate();
    }

    public double convertToEuro(double value, String from) throws CurrencyISONotFoundException {
        CurrencyRate currencyRate = currencyRepository.findOne(from);
        if(currencyRate == null)
            throw new CurrencyISONotFoundException("CurrencyRate ISO not available.");
        return value / currencyRate.getRate();
    }

    /**
     * Adapt currency on a given {@link Product}
     *
     * @param currencyIso
     * @param product
     */
    public void adaptCurrency(String currencyIso, Product product)  {

        try {
            if (currencyIso.equalsIgnoreCase(product.getCurrencyIso())) {
                return;
            }

            if (currencyIso.equalsIgnoreCase(EUR) && !product.getCurrencyIso().equalsIgnoreCase(EUR)) {
                double newValue = convertToEuro(product.getValue(), product.getCurrencyIso());
                product.setValue(newValue);
                product.setCurrencyIso(currencyIso);
                return;
            }

            if (!currencyIso.equalsIgnoreCase(EUR) && product.getCurrencyIso().equalsIgnoreCase(EUR)) {
                double newValue = convertFromEuroTo(product.getValue(), currencyIso);
                product.setValue(newValue);
                product.setCurrencyIso(currencyIso);
                return;
            }

            if (!currencyIso.equalsIgnoreCase(EUR) && !product.getCurrencyIso().equalsIgnoreCase(EUR)) {
                double newValue = convertFromEuroTo(convertToEuro(product.getValue(), product.getCurrencyIso()), currencyIso);
                product.setValue(newValue);
                product.setCurrencyIso(currencyIso);
                return;
            }
        } catch (Exception ex) {
            LOGGER.error("Could not convert product currency into " + currencyIso, ex);
            return;
        }
     }
}
