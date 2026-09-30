package com.gkcontas.openapi.validation;

import com.gkcontas.openapi.api.admin.CreateProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConsistentDeliveryDetailValidator
        implements ConstraintValidator<ConsistentDeliveryDetail, CreateProductRequest> {

    @Override
    public boolean isValid(CreateProductRequest request, ConstraintValidatorContext context) {
        if (request == null || request.kind() == null) {
            // @NotNull on kind already reports this. Reporting it twice would put two
            // violations on the caller's screen for one mistake.
            return true;
        }
        return switch (request.kind()) {
            case PHYSICAL -> request.weightKg() != null;
            case DIGITAL -> request.downloadUrl() != null && !request.downloadUrl().isBlank();
        };
    }
}
