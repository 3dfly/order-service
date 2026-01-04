package com.threedfly.orderservice.validation;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.exception.InvalidParameterCombinationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaterialCombinationValidatorTest {

    private MaterialCombinationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MaterialCombinationValidator();
    }

    /**
     * Valid combinations:
     * FDM: PLA, ABS, PETG, TPU
     * SLS: PLA, ABS, PETG
     * SLA: PLA, ABS
     */
    static Stream<Arguments> validCombinations() {
        return Stream.of(
                // FDM with all materials
                Arguments.of("FDM", "PLA"),
                Arguments.of("FDM", "ABS"),
                Arguments.of("FDM", "PETG"),
                Arguments.of("FDM", "TPU"),

                // SLS with valid materials
                Arguments.of("SLS", "PLA"),
                Arguments.of("SLS", "ABS"),
                Arguments.of("SLS", "PETG"),

                // SLA with valid materials
                Arguments.of("SLA", "PLA"),
                Arguments.of("SLA", "ABS")
        );
    }

    /**
     * Invalid combinations:
     * SLS: TPU not allowed
     * SLA: PETG, TPU not allowed
     */
    static Stream<Arguments> invalidCombinations() {
        return Stream.of(
                Arguments.of("SLS", "TPU"),
                Arguments.of("SLA", "PETG"),
                Arguments.of("SLA", "TPU")
        );
    }

    @ParameterizedTest(name = "Valid: {0} + {1}")
    @MethodSource("validCombinations")
    void testValidate_ValidCombinations(String technology, String material) {
        assertThatNoException().isThrownBy(() ->
                validator.validate(technology, material)
        );
    }

    @ParameterizedTest(name = "Invalid: {0} + {1}")
    @MethodSource("invalidCombinations")
    void testValidate_InvalidCombinations(String technology, String material) {
        assertThatThrownBy(() ->
                validator.validate(technology, material)
        ).isInstanceOf(InvalidParameterCombinationException.class)
                .hasMessageContaining("not compatible");
    }

    @ParameterizedTest(name = "ConstraintValidator Valid: {0} + {1}")
    @MethodSource("validCombinations")
    void testConstraintValidator_ValidCombinations(String technology, String material) {
        PrintCalculationRequest request = new PrintCalculationRequest();
        request.setTechnology(technology);
        request.setMaterial(material);
        request.setLayerHeight(0.2);
        request.setShells(2);
        request.setInfill(15);
        request.setSupporters(true);

        boolean result = validator.isValid(request, null);

        assertThat(result).isTrue();
    }

    @ParameterizedTest(name = "ConstraintValidator Invalid: {0} + {1}")
    @MethodSource("invalidCombinations")
    void testConstraintValidator_InvalidCombinations(String technology, String material) {
        PrintCalculationRequest request = new PrintCalculationRequest();
        request.setTechnology(technology);
        request.setMaterial(material);
        request.setLayerHeight(0.2);
        request.setShells(2);
        request.setInfill(15);
        request.setSupporters(true);

        // Note: We can't test ConstraintValidatorContext properly in unit test
        // But we can verify the logic returns false
        boolean result = validator.isValid(request, null);

        assertThat(result).isFalse();
    }
}
