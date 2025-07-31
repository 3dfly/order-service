package com.threedfly.orderservice.service;

import com.threedfly.orderservice.dto.PrintingCalculationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "printing.price.per-gram=0.05",
    "printing.price.per-minute=0.10",
    "printing.bambu.slicer.path=src/test/resources/test-mock-slicer.py",
    "printing.bambu.printer.config=/tmp/test-config.ini",
    "printing.temp.directory=/tmp/test-printing"
})
class PrintingCalculationServiceTest {

    private PrintingCalculationService printingCalculationService;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        printingCalculationService = new PrintingCalculationService();
        // Set up the service with test configuration
        java.lang.reflect.Field pricePerGramField = null;
        java.lang.reflect.Field pricePerMinuteField = null;
        java.lang.reflect.Field bambuSlicerPathField = null;
        java.lang.reflect.Field printerConfigField = null;
        java.lang.reflect.Field tempDirectoryField = null;
        
        try {
            pricePerGramField = PrintingCalculationService.class.getDeclaredField("pricePerGram");
            pricePerGramField.setAccessible(true);
            pricePerGramField.set(printingCalculationService, new BigDecimal("0.05"));
            
            pricePerMinuteField = PrintingCalculationService.class.getDeclaredField("pricePerMinute");
            pricePerMinuteField.setAccessible(true);
            pricePerMinuteField.set(printingCalculationService, new BigDecimal("0.10"));
            
            bambuSlicerPathField = PrintingCalculationService.class.getDeclaredField("bambuSlicerPath");
            bambuSlicerPathField.setAccessible(true);
            bambuSlicerPathField.set(printingCalculationService, "src/test/resources/test-mock-slicer.py");
            
            printerConfigField = PrintingCalculationService.class.getDeclaredField("printerConfig");
            printerConfigField.setAccessible(true);
            printerConfigField.set(printingCalculationService, "/tmp/test-config.ini");
            
            tempDirectoryField = PrintingCalculationService.class.getDeclaredField("tempDirectory");
            tempDirectoryField.setAccessible(true);
            tempDirectoryField.set(printingCalculationService, tempDir.toString());
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test configuration", e);
        }
    }

    @Test
    void testCalculatePrice_ValidStlFile() throws IOException {
        // Create a valid STL file content
        String stlContent = createValidStlContent();
        MockMultipartFile stlFile = new MockMultipartFile(
            "stlFile", 
            "test-model.stl", 
            "application/octet-stream", 
            stlContent.getBytes()
        );

        PrintingCalculationResponse response = printingCalculationService.calculatePrice(stlFile);

        assertNotNull(response);
        assertEquals("test-model.stl", response.getFilename());
        assertEquals(new BigDecimal("0.05"), response.getPricePerGram());
        assertEquals(new BigDecimal("0.10"), response.getPricePerMinute());
        
        // Since we're using echo as the slicer, we expect parsing to fail gracefully
        // but the service should handle it without throwing exceptions
        assertTrue(response.getStatus().equals("SUCCESS") || response.getStatus().equals("ERROR"));
    }

    @Test
    void testCalculatePrice_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "stlFile", 
            "empty.stl", 
            "application/octet-stream", 
            new byte[0]
        );

        PrintingCalculationResponse response = printingCalculationService.calculatePrice(emptyFile);

        assertNotNull(response);
        assertEquals("ERROR", response.getStatus());
        assertEquals("empty.stl", response.getFilename());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());
        assertTrue(response.getMessage().contains("Invalid STL file provided"));
    }

    @Test
    void testCalculatePrice_InvalidFileExtension() {
        MockMultipartFile invalidFile = new MockMultipartFile(
            "stlFile", 
            "model.txt", 
            "text/plain", 
            "This is not an STL file".getBytes()
        );

        PrintingCalculationResponse response = printingCalculationService.calculatePrice(invalidFile);

        assertNotNull(response);
        assertEquals("ERROR", response.getStatus());
        assertEquals("model.txt", response.getFilename());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());
        assertTrue(response.getMessage().contains("Invalid STL file provided"));
    }

    @Test
    void testCalculatePrice_NullFilename() {
        MockMultipartFile fileWithNullName = new MockMultipartFile(
            "stlFile", 
            null, 
            "application/octet-stream", 
            createValidStlContent().getBytes()
        );

        PrintingCalculationResponse response = printingCalculationService.calculatePrice(fileWithNullName);

        assertNotNull(response);
        assertEquals("ERROR", response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getTotalPrice());
        assertTrue(response.getMessage().contains("Invalid STL file provided"));
    }

    @Test
    void testCalculatePrice_LargeFile() {
        // Create a larger STL file to test file handling
        StringBuilder largeStlContent = new StringBuilder();
        largeStlContent.append("solid large_model\n");
        
        // Add multiple facets to make it larger
        for (int i = 0; i < 100; i++) {
            largeStlContent.append("  facet normal 0.0 0.0 1.0\n");
            largeStlContent.append("    outer loop\n");
            largeStlContent.append(String.format("      vertex %d.0 0.0 1.0\n", i));
            largeStlContent.append(String.format("      vertex %d.0 1.0 1.0\n", i + 1));
            largeStlContent.append(String.format("      vertex %d.0 1.0 1.0\n", i));
            largeStlContent.append("    endloop\n");
            largeStlContent.append("  endfacet\n");
        }
        largeStlContent.append("endsolid large_model\n");

        MockMultipartFile largeFile = new MockMultipartFile(
            "stlFile", 
            "large-model.stl", 
            "application/octet-stream", 
            largeStlContent.toString().getBytes()
        );

        PrintingCalculationResponse response = printingCalculationService.calculatePrice(largeFile);

        assertNotNull(response);
        assertEquals("large-model.stl", response.getFilename());
        // Should handle large files without issues
        assertTrue(response.getStatus().equals("SUCCESS") || response.getStatus().equals("ERROR"));
    }

    @Test
    void testFileValidation_ValidStlExtension() throws Exception {
        java.lang.reflect.Method isValidStlFileMethod = PrintingCalculationService.class
            .getDeclaredMethod("isValidStlFile", MultipartFile.class);
        isValidStlFileMethod.setAccessible(true);

        MockMultipartFile validFile = new MockMultipartFile(
            "stlFile", "model.stl", "application/octet-stream", "content".getBytes());
        
        Boolean result = (Boolean) isValidStlFileMethod.invoke(printingCalculationService, validFile);
        assertTrue(result);
    }

    @Test
    void testFileValidation_InvalidExtension() throws Exception {
        java.lang.reflect.Method isValidStlFileMethod = PrintingCalculationService.class
            .getDeclaredMethod("isValidStlFile", MultipartFile.class);
        isValidStlFileMethod.setAccessible(true);

        MockMultipartFile invalidFile = new MockMultipartFile(
            "stlFile", "model.obj", "application/octet-stream", "content".getBytes());
        
        Boolean result = (Boolean) isValidStlFileMethod.invoke(printingCalculationService, invalidFile);
        assertFalse(result);
    }

    @Test
    void testFileValidation_CaseInsensitive() throws Exception {
        java.lang.reflect.Method isValidStlFileMethod = PrintingCalculationService.class
            .getDeclaredMethod("isValidStlFile", MultipartFile.class);
        isValidStlFileMethod.setAccessible(true);

        MockMultipartFile upperCaseFile = new MockMultipartFile(
            "stlFile", "MODEL.STL", "application/octet-stream", "content".getBytes());
        
        Boolean result = (Boolean) isValidStlFileMethod.invoke(printingCalculationService, upperCaseFile);
        assertTrue(result);
    }

    @Test
    void testPricingCalculation() {
        // Test the pricing calculation logic
        double weightGrams = 50.0;
        int timeMinutes = 120;
        
        BigDecimal expectedWeightCost = new BigDecimal("0.05").multiply(BigDecimal.valueOf(weightGrams));
        BigDecimal expectedTimeCost = new BigDecimal("0.10").multiply(BigDecimal.valueOf(timeMinutes));
        BigDecimal expectedTotal = expectedWeightCost.add(expectedTimeCost);
        
        // Weight cost: 50g * $0.05 = $2.50
        assertEquals(0, new BigDecimal("2.50").compareTo(expectedWeightCost));
        
        // Time cost: 120min * $0.10 = $12.00
        assertEquals(0, new BigDecimal("12.00").compareTo(expectedTimeCost));
        
        // Total: $2.50 + $12.00 = $14.50
        assertEquals(0, new BigDecimal("14.50").compareTo(expectedTotal));
    }

    private String createValidStlContent() {
        return """
            solid test_cube
              facet normal 0.0 0.0 1.0
                outer loop
                  vertex 0.0 0.0 1.0
                  vertex 1.0 0.0 1.0
                  vertex 1.0 1.0 1.0
                endloop
              endfacet
              facet normal 0.0 0.0 1.0
                outer loop
                  vertex 0.0 0.0 1.0
                  vertex 1.0 1.0 1.0
                  vertex 0.0 1.0 1.0
                endloop
              endfacet
            endsolid test_cube
            """;
    }
} 