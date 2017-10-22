package com.sander.store.exceptions;

public class ProductUpdateException extends Exception {
    public ProductUpdateException(String field) {
        super("Could not update field " + field);
    }
}
