package com.threedfly.orderservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaterialCombinationValidator.class)
public @interface ValidMaterialCombination {
    String message() default "Invalid technology-material combination";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
