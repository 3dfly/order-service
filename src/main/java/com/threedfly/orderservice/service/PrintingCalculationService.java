package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintingCalculationResponse;
import com.threedfly.orderservice.dto.SlicingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PrintingCalculationService {

    @Value("${printing.price.per-gram}")
    private BigDecimal pricePerGram;

    @Value("${printing.price.per-minute}")
    private BigDecimal pricePerMinute;

    @Value("${printing.bambu.slicer.path}")
    private String bambuSlicerPath;

    @Value("${printing.bambu.printer.config}")
    private String printerConfig;

    @Value("${printing.temp.directory}")
    private String tempDirectory;

    public PrintingCalculationResponse calculatePrice(MultipartFile stlFile) {
        log.info("🧮 Starting price calculation for STL file: {}", stlFile.getOriginalFilename());
        
        try {
            // Validate input file
            if (stlFile.isEmpty() || !isValidStlFile(stlFile)) {
                return createErrorResponse("Invalid STL file provided", stlFile.getOriginalFilename());
            }

            // Save file temporarily
            Path tempFilePath = saveTemporaryFile(stlFile);
            
            try {
                // Process with Bambu slicer
                SlicingResult slicingResult = processWithBambuSlicer(tempFilePath);
                
                if (!slicingResult.isSuccess()) {
                    return createErrorResponse("Slicing failed: " + slicingResult.getErrorMessage(), stlFile.getOriginalFilename());
                }

                // Calculate pricing
                return calculatePricing(slicingResult, stlFile.getOriginalFilename());
                
            } finally {
                // Clean up temporary file
                cleanupTemporaryFile(tempFilePath);
            }
            
        } catch (Exception e) {
            log.error("❌ Error during price calculation", e);
            return createErrorResponse("Calculation failed: " + e.getMessage(), stlFile.getOriginalFilename());
        }
    }

    private boolean isValidStlFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".stl");
    }

    private Path saveTemporaryFile(MultipartFile file) throws IOException {
        // Create temp directory if it doesn't exist
        Path tempDir = Paths.get(tempDirectory);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String tempFilename = timestamp + "_" + originalFilename;
        Path tempFilePath = tempDir.resolve(tempFilename);
        
        // Save file
        file.transferTo(tempFilePath.toFile());
        log.info("📄 Saved temporary STL file: {}", tempFilePath);
        
        return tempFilePath;
    }

    private SlicingResult processWithBambuSlicer(Path stlFilePath) {
        log.info("⚙️ Processing STL file with Bambu slicer: {}", stlFilePath);
        
        try {
            // Prepare output file path
            Path outputDir = stlFilePath.getParent();
            String outputFilename = stlFilePath.getFileName().toString().replace(".stl", "_output.gcode");
            Path outputPath = outputDir.resolve(outputFilename);
            
            // Build command for slicer (supports both Bambu and PrusaSlicer)
            ProcessBuilder processBuilder;
            
            if (bambuSlicerPath.contains("prusa-slicer")) {
                // PrusaSlicer command structure
                processBuilder = new ProcessBuilder(
                    bambuSlicerPath,
                    "--load", printerConfig,
                    "--output", outputPath.toString(),
                    "--export-gcode",
                    "--dont-arrange",
                    stlFilePath.toString()
                );
            } else {
                // Bambu slicer command structure
                processBuilder = new ProcessBuilder(
                    bambuSlicerPath,
                    "--load", printerConfig,
                    "--output", outputPath.toString(),
                    "--export-gcode",
                    stlFilePath.toString()
                );
            }
            
            log.info("🔧 Executing slicer command: {}", String.join(" ", processBuilder.command()));
            
            // Execute slicer
            Process process = processBuilder.start();
            
            // Capture output
            String output = readProcessOutput(process);
            String errors = readProcessErrors(process);
            
            // Wait for completion with timeout
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            
            if (!finished) {
                process.destroyForcibly();
                return SlicingResult.builder()
                    .success(false)
                    .errorMessage("Slicing process timed out after 5 minutes")
                    .build();
            }
            
            int exitCode = process.exitValue();
            log.info("🎯 Bambu slicer completed with exit code: {}", exitCode);
            
            if (exitCode != 0) {
                log.error("❌ Slicer failed with errors: {}", errors);
                return SlicingResult.builder()
                    .success(false)
                    .errorMessage("Slicer failed: " + errors)
                    .build();
            }
            
            // Parse slicer output
            SlicingResult result = parseSlicerOutput(output, outputPath);
            
            // Clean up output file
            try {
                Files.deleteIfExists(outputPath);
            } catch (IOException e) {
                log.warn("⚠️ Could not delete output file: {}", outputPath);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error processing with Bambu slicer", e);
            return SlicingResult.builder()
                .success(false)
                .errorMessage("Processing error: " + e.getMessage())
                .build();
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    private String readProcessErrors(Process process) throws IOException {
        StringBuilder errors = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errors.append(line).append("\n");
            }
        }
        return errors.toString();
    }

    private SlicingResult parseSlicerOutput(String output, Path outputPath) throws IOException {
        log.info("📊 Parsing slicer output for weight and time information");
        
        // Default values
        double weightGrams = 0.0;
        int timeMinutes = 0;
        
        // Patterns to extract information from slicer output
        Pattern weightPattern = Pattern.compile(".*filament used\\s*=\\s*([0-9.]+)g.*", Pattern.CASE_INSENSITIVE);
        Pattern timePattern = Pattern.compile(".*estimated printing time\\s*=\\s*([0-9.]+).*", Pattern.CASE_INSENSITIVE);
        Pattern timeHoursMinutesPattern = Pattern.compile(".*estimated printing time\\s*=\\s*([0-9]+)h\\s*([0-9]+)m.*", Pattern.CASE_INSENSITIVE);
        
        // Parse output lines
        String[] lines = output.split("\n");
        for (String line : lines) {
            // Try to extract weight
            Matcher weightMatcher = weightPattern.matcher(line);
            if (weightMatcher.matches()) {
                weightGrams = Double.parseDouble(weightMatcher.group(1));
                log.info("📏 Found filament weight: {}g", weightGrams);
            }
            
            // Try to extract time (hours and minutes)
            Matcher timeHMatcher = timeHoursMinutesPattern.matcher(line);
            if (timeHMatcher.matches()) {
                int hours = Integer.parseInt(timeHMatcher.group(1));
                int minutes = Integer.parseInt(timeHMatcher.group(2));
                timeMinutes = hours * 60 + minutes;
                log.info("⏱️ Found printing time: {}h {}m ({}min total)", hours, minutes, timeMinutes);
                continue;
            }
            
            // Try to extract time (minutes only)
            Matcher timeMatcher = timePattern.matcher(line);
            if (timeMatcher.matches()) {
                timeMinutes = (int) Math.round(Double.parseDouble(timeMatcher.group(1)));
                log.info("⏱️ Found printing time: {}min", timeMinutes);
            }
        }
        
        // If we couldn't parse from console output, try reading the G-code file
        if ((weightGrams == 0.0 || timeMinutes == 0) && Files.exists(outputPath)) {
            log.info("📄 Attempting to parse G-code file for additional information");
            SlicingResult gcodeResult = parseGCodeFile(outputPath);
            if (gcodeResult.isSuccess()) {
                if (weightGrams == 0.0) weightGrams = gcodeResult.getFilamentWeightGrams();
                if (timeMinutes == 0) timeMinutes = gcodeResult.getEstimatedPrintTimeMinutes();
            }
        }
        
        return SlicingResult.builder()
            .filamentWeightGrams(weightGrams)
            .estimatedPrintTimeMinutes(timeMinutes)
            .success(true)
            .build();
    }

    private SlicingResult parseGCodeFile(Path gcodeFilePath) throws IOException {
        log.info("🔍 Parsing G-code file: {}", gcodeFilePath);
        
        double weightGrams = 0.0;
        int timeMinutes = 0;
        
        List<String> lines = Files.readAllLines(gcodeFilePath);
        
        for (String line : lines) {
            // G-code comments often contain metadata
            if (line.startsWith(";")) {
                // Look for filament weight patterns in comments
                if (line.toLowerCase().contains("filament") && line.toLowerCase().contains("weight")) {
                    Pattern weightPattern = Pattern.compile(".*([0-9.]+)\\s*g.*");
                    Matcher matcher = weightPattern.matcher(line);
                    if (matcher.find()) {
                        weightGrams = Double.parseDouble(matcher.group(1));
                    }
                }
                
                // Look for time patterns in comments
                if (line.toLowerCase().contains("time") || line.toLowerCase().contains("duration")) {
                    Pattern timePattern = Pattern.compile(".*([0-9]+)\\s*m.*");
                    Matcher matcher = timePattern.matcher(line);
                    if (matcher.find()) {
                        timeMinutes = Integer.parseInt(matcher.group(1));
                    }
                }
            }
        }
        
        return SlicingResult.builder()
            .filamentWeightGrams(weightGrams)
            .estimatedPrintTimeMinutes(timeMinutes)
            .success(weightGrams > 0 || timeMinutes > 0)
            .build();
    }

    private PrintingCalculationResponse calculatePricing(SlicingResult slicingResult, String filename) {
        log.info("💰 Calculating pricing - Weight: {}g, Time: {}min", 
            slicingResult.getFilamentWeightGrams(), slicingResult.getEstimatedPrintTimeMinutes());
        
        // Calculate individual costs
        BigDecimal weightCost = pricePerGram.multiply(BigDecimal.valueOf(slicingResult.getFilamentWeightGrams()))
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal timeCost = pricePerMinute.multiply(BigDecimal.valueOf(slicingResult.getEstimatedPrintTimeMinutes()))
            .setScale(2, RoundingMode.HALF_UP);
        
        // Calculate total price
        BigDecimal totalPrice = weightCost.add(timeCost).setScale(2, RoundingMode.HALF_UP);
        
        log.info("🎯 Pricing calculated - Weight cost: ${}, Time cost: ${}, Total: ${}", 
            weightCost, timeCost, totalPrice);
        
        return PrintingCalculationResponse.builder()
            .totalPrice(totalPrice)
            .weightGrams(slicingResult.getFilamentWeightGrams())
            .printingTimeMinutes(slicingResult.getEstimatedPrintTimeMinutes())
            .pricePerGram(pricePerGram)
            .pricePerMinute(pricePerMinute)
            .weightCost(weightCost)
            .timeCost(timeCost)
            .filename(filename)
            .status("SUCCESS")
            .message("Price calculated successfully")
            .build();
    }

    private PrintingCalculationResponse createErrorResponse(String errorMessage, String filename) {
        log.error("❌ Creating error response: {}", errorMessage);
        
        return PrintingCalculationResponse.builder()
            .totalPrice(BigDecimal.ZERO)
            .weightGrams(0.0)
            .printingTimeMinutes(0)
            .pricePerGram(pricePerGram)
            .pricePerMinute(pricePerMinute)
            .weightCost(BigDecimal.ZERO)
            .timeCost(BigDecimal.ZERO)
            .filename(filename)
            .status("ERROR")
            .message(errorMessage)
            .build();
    }

    private void cleanupTemporaryFile(Path tempFilePath) {
        try {
            Files.deleteIfExists(tempFilePath);
            log.info("🧹 Cleaned up temporary file: {}", tempFilePath);
        } catch (IOException e) {
            log.warn("⚠️ Could not delete temporary file: {}", tempFilePath, e);
        }
    }
} 