package com.threedfly.orderservice.service;

import com.threedfly.orderservice.config.PrintingPricingConfig;
import com.threedfly.orderservice.dto.PrintQuotationRequest;
import com.threedfly.orderservice.dto.PrintQuotationResponse;
import com.threedfly.orderservice.dto.SlicingResult;
import com.threedfly.orderservice.entity.ModelFileType;
import com.threedfly.orderservice.exception.FileParseException;
import com.threedfly.orderservice.exception.InvalidFileTypeException;
import com.threedfly.orderservice.service.slicer.SlicerService;
import com.threedfly.orderservice.service.slicer.SlicerServiceFactory;
import com.threedfly.orderservice.validation.MaterialCombinationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
@RequiredArgsConstructor
public class PrintQuotationService {

    private final IniConfigurationMapper iniConfigurationMapper;
    private final PrintingPricingConfig pricingConfig;
    private final MaterialCombinationValidator materialValidator;
    private final SlicerServiceFactory slicerServiceFactory;

    @Value("${printing.slicer.type}")
    private String slicerType;

    @Value("${printing.temp.directory}")
    private String tempDirectory;

    @Transactional(readOnly = true)
    public PrintQuotationResponse calculateQuotation(MultipartFile file, PrintQuotationRequest request) {
        log.info("📐 Starting quotation calculation for file: {}", file.getOriginalFilename());

        // 1. Validate file type
        ModelFileType fileType = validateAndDetectFileType(file);

        // 2. Validate technology-material combination
        materialValidator.validate(request.getTechnology(), request.getMaterial());

        // 3. Get appropriate INI configuration
        String iniFile = iniConfigurationMapper.getConfigurationFile(
                request.getTechnology(),
                request.getMaterial(),
                request.getLayerHeight(),
                request.getSupporters()
        );
        Path iniPath = iniConfigurationMapper.getConfigurationPath(iniFile);

        log.info("🔧 Using INI configuration: {}", iniFile);

        // 4. Save file temporarily
        Path tempFilePath = null;
        try {
            tempFilePath = saveTemporaryFile(file);

            // 5. Process with slicer
            SlicingResult slicingResult = processWithSlicer(
                    tempFilePath,
                    iniPath,
                    request.getLayerHeight(),
                    request.getShells(),
                    request.getInfill(),
                    request.getSupporters()
            );

            if (!slicingResult.isSuccess()) {
                throw new FileParseException("Slicing failed: " + slicingResult.getErrorMessage());
            }

            // 6. Calculate pricing
            return calculatePricing(slicingResult, file.getOriginalFilename(), request);

        } catch (IOException e) {
            throw new FileParseException("File processing failed: " + e.getMessage(), e);
        } finally {
            // Clean up temporary file
            if (tempFilePath != null) {
                cleanupTemporaryFile(tempFilePath);
            }
        }
    }

    private ModelFileType validateAndDetectFileType(MultipartFile file) {
        // Validate size
        if (file.isEmpty()) {
            throw new InvalidFileTypeException("File is empty");
        }

        // Validate filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileTypeException("Invalid filename");
        }

        // Detect file type by extension
        ModelFileType fileType = ModelFileType.fromFilename(filename);

        log.info("✅ File validated: {} (type: {})", filename, fileType);
        return fileType;
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
        log.info("📄 Saved temporary file: {}", tempFilePath);

        return tempFilePath;
    }

    private SlicingResult processWithSlicer(Path modelFilePath, Path iniPath,
                                             Double layerHeight, Integer shells,
                                             Integer infill, Boolean supporters) {
        log.info("⚙️ Processing file with slicer: {}", modelFilePath);
        log.info("📊 Parameters - layerHeight: {}, shells: {}, infill: {}%, supporters: {}",
                layerHeight, shells, infill, supporters);

        try {
            // Validate paths to prevent command injection
            validatePathSafety(modelFilePath, "model file");
            validatePathSafety(iniPath, "configuration file");

            // Validate and sanitize numeric parameters to prevent command injection
            validateNumericParameter(layerHeight, 0.05, 0.4, "layer height");
            validateNumericParameter(shells.doubleValue(), 1.0, 5.0, "shells");
            validateNumericParameter(infill.doubleValue(), 5.0, 20.0, "infill");

            // Validate boolean parameter
            if (supporters == null) {
                throw new IllegalArgumentException("Supporters parameter cannot be null");
            }

            // Prepare output file path
            Path outputDir = modelFilePath.getParent();
            String outputFilename = modelFilePath.getFileName().toString()
                    .replaceAll("\\.(stl|obj|3mf)$", "_output.gcode");
            Path outputPath = outputDir.resolve(outputFilename);

            // Get appropriate slicer implementation and build command
            SlicerService slicerService = slicerServiceFactory.getSlicer(slicerType);
            log.info("🔧 Using slicer: {}", slicerService.getSlicerName());

            ProcessBuilder processBuilder = slicerService.buildSlicerCommand(
                    modelFilePath,
                    iniPath,
                    outputPath,
                    layerHeight,
                    shells,
                    infill,
                    supporters
            );

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
            log.info("🎯 Slicer completed with exit code: {}", exitCode);

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
            log.error("❌ Error processing with slicer", e);
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
        // PrusaSlicer format: "; total filament used [g] = 0.67"
        Pattern weightPattern = Pattern.compile(".*total filament used.*\\[g\\]\\s*=\\s*([0-9.]+).*", Pattern.CASE_INSENSITIVE);
        // Also support older format: "filament used = 0.67g"
        Pattern weightPatternLegacy = Pattern.compile(".*filament used\\s*=\\s*([0-9.]+)\\s*g.*", Pattern.CASE_INSENSITIVE);
        // PrusaSlicer format: "; estimated printing time (normal mode) = 5m 33s"
        Pattern timePattern = Pattern.compile(".*estimated printing time.*=\\s*([0-9]+)m\\s*([0-9]+)s.*", Pattern.CASE_INSENSITIVE);
        Pattern timeHoursMinutesPattern = Pattern.compile(".*estimated printing time.*=\\s*([0-9]+)h\\s*([0-9]+)m.*", Pattern.CASE_INSENSITIVE);

        // Parse output lines
        String[] lines = output.split("\n");
        for (String line : lines) {
            // Try to extract weight (PrusaSlicer format)
            Matcher weightMatcher = weightPattern.matcher(line);
            if (weightMatcher.matches()) {
                weightGrams = Double.parseDouble(weightMatcher.group(1));
                log.info("📏 Found filament weight: {}g (PrusaSlicer format)", weightGrams);
            }

            // Try legacy weight format
            if (weightGrams == 0.0) {
                Matcher weightMatcherLegacy = weightPatternLegacy.matcher(line);
                if (weightMatcherLegacy.matches()) {
                    weightGrams = Double.parseDouble(weightMatcherLegacy.group(1));
                    log.info("📏 Found filament weight: {}g (legacy format)", weightGrams);
                }
            }

            // Try to extract time (hours and minutes): "1h 23m"
            Matcher timeHMatcher = timeHoursMinutesPattern.matcher(line);
            if (timeHMatcher.matches()) {
                int hours = Integer.parseInt(timeHMatcher.group(1));
                int minutes = Integer.parseInt(timeHMatcher.group(2));
                timeMinutes = hours * 60 + minutes;
                log.info("⏱️ Found printing time: {}h {}m ({}min total)", hours, minutes, timeMinutes);
                continue;
            }

            // Try to extract time (minutes and seconds): "5m 33s"
            Matcher timeMatcher = timePattern.matcher(line);
            if (timeMatcher.matches()) {
                int minutes = Integer.parseInt(timeMatcher.group(1));
                int seconds = Integer.parseInt(timeMatcher.group(2));
                timeMinutes = minutes + (seconds >= 30 ? 1 : 0); // Round up if >= 30 seconds
                log.info("⏱️ Found printing time: {}m {}s ({}min total)", minutes, seconds, timeMinutes);
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
        log.debug("📄 G-code file has {} lines", lines.size());

        for (String line : lines) {
            // G-code comments often contain metadata
            if (line.startsWith(";")) {
                // Look for PrusaSlicer filament weight: "; total filament used [g] = 0.67"
                // Exclude wipe tower lines
                if (line.toLowerCase().contains("total filament used") && !line.toLowerCase().contains("wipe tower")) {
                    log.debug("🔍 Found 'total filament used' line: {}", line);
                    if (line.toLowerCase().contains("[g]")) {
                        log.debug("🔍 Line contains [g], trying to parse...");
                        Pattern weightPattern = Pattern.compile(".*\\[g\\]\\s*=\\s*([0-9.]+).*");
                        Matcher matcher = weightPattern.matcher(line);
                        if (matcher.find()) {
                            weightGrams = Double.parseDouble(matcher.group(1));
                            log.info("📏 Parsed weight from G-code: {}g", weightGrams);
                        } else {
                            log.warn("⚠️ Pattern didn't match line: {}", line);
                        }
                    }
                }

                // Also support legacy format with "weight" keyword
                if (weightGrams == 0.0 && line.toLowerCase().contains("filament") && line.toLowerCase().contains("weight")) {
                    Pattern weightPattern = Pattern.compile(".*([0-9.]+)\\s*g.*");
                    Matcher matcher = weightPattern.matcher(line);
                    if (matcher.find()) {
                        weightGrams = Double.parseDouble(matcher.group(1));
                        log.info("📏 Parsed weight from G-code (legacy): {}g", weightGrams);
                    }
                }

                // Look for PrusaSlicer time: "; estimated printing time (normal mode) = 5m 33s"
                if (line.toLowerCase().contains("estimated printing time") && line.contains("=")) {
                    // Try minutes and seconds format: "5m 33s"
                    Pattern timePatternMS = Pattern.compile(".*=\\s*([0-9]+)m\\s*([0-9]+)s.*");
                    Matcher matcherMS = timePatternMS.matcher(line);
                    if (matcherMS.find()) {
                        int minutes = Integer.parseInt(matcherMS.group(1));
                        int seconds = Integer.parseInt(matcherMS.group(2));
                        timeMinutes = minutes + (seconds >= 30 ? 1 : 0);
                        log.info("⏱️ Parsed time from G-code: {}m {}s ({}min)", minutes, seconds, timeMinutes);
                        continue;
                    }

                    // Try hours and minutes format: "1h 23m"
                    Pattern timePatternHM = Pattern.compile(".*=\\s*([0-9]+)h\\s*([0-9]+)m.*");
                    Matcher matcherHM = timePatternHM.matcher(line);
                    if (matcherHM.find()) {
                        int hours = Integer.parseInt(matcherHM.group(1));
                        int minutes = Integer.parseInt(matcherHM.group(2));
                        timeMinutes = hours * 60 + minutes;
                        log.info("⏱️ Parsed time from G-code: {}h {}m ({}min)", hours, minutes, timeMinutes);
                    }
                }

                // Legacy time format
                if (timeMinutes == 0 && (line.toLowerCase().contains("time") || line.toLowerCase().contains("duration"))) {
                    Pattern timePattern = Pattern.compile(".*([0-9]+)\\s*m.*");
                    Matcher matcher = timePattern.matcher(line);
                    if (matcher.find()) {
                        timeMinutes = Integer.parseInt(matcher.group(1));
                        log.info("⏱️ Parsed time from G-code (legacy): {}min", timeMinutes);
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

    private PrintQuotationResponse calculatePricing(
            SlicingResult slicingResult,
            String filename,
            PrintQuotationRequest request) {

        log.info("💰 Calculating pricing - Weight: {}g, Time: {}min",
                slicingResult.getFilamentWeightGrams(), slicingResult.getEstimatedPrintTimeMinutes());

        // Get material configuration for pricing
        PrintingPricingConfig.MaterialConfig materialConfig = pricingConfig.getMaterialConfig(request.getMaterial());

        // Calculate individual costs
        BigDecimal pricePerGram = materialConfig.getPricePerGram();
        BigDecimal pricePerMinute = pricingConfig.getStandardLayerHeight(); // Using this as per-minute rate

        BigDecimal materialCost = pricePerGram
                .multiply(BigDecimal.valueOf(slicingResult.getFilamentWeightGrams()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal timeCost = pricePerMinute
                .multiply(BigDecimal.valueOf(slicingResult.getEstimatedPrintTimeMinutes()))
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate total price
        BigDecimal totalPrice = materialCost.add(timeCost).setScale(2, RoundingMode.HALF_UP);

        log.info("🎯 Pricing calculated - Material cost: ${}, Time cost: ${}, Total: ${}",
                materialCost, timeCost, totalPrice);

        return PrintQuotationResponse.builder()
                .fileName(filename)
                .materialUsedGrams(slicingResult.getFilamentWeightGrams())
                .printingTimeMinutes(slicingResult.getEstimatedPrintTimeMinutes())
                .technology(request.getTechnology())
                .material(request.getMaterial())
                .layerHeight(request.getLayerHeight())
                .shells(request.getShells())
                .infill(request.getInfill())
                .supporters(request.getSupporters())
                .estimatedPrice(totalPrice)
                .currency("USD")
                .pricePerGram(pricePerGram)
                .pricePerMinute(pricePerMinute)
                .materialCost(materialCost)
                .timeCost(timeCost)
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

    /**
     * Validates that a path is safe and doesn't contain malicious characters
     * that could lead to command injection or path traversal attacks.
     */
    private void validatePathSafety(Path path, String pathDescription) {
        if (path == null) {
            throw new IllegalArgumentException(pathDescription + " path cannot be null");
        }

        String pathString = path.toString();

        // Check for path traversal attempts
        if (pathString.contains("..")) {
            throw new SecurityException("Path traversal detected in " + pathDescription + ": " + pathString);
        }

        // Check for shell metacharacters that could be dangerous
        String[] dangerousChars = {";", "&", "|", "$", "`", "\n", "\r"};
        for (String dangerousChar : dangerousChars) {
            if (pathString.contains(dangerousChar)) {
                throw new SecurityException("Dangerous character detected in " + pathDescription + ": " + pathString);
            }
        }

        // Ensure the path exists and is readable
        if (!Files.exists(path)) {
            throw new IllegalArgumentException(pathDescription + " does not exist: " + pathString);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException(pathDescription + " is not readable: " + pathString);
        }
    }

    /**
     * Validates that a numeric parameter is within acceptable bounds and contains
     * only valid numeric characters to prevent command injection.
     */
    private void validateNumericParameter(Double value, double min, double max, String parameterName) {
        if (value == null) {
            throw new IllegalArgumentException(parameterName + " cannot be null");
        }

        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(parameterName + " must be a valid number");
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %.2f and %.2f, got: %.2f",
                            parameterName, min, max, value));
        }
    }
}
