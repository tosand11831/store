package com.sander.store.currency;

import com.sander.store.StoreApplication;
import com.sander.store.exceptions.CurrencyISONotFoundException;
import com.sander.store.pojo.Product;
import com.sander.store.repository.CurrencyRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StoreApplication.class)
@WebAppConfiguration
public class CurrencyRateConversionProviderTest {

    @Autowired
    private CurrencyRepository currencyRepository;
    private CurrencyConversionProvider currencyConversionProvider;

    @Before
    public void setup() {
        currencyConversionProvider = new CurrencyConversionProvider(currencyRepository);
    }

    @Test
    public void shouldCalculateInDifferentCurrencies() throws CurrencyISONotFoundException {
        init();
        assertTrue(currencyConversionProvider.convertFromEuroTo(1.0, "AUD") != 0.0);
    }

    @Test(expected = CurrencyISONotFoundException.class)
    public void shouldFailIfNoConversionIsPossible() throws CurrencyISONotFoundException {
        currencyRepository.deleteAll();
        currencyConversionProvider.convertFromEuroTo(2.0, "jcvjdnfjnv");
    }

    @Test
    public void shouldUpdateCurrencyRepository() throws CurrencyISONotFoundException {
        assertTrue(currencyConversionProvider.updateCurrencies());
        assertFalse(currencyRepository.findAll().isEmpty());
        assertTrue(currencyConversionProvider.convertFromEuroTo(1.0, "USD") != 1.5033);
    }

    @Test
    public void shouldAdaptCurrencies() {
        init();

        // product saved in EURO
        Product product = Product.newProduct("p1", 1.0, "EUR");

        // no conversion needed
        currencyConversionProvider.adaptCurrency("EUR", product);
        assertEquals(1.0, product.getValue(), 0.0);

        // adapt currency from EUR to USD
        currencyConversionProvider.adaptCurrency("USD", product);
        assertEquals(1.1834, product.getValue(), 0.0);
        assertEquals("USD", product.getCurrencyIso());

        // convert BGN to EUR
        product = Product.newProduct("p2", 1.0, "BGN");
        currencyConversionProvider.adaptCurrency("EUR", product);
        assertEquals(0.526, product.getValue(), 0.001);
        assertEquals("EUR", product.getCurrencyIso());

        // convert USD to BGN
        product = Product.newProduct("p3", 99.99, "USD");
        currencyConversionProvider.adaptCurrency("BGN", product);
        assertEquals(160.53827953354738, product.getValue(), 0.0);
        assertEquals("BGN", product.getCurrencyIso());
    }

    private void init() {

        currencyRepository.deleteAll();

        List<CurrencyRate> currencies = Arrays.asList(
                new CurrencyRate("AUD", 1.5033),
                new CurrencyRate("BGN", 1.90),
                new CurrencyRate("USD", 1.1834),
                new CurrencyRate("PHP", 60.345)
        );

        currencyRepository.save(currencies);
    }
}
