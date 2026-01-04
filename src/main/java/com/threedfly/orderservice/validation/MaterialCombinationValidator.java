package com.threedfly.orderservice.validation;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.exception.InvalidParameterCombinationException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class MaterialCombinationValidator
        implements ConstraintValidator<ValidMaterialCombination, PrintCalculationRequest> {

    // Valid combinations matrix
    private static final Map<String, Set<String>> VALID_COMBINATIONS = Map.of(
            "FDM", Set.of("PLA", "ABS", "PETG", "TPU"),
            "SLS", Set.of("PLA", "ABS", "PETG"),
            "SLA", Set.of("PLA", "ABS")
    );

    @Override
    public boolean isValid(PrintCalculationRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getTechnology() == null || request.getMaterial() == null) {
            return true; // Let @NotBlank handle null checks
        }

        Set<String> validMaterials = VALID_COMBINATIONS.get(request.getTechnology());
        if (validMaterials == null || !validMaterials.contains(request.getMaterial())) {
            log.warn("⚠️ Invalid combination: {} with {}", request.getTechnology(), request.getMaterial());

            // Only modify context if it's not null (for proper validation)
            if (context != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                String.format("Material %s is not compatible with technology %s",
                                        request.getMaterial(), request.getTechnology()))
                        .addConstraintViolation();
            }
            return false;
        }

        return true;
    }

    public void validate(String technology, String material) {
        // Programmatic validation for service layer
        Set<String> validMaterials = VALID_COMBINATIONS.get(technology);
        if (validMaterials == null || !validMaterials.contains(material)) {
            throw new InvalidParameterCombinationException(
                    String.format("Material %s is not compatible with technology %s", material, technology));
        }
    }
}
