package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.dto.PrintCalculationResponse;
import com.threedfly.orderservice.exception.FileParseException;
import com.threedfly.orderservice.exception.InvalidFileTypeException;
import com.threedfly.orderservice.exception.InvalidParameterCombinationException;
import com.threedfly.orderservice.exception.ValidationException;
import com.threedfly.orderservice.service.PrintCalculationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/print")
@RequiredArgsConstructor
@Slf4j
public class PrintCalculationController {

    private final PrintCalculationService calculationService;
    private final Validator validator;

    @PostMapping("/calculate")
    public ResponseEntity<PrintCalculationResponse> calculatePrice(
            @RequestPart("file") MultipartFile file,
            @ModelAttribute PrintCalculationRequest request) {

        String tech = (request != null) ? request.getTechnology() : "extracted from file";
        String material = (request != null) ? request.getMaterial() : "extracted from file";
        log.info("💰 POST /api/print/calculate - file: {}, tech: {}, material: {}",
                file.getOriginalFilename(), tech, material);

        // Validate request parameters only if they're provided (STL/OBJ files)
        // For 3MF files, request will be empty and validation is skipped
        if (request != null && isRequestPopulated(request)) {
            Set<ConstraintViolation<PrintCalculationRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new ValidationException("Request validation failed", violations);
            }
        }

        try {
            PrintCalculationResponse response = calculationService.calculatePrice(file, request);
            log.info("✅ Calculation completed successfully: ${}", response.getEstimatedPrice());
            return ResponseEntity.ok(response);
        } catch (InvalidFileTypeException | InvalidParameterCombinationException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            throw e;
        } catch (FileParseException e) {
            log.error("❌ File parse error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error calculating price", e);
            throw e;
        }
    }

    /**
     * Checks if the request has any populated fields (indicating STL/OBJ file).
     * For 3MF files, Spring creates an empty object with all null fields.
     */
    private boolean isRequestPopulated(PrintCalculationRequest request) {
        return request.getTechnology() != null ||
               request.getMaterial() != null ||
               request.getLayerHeight() != null ||
               request.getShells() != null ||
               request.getInfill() != null ||
               request.getSupporters() != null;
    }
}
