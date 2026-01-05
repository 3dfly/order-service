package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.TestFileFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REAL Integration Tests using actual PrusaSlicer/BambuStudio.
 *
 * These tests:
 * - Use REAL slicer binaries (not mocks)
 * - Take longer to run (5-30 seconds per test)
 * - Validate actual slicing behavior
 * - Provide high confidence in production behavior
 *
 * Run with: ./gradlew test --tests "*RealSlicerIntegrationTest"
 * Or tag-based: ./gradlew test -Dgroups=integration
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@TestPropertySource(properties = {
        "printing.slicer.type=prusa",
        "printing.prusa.slicer.path=/Applications/PrusaSlicer.app/Contents/MacOS/PrusaSlicer",
        "printing.bambu.slicer.path=/Applications/BambuStudio.app/Contents/MacOS/BambuStudio",
        "printing.temp.directory=/tmp/printing-test-real",
        "printing.slicer.config.directory=slicer-configs",
        "printing.pricing.technology.FDM=1.0",
        "printing.pricing.technology.SLS=1.5",
        "printing.pricing.technology.SLA=2.0",
        "printing.pricing.material.PLA.density=1.24",
        "printing.pricing.material.PLA.price-per-gram=0.05",
        "printing.pricing.material.ABS.density=1.04",
        "printing.pricing.material.ABS.price-per-gram=0.06",
        "printing.pricing.material.PETG.density=1.27",
        "printing.pricing.material.PETG.price-per-gram=0.055",
        "printing.pricing.material.TPU.density=1.2",
        "printing.pricing.material.TPU.price-per-gram=0.08",
        "printing.pricing.material.ASA.density=1.05",
        "printing.pricing.material.ASA.price-per-gram=0.065",
        "printing.pricing.shell-cost-factor=0.25",
        "printing.pricing.standard-layer-height=0.2"
})
@EnabledIf("isSlicerInstalled")
class PrintCalculationRealSlicerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Check if PrusaSlicer is actually installed before running tests.
     */
    static boolean isSlicerInstalled() {
        Path prusaSlicer = Path.of("/Applications/PrusaSlicer.app/Contents/MacOS/PrusaSlicer");
        boolean installed = Files.exists(prusaSlicer) && Files.isExecutable(prusaSlicer);

        if (!installed) {
            System.out.println("⚠️  Skipping real slicer integration tests - PrusaSlicer not found at: " + prusaSlicer);
            System.out.println("   Install PrusaSlicer to run these tests: https://www.prusa3d.com/page/prusaslicer_424/");
        }

        return installed;
    }

    /**
     * Key test scenarios (subset of all combinations for speed).
     * These represent the most common use cases.
     */
    static Stream<Arguments> keyScenarios() {
        return Stream.of(
                // FDM with common materials
                Arguments.of("FDM", "PLA", true, "Common FDM with PLA and supports"),
                Arguments.of("FDM", "PLA", false, "Common FDM with PLA without supports"),
                Arguments.of("FDM", "PETG", true, "FDM with PETG and supports"),
                Arguments.of("FDM", "ABS", false, "FDM with ABS without supports"),
                Arguments.of("FDM", "ASA", true, "FDM with ASA and supports")
        );
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("keyScenarios")
    void testCalculateQuotation_RealSlicer_KeyScenarios(
            String technology,
            String material,
            boolean supporters,
            String description) throws Exception {

        System.out.println("\n🔧 Testing with REAL slicer: " + description);

        // Use real STL file (Model3 - Love is small and fast to slice)
        MockMultipartFile file = TestFileFactory.createRealStlFile("Model3 - Love.stl");

        MvcResult result = mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", technology)
                        .param("material", material)
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", String.valueOf(supporters))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("Model3 - Love.stl"))
                .andExpect(jsonPath("$.technology").value(technology))
                .andExpect(jsonPath("$.material").value(material))
                .andExpect(jsonPath("$.supporters").value(supporters))
                .andExpect(jsonPath("$.estimatedPrice").exists())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        // Parse response to validate real values
        String responseBody = result.getResponse().getContentAsString();
        org.json.JSONObject json = new org.json.JSONObject(responseBody);

        double materialUsed = json.getDouble("materialUsedGrams");
        int printTime = json.getInt("printingTimeMinutes");
        double estimatedPrice = json.getDouble("estimatedPrice");

        System.out.printf("   ✅ Material: %.2fg, Time: %dmin, Price: $%.2f%n",
                materialUsed, printTime, estimatedPrice);

        // Validate actual expected ranges for Model3 - Love.stl with REAL PrusaSlicer
        // These values are based on actual PrusaSlicer output with:
        // - 0.2mm layer height, 2 shells, 15% infill, 5 top layers, 3 bottom layers
        // Allow ±30% tolerance for slicer version differences and parameter variations

        // Expected ranges based on real slicer output (material: ~50-55g, time: varies by settings)
        if (supporters) {
            // With supporters (may add 20-50% more material if geometry requires it)
            assertTrue(materialUsed >= 45.0 && materialUsed <= 75.0,
                    String.format("Material with supporters should be 45-75g, got %.2fg", materialUsed));
            assertTrue(printTime >= 60 && printTime <= 250,
                    String.format("Print time with supporters should be 60-250min, got %dmin", printTime));
        } else {
            // Without supporters
            assertTrue(materialUsed >= 45.0 && materialUsed <= 65.0,
                    String.format("Material without supporters should be 45-65g, got %.2fg", materialUsed));
            assertTrue(printTime >= 60 && printTime <= 220,
                    String.format("Print time without supporters should be 60-220min, got %dmin", printTime));
        }

        // Price should be reasonable for this model size (material + time cost)
        assertTrue(estimatedPrice > 5.0 && estimatedPrice < 50.0,
                String.format("Price should be $5.00-$50.00 for Love model, got $%.2f", estimatedPrice));
    }

    @Test
    void testCalculateQuotation_RealSlicer_SupportersAffectValues() throws Exception {
        System.out.println("\n🔧 Testing REAL slicer: Supporters impact on material/time");

        // Test WITHOUT supporters (using real Pineapple model - has overhangs that need supports)
        MvcResult resultWithoutSupport = mockMvc.perform(multipart("/api/print/calculate")
                        .file(TestFileFactory.createRealStlFile("Model4 - Pineapple.stl"))
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "false")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Test WITH supporters (same Pineapple model)
        MvcResult resultWithSupport = mockMvc.perform(multipart("/api/print/calculate")
                        .file(TestFileFactory.createRealStlFile("Model4 - Pineapple.stl"))
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse responses
        org.json.JSONObject jsonWithout = new org.json.JSONObject(
                resultWithoutSupport.getResponse().getContentAsString());
        org.json.JSONObject jsonWith = new org.json.JSONObject(
                resultWithSupport.getResponse().getContentAsString());

        double materialWithout = jsonWithout.getDouble("materialUsedGrams");
        double materialWith = jsonWith.getDouble("materialUsedGrams");
        int timeWithout = jsonWithout.getInt("printingTimeMinutes");
        int timeWith = jsonWith.getInt("printingTimeMinutes");
        double priceWithout = jsonWithout.getDouble("estimatedPrice");
        double priceWith = jsonWith.getDouble("estimatedPrice");

        System.out.printf("   WITHOUT supporters: %.2fg, %dmin, $%.2f%n",
                materialWithout, timeWithout, priceWithout);
        System.out.printf("   WITH supporters:    %.2fg, %dmin, $%.2f%n",
                materialWith, timeWith, priceWith);

        // Model4 - Pineapple has overhangs that require supports
        // Validate expected ranges for this specific model

        // Without supporters: should be in reasonable range for Pineapple model
        assertTrue(materialWithout >= 18.0 && materialWithout <= 28.0,
                String.format("Material without supporters should be 18-28g for Pineapple, got %.2fg", materialWithout));
        assertTrue(timeWithout >= 50 && timeWithout <= 90,
                String.format("Print time without supporters should be 50-90min for Pineapple, got %dmin", timeWithout));

        // With supporters: Pineapple WILL generate supports (has overhangs)
        // Supports should add significant material (~25-35% increase)
        assertTrue(materialWith > materialWithout,
                "Material with supporters should be greater than without for Pineapple");
        assertTrue(materialWith >= 24.0 && materialWith <= 36.0,
                String.format("Material with supporters should be 24-36g for Pineapple, got %.2fg", materialWith));
        assertTrue(timeWith >= 80 && timeWith <= 130,
                String.format("Print time with supporters should be 80-130min for Pineapple, got %dmin", timeWith));

        // Calculate and verify percentage increases
        double materialIncrease = (materialWith - materialWithout) / materialWithout * 100;
        double timeIncrease = (timeWith - timeWithout) / (double) timeWithout * 100;
        double priceIncrease = (priceWith - priceWithout) / priceWithout * 100;

        System.out.printf("   ✅ Supports generated - Material increase: %.1f%%, Time increase: %.1f%%, Price increase: %.1f%%%n",
                materialIncrease, timeIncrease, priceIncrease);

        // Verify reasonable increase ranges (supports should add 20-50%)
        assertTrue(materialIncrease >= 20.0 && materialIncrease <= 50.0,
                String.format("Material increase should be 20-50%%, got %.1f%%", materialIncrease));
        assertTrue(priceWith > priceWithout,
                "Price should increase when supports add material");
    }

    @Test
    void testCalculateQuotation_RealSlicer_DifferentLayerHeights() throws Exception {
        System.out.println("\n🔧 Testing REAL slicer: Different layer heights");

        // Test with 0.1mm layer (fine) - using real Love model
        MvcResult resultFine = mockMvc.perform(multipart("/api/print/calculate")
                        .file(TestFileFactory.createRealStlFile("Model3 - Love.stl"))
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.1")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "false")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        // Test with 0.3mm layer (coarse) - same Love model
        MvcResult resultCoarse = mockMvc.perform(multipart("/api/print/calculate")
                        .file(TestFileFactory.createRealStlFile("Model3 - Love.stl"))
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.3")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "false")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        org.json.JSONObject jsonFine = new org.json.JSONObject(
                resultFine.getResponse().getContentAsString());
        org.json.JSONObject jsonCoarse = new org.json.JSONObject(
                resultCoarse.getResponse().getContentAsString());

        double materialFine = jsonFine.getDouble("materialUsedGrams");
        double materialCoarse = jsonCoarse.getDouble("materialUsedGrams");
        int timeFine = jsonFine.getInt("printingTimeMinutes");
        int timeCoarse = jsonCoarse.getInt("printingTimeMinutes");

        System.out.printf("   Fine layer (0.1mm):   %.2fg, %dmin%n", materialFine, timeFine);
        System.out.printf("   Coarse layer (0.3mm): %.2fg, %dmin%n", materialCoarse, timeCoarse);

        // Validate expected ranges for Model3 - Love with real PrusaSlicer
        // Material should be similar regardless of layer height (same volume)
        // Based on real slicer output: ~50-55g for this model
        assertTrue(materialFine >= 45.0 && materialFine <= 65.0,
                String.format("Fine layer material should be 45-65g, got %.2fg", materialFine));
        assertTrue(materialCoarse >= 45.0 && materialCoarse <= 65.0,
                String.format("Coarse layer material should be 45-65g, got %.2fg", materialCoarse));

        // Material should be roughly equal (±10%) since same model volume
        double materialDiff = Math.abs(materialFine - materialCoarse) / materialCoarse * 100;
        assertTrue(materialDiff <= 10.0,
                String.format("Material should be similar (±10%%), difference was %.1f%%", materialDiff));

        // Finer layers = more layers = significantly longer print time
        assertTrue(timeFine > timeCoarse,
                String.format("Fine layer (%dmin) should take longer than coarse (%dmin)",
                        timeFine, timeCoarse));

        // Fine layers should take at least 40% longer (0.1mm vs 0.3mm = 3x more layers)
        double timeIncrease = (timeFine - timeCoarse) / (double) timeCoarse * 100;
        assertTrue(timeIncrease >= 40.0,
                String.format("Fine layers should take at least 40%% longer, got %.1f%%", timeIncrease));

        System.out.printf("   ✅ Layer height correctly affects print time (+%.1f%% for fine layers)%n", timeIncrease);
    }

    @Test
    void testCalculateQuotation_RealSlicer_AllMaterials() throws Exception {
        System.out.println("\n🔧 Testing REAL slicer: All FDM materials");

        String[] materials = {"PLA", "ABS", "PETG", "TPU", "ASA"};
        java.util.Map<String, Double> materialWeights = new java.util.HashMap<>();
        java.util.Map<String, Integer> materialTimes = new java.util.HashMap<>();

        for (String material : materials) {
            System.out.print("   Testing material: " + material);

            // Using real Love model for each material test
            MvcResult result = mockMvc.perform(multipart("/api/print/calculate")
                            .file(TestFileFactory.createRealStlFile("Model3 - Love.stl"))
                            .param("technology", "FDM")
                            .param("material", material)
                            .param("layerHeight", "0.2")
                            .param("shells", "2")
                            .param("infill", "15")
                            .param("supporters", "false")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.material").value(material))
                    .andExpect(jsonPath("$.materialUsedGrams").exists())
                    .andExpect(jsonPath("$.printingTimeMinutes").exists())
                    .andExpect(jsonPath("$.estimatedPrice").exists())
                    .andReturn();

            org.json.JSONObject json = new org.json.JSONObject(result.getResponse().getContentAsString());
            double weight = json.getDouble("materialUsedGrams");
            int time = json.getInt("printingTimeMinutes");
            double price = json.getDouble("estimatedPrice");

            materialWeights.put(material, weight);
            materialTimes.put(material, time);

            // Validate reasonable ranges for Model3 - Love with real PrusaSlicer
            // Based on actual slicer output: ~50-55g material, varies by settings
            assertTrue(weight >= 45.0 && weight <= 65.0,
                    String.format("%s: Material should be 45-65g, got %.2fg", material, weight));
            assertTrue(time >= 60 && time <= 220,
                    String.format("%s: Time should be 60-220min, got %dmin", material, time));
            assertTrue(price > 5.0 && price < 50.0,
                    String.format("%s: Price should be $5.00-$50.00, got $%.2f", material, price));

            System.out.printf(" - %.2fg, %dmin, $%.2f%n", weight, time, price);
        }

        // Verify material-specific differences based on density
        // PLA (1.24 g/cm³), ABS (1.04 g/cm³), PETG (1.27 g/cm³), TPU (1.2 g/cm³), ASA (1.05 g/cm³)
        // Same slicing parameters should produce slightly different material usage due to density

        // All materials should produce similar but not identical results
        // Print times vary significantly across materials due to different print speeds
        // (e.g., TPU is much slower than PLA due to retraction and speed settings)
        int minTime = materialTimes.values().stream().min(Integer::compareTo).orElse(0);
        int maxTime = materialTimes.values().stream().max(Integer::compareTo).orElse(0);
        assertTrue(maxTime - minTime <= 80,
                String.format("Print times should vary by ≤80min across materials, got %d-%dmin", minTime, maxTime));

        System.out.println("   ✅ All materials processed successfully with consistent results");
    }

    @Test
    void testCalculateQuotation_RealSlicer_With3MFFile() throws Exception {
        System.out.println("\n🔧 Testing REAL slicer: 3MF file support");

        // Use real 3MF file with embedded parameters
        MockMultipartFile file = TestFileFactory.createReal3MFFile("Model3 - Love.3mf");

        // 3MF files don't need manual parameters - they have embedded settings
        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("Model3 - Love.3mf"))
                .andExpect(jsonPath("$.materialUsedGrams").exists())
                .andExpect(jsonPath("$.printingTimeMinutes").exists())
                .andExpect(jsonPath("$.technology").exists())
                .andExpect(jsonPath("$.material").exists());

        System.out.println("   ✅ 3MF file processed successfully with embedded parameters");
    }
}
