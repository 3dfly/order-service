package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.dto.PrintCalculationRequest;
import com.threedfly.orderservice.dto.PrintCalculationResponse;
import com.threedfly.orderservice.exception.FileParseException;
import com.threedfly.orderservice.exception.InvalidFileTypeException;
import com.threedfly.orderservice.exception.InvalidParameterCombinationException;
import com.threedfly.orderservice.service.PrintCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/print")
@RequiredArgsConstructor
@Slf4j
public class PrintCalculationController {

    private final PrintCalculationService calculationService;

    @PostMapping("/calculate")
    public ResponseEntity<PrintCalculationResponse> calculatePrice(
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute PrintCalculationRequest request) {

        log.info("💰 POST /api/print/calculate - file: {}, tech: {}, material: {}",
                file.getOriginalFilename(), request.getTechnology(), request.getMaterial());

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
}
