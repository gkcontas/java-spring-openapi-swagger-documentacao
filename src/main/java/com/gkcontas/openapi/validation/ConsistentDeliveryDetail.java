package com.gkcontas.openapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A rule that spans two fields, so it is declared on the type rather than on a field.
 *
 * <p>The obvious alternative is {@code @AssertTrue} on a helper method, and it works —
 * but it costs twice. Bean Validation needs the method to look like a getter, and Jackson
 * reports getters as properties, so the generated schema grows a phantom
 * {@code deliveryDetailValid} boolean that no caller can send and every generated client
 * offers as a settable field (unless it is also annotated {@code @JsonIgnore} and
 * {@code @Schema(hidden = true)}). And the violation is attached to that pseudo-property,
 * so the error message ends up prefixed with the getter's name.
 *
 * <p>A type-level constraint has neither problem: no accessor, no property in the schema,
 * and the violation is a global error whose message stands on its own.
 *
 * <p>What neither approach fixes is the schema itself. "Required when another field has a
 * particular value" is expressible in JSON Schema with {@code if}/{@code then}, but no
 * annotation generates it and most client generators ignore it. So the rule is stated in
 * the operation description, where a human reads it, and enforced here.
 */
@Documented
@Constraint(validatedBy = ConsistentDeliveryDetailValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsistentDeliveryDetail {

    String message() default
            "weightKg is required when kind is PHYSICAL, and downloadUrl when kind is DIGITAL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
