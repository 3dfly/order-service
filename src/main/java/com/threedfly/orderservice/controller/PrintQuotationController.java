package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.dto.PrintQuotationRequest;
import com.threedfly.orderservice.dto.PrintQuotationResponse;
import com.threedfly.orderservice.exception.FileParseException;
import com.threedfly.orderservice.exception.InvalidFileTypeException;
import com.threedfly.orderservice.exception.InvalidParameterCombinationException;
import com.threedfly.orderservice.service.PrintQuotationService;
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
public class PrintQuotationController {

    private final PrintQuotationService quotationService;

    @PostMapping("/quotation")
    public ResponseEntity<PrintQuotationResponse> calculateQuotation(
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute PrintQuotationRequest request) {

        log.info("💰 POST /api/print/quotation - file: {}, tech: {}, material: {}",
                file.getOriginalFilename(), request.getTechnology(), request.getMaterial());

        try {
            PrintQuotationResponse response = quotationService.calculateQuotation(file, request);
            log.info("✅ Quotation calculated successfully: ${}", response.getEstimatedPrice());
            return ResponseEntity.ok(response);
        } catch (InvalidFileTypeException | InvalidParameterCombinationException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            throw e;
        } catch (FileParseException e) {
            log.error("❌ File parse error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error calculating quotation", e);
            throw e;
        }
    }
}
