package com.gkcontas.openapi.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * The internal model, deliberately separate from anything a client sees.
 *
 * <p>Serving entities directly is what makes an API contract impossible to keep: a field
 * renamed for internal reasons becomes a breaking change, and v1 and v2 can no longer
 * differ because they are the same class. Every response here is mapped from this.
 */
public record Product(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        String currency,
        List<String> categories,
        ProductKind kind,
        Double weightKg,
        String downloadUrl,
        boolean discontinued) {
}
