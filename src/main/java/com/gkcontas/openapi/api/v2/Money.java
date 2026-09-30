package com.gkcontas.openapi.api.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "Money", description = "An amount together with the currency it is expressed in")
public record Money(

        @Schema(description = "Decimal amount", example = "450.00")
        BigDecimal amount,

        @Schema(description = "ISO 4217 code", example = "BRL", pattern = "^[A-Z]{3}$")
        String currency) {
}
