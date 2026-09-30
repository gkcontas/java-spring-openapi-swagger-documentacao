package com.gkcontas.openapi.error;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("No product with id %d exists in the catalogue.".formatted(id));
    }
}
