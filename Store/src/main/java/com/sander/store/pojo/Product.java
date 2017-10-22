package com.sander.store.pojo;

import com.sander.store.currency.CurrencyConversionProvider;
import com.sander.store.exceptions.ProductUpdateException;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.Map;

/**
 * Describes a simple product
 */
public class Product {

    private static final String NAME = "name";

    @Id
    private String id;

    @Version
    private Long version;

    private String name, currencyIso;
    private double value;

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setCurrencyIso(String currencyIso) {
        this.currencyIso = currencyIso;
    }

    public String getId() { return id; }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public static Product newProduct(String name, double value, String currencyIso) {
        Product product = new Product();
        product.setName(name);
        product.setValue(value);
        product.setCurrencyIso(currencyIso);
        return product;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", value=" + value +
                ", currencyIso='" + currencyIso + '\'' +
                '}';
    }

    public void update(Map<String, Object> dataMap) throws ProductUpdateException {

        if(dataMap.containsKey(NAME)) {
            String name = (String) dataMap.get(NAME);
            if(name != null)
                setName(name);
            else
                throw new ProductUpdateException(NAME);
        }

        if(dataMap.containsKey(CurrencyConversionProvider.VALUE)) {
            double value = (double) dataMap.get(CurrencyConversionProvider.VALUE);
            if(value > 0)
                setValue(value);
            else
                throw new ProductUpdateException(CurrencyConversionProvider.VALUE);
        }

        // TODO can be extended so that only currency can be updated and value is adapted automatically
    }
}
