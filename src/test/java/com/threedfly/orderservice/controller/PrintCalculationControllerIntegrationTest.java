package com.threedfly.orderservice.controller;

import com.threedfly.orderservice.TestFileFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "printing.slicer.type=prusa",
        "printing.prusa.slicer.path=/Users/sefica/Downloads/order-service/src/test/resources/mock-slicer/mock-slicer.sh",
        "printing.temp.directory=/tmp/printing-test",
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
        "printing.pricing.shell-cost-factor=0.25",
        "printing.pricing.standard-layer-height=0.2"
})
class PrintCalculationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Provides all valid technology-material-supporter combinations
     * FDM: All materials (PLA, ABS, PETG, TPU)
     * SLS: PLA, ABS, PETG (no TPU)
     * SLA: PLA, ABS only (no PETG, TPU)
     */
    static Stream<Arguments> validCombinations() {
        return Stream.of(
                // FDM + All Materials + Supporters true/false
                Arguments.of("FDM", "PLA", true),
                Arguments.of("FDM", "PLA", false),
                Arguments.of("FDM", "ABS", true),
                Arguments.of("FDM", "ABS", false),
                Arguments.of("FDM", "PETG", true),
                Arguments.of("FDM", "PETG", false),
                Arguments.of("FDM", "TPU", true),
                Arguments.of("FDM", "TPU", false),

                // SLS + PLA, ABS, PETG + Supporters true/false
                Arguments.of("SLS", "PLA", true),
                Arguments.of("SLS", "PLA", false),
                Arguments.of("SLS", "ABS", true),
                Arguments.of("SLS", "ABS", false),
                Arguments.of("SLS", "PETG", true),
                Arguments.of("SLS", "PETG", false),

                // SLA + PLA, ABS + Supporters true/false
                Arguments.of("SLA", "PLA", true),
                Arguments.of("SLA", "PLA", false),
                Arguments.of("SLA", "ABS", true),
                Arguments.of("SLA", "ABS", false)
        );
    }

    /**
     * Provides invalid technology-material combinations
     */
    static Stream<Arguments> invalidCombinations() {
        return Stream.of(
                // SLS with TPU
                Arguments.of("SLS", "TPU", true),
                Arguments.of("SLS", "TPU", false),

                // SLA with PETG or TPU
                Arguments.of("SLA", "PETG", true),
                Arguments.of("SLA", "PETG", false),
                Arguments.of("SLA", "TPU", true),
                Arguments.of("SLA", "TPU", false)
        );
    }

    @ParameterizedTest(name = "Test {index}: {0} + {1} + supporters={2}")
    @MethodSource("validCombinations")
    void testCalculateQuotation_AllValidCombinations(String technology, String material, boolean supporters) throws Exception {
        MockMultipartFile file = TestFileFactory.createTestStlFile();

        mockMvc.perform(multipart("/api/print/calculate")
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
                .andExpect(jsonPath("$.fileName").value("test_cube.stl"))
                .andExpect(jsonPath("$.technology").value(technology))
                .andExpect(jsonPath("$.material").value(material))
                .andExpect(jsonPath("$.layerHeight").value(0.2))
                .andExpect(jsonPath("$.shells").value(2))
                .andExpect(jsonPath("$.infill").value(15))
                .andExpect(jsonPath("$.supporters").value(supporters))
                .andExpect(jsonPath("$.estimatedPrice").exists())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.materialUsedGrams").exists())
                .andExpect(jsonPath("$.printingTimeMinutes").exists())
                .andExpect(jsonPath("$.pricePerGram").exists())
                .andExpect(jsonPath("$.pricePerMinute").exists())
                .andExpect(jsonPath("$.materialCost").exists())
                .andExpect(jsonPath("$.timeCost").exists());
    }

    @ParameterizedTest(name = "Test invalid {index}: {0} + {1} + supporters={2}")
    @MethodSource("invalidCombinations")
    void testCalculateQuotation_InvalidCombinations(String technology, String material, boolean supporters) throws Exception {
        MockMultipartFile file = TestFileFactory.createTestStlFile();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", technology)
                        .param("material", material)
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", String.valueOf(supporters))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Parameter Combination"))
                .andExpect(jsonPath("$.message").value(containsString("not compatible")));
    }

    @Test
    void testCalculateQuotation_WithObjFile() throws Exception {
        MockMultipartFile file = TestFileFactory.createTestObjFile();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("test_cube.obj"));
    }

    @Test
    void testCalculateQuotation_EmptyFile() throws Exception {
        MockMultipartFile file = TestFileFactory.createEmptyFile();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid File Type"))
                .andExpect(jsonPath("$.message").value("File is empty"));
    }

    @Test
    void testCalculateQuotation_InvalidFileType() throws Exception {
        MockMultipartFile file = TestFileFactory.createInvalidFileType();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid File Type"))
                .andExpect(jsonPath("$.message").value(containsString("Unsupported file type")));
    }

    @Test
    void testCalculateQuotation_MissingRequiredParameter() throws Exception {
        MockMultipartFile file = TestFileFactory.createTestStlFile();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "15")
                        // Missing supporters parameter
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.supporters").exists());
    }

    @Test
    void testCalculateQuotation_InvalidLayerHeight() throws Exception {
        MockMultipartFile file = TestFileFactory.createTestStlFile();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.01") // Too small
                        .param("shells", "2")
                        .param("infill", "15")
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.layerHeight").exists());
    }

    @Test
    void testCalculateQuotation_InvalidShellsCount() throws Exception {
        MockMultipartFile file = TestFileFactory.createTestStlFile();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "10") // Too many
                        .param("infill", "15")
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.shells").exists());
    }

    @Test
    void testCalculateQuotation_InvalidInfill() throws Exception {
        MockMultipartFile file = TestFileFactory.createTestStlFile();

        mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "25") // Invalid value
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.infill").exists());
    }

    @Test
    void testCalculateQuotation_SupportersAffectPricing() throws Exception {
        MockMultipartFile file = TestFileFactory.createTestStlFile();

        // Test WITHOUT supporters
        String responseWithoutSupport = mockMvc.perform(multipart("/api/print/calculate")
                        .file(file)
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "20")
                        .param("supporters", "false")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supporters").value(false))
                .andExpect(jsonPath("$.estimatedPrice").exists())
                .andExpect(jsonPath("$.materialUsedGrams").value(12.34))
                .andExpect(jsonPath("$.printingTimeMinutes").value(83))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Test WITH supporters
        String responseWithSupport = mockMvc.perform(multipart("/api/print/calculate")
                        .file(TestFileFactory.createTestStlFile()) // Create fresh file
                        .param("technology", "FDM")
                        .param("material", "PLA")
                        .param("layerHeight", "0.2")
                        .param("shells", "2")
                        .param("infill", "20")
                        .param("supporters", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supporters").value(true))
                .andExpect(jsonPath("$.estimatedPrice").exists())
                .andExpect(jsonPath("$.materialUsedGrams").value(15.50))
                .andExpect(jsonPath("$.printingTimeMinutes").value(105))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse the responses to compare prices
        org.json.JSONObject jsonWithoutSupport = new org.json.JSONObject(responseWithoutSupport);
        org.json.JSONObject jsonWithSupport = new org.json.JSONObject(responseWithSupport);

        double priceWithoutSupport = jsonWithoutSupport.getDouble("estimatedPrice");
        double priceWithSupport = jsonWithSupport.getDouble("estimatedPrice");

        // Assert that price WITH supporters is higher than WITHOUT supporters
        org.junit.jupiter.api.Assertions.assertTrue(
                priceWithSupport > priceWithoutSupport,
                String.format("Price with supporters ($%.2f) should be greater than without supporters ($%.2f)",
                        priceWithSupport, priceWithoutSupport)
        );

        // Also verify the material and time differences
        double materialWithoutSupport = jsonWithoutSupport.getDouble("materialUsedGrams");
        double materialWithSupport = jsonWithSupport.getDouble("materialUsedGrams");
        org.junit.jupiter.api.Assertions.assertTrue(
                materialWithSupport > materialWithoutSupport,
                "Material usage with supporters should be greater than without"
        );

        int timeWithoutSupport = jsonWithoutSupport.getInt("printingTimeMinutes");
        int timeWithSupport = jsonWithSupport.getInt("printingTimeMinutes");
        org.junit.jupiter.api.Assertions.assertTrue(
                timeWithSupport > timeWithoutSupport,
                "Print time with supporters should be greater than without"
        );
    }
}
