package com.threedfly.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.dto.CreateSellerRequest;
import com.threedfly.orderservice.entity.Seller;
import com.threedfly.orderservice.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SellerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SellerRepository sellerRepository;

    private CreateSellerRequest validSellerRequest;
    private Seller testSeller;

    @BeforeEach
    void setUp() {
        sellerRepository.deleteAll();
        
        validSellerRequest = new CreateSellerRequest();
        validSellerRequest.setUserId(1001L);
        validSellerRequest.setBusinessName("Test Electronics Store");
        validSellerRequest.setBusinessAddress("123 Business St, City");
        validSellerRequest.setContactEmail("seller@test.com");
        validSellerRequest.setContactPhone("+15550123");

        // Create a test seller for read/update/delete operations
        testSeller = new Seller();
        testSeller.setUserId(2001L);
        testSeller.setBusinessName("Existing Store");
        testSeller.setBusinessAddress("456 Store Ave, City");
        testSeller.setContactEmail("existing@test.com");
        testSeller.setContactPhone("+15550456");
        testSeller.setVerified(false);
        testSeller = sellerRepository.save(testSeller);
    }

    // Create Seller Tests
    @Test
    void testCreateSeller_Success() throws Exception {
        String sellerJson = objectMapper.writeValueAsString(validSellerRequest);

        mockMvc.perform(post("/sellers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sellerJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(1001L))
                .andExpect(jsonPath("$.businessName").value("Test Electronics Store"))
                .andExpect(jsonPath("$.businessAddress").value("123 Business St, City"))
                .andExpect(jsonPath("$.contactEmail").value("seller@test.com"))
                .andExpect(jsonPath("$.contactPhone").value("+15550123"))
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void testCreateSeller_InvalidRequest_MissingFields() throws Exception {
        CreateSellerRequest invalidRequest = new CreateSellerRequest();
        // Missing required fields
        String invalidJson = objectMapper.writeValueAsString(invalidRequest);

        mockMvc.perform(post("/sellers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateSeller_InvalidRequest_InvalidEmail() throws Exception {
        validSellerRequest.setContactEmail("invalid-email");
        String invalidJson = objectMapper.writeValueAsString(validSellerRequest);

        mockMvc.perform(post("/sellers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // Get All Sellers Tests
    @Test
    void testGetAllSellers_Success() throws Exception {
        mockMvc.perform(get("/sellers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].businessName").exists());
    }

    @Test
    void testGetAllSellers_EmptyDatabase() throws Exception {
        sellerRepository.deleteAll();

        mockMvc.perform(get("/sellers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // Get Seller by ID Tests
    @Test
    void testGetSellerById_Success() throws Exception {
        mockMvc.perform(get("/sellers/" + testSeller.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testSeller.getId()))
                .andExpect(jsonPath("$.businessName").value("Existing Store"))
                .andExpect(jsonPath("$.userId").value(2001L));
    }

    @Test
    void testGetSellerById_NotFound() throws Exception {
        mockMvc.perform(get("/sellers/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetSellerById_InvalidId() throws Exception {
        mockMvc.perform(get("/sellers/invalid"))
                .andExpect(status().isBadRequest());
    }

    // Get Seller by User ID Tests
    @Test
    void testGetSellerByUserId_Success() throws Exception {
        mockMvc.perform(get("/sellers/user/" + testSeller.getUserId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(testSeller.getUserId()))
                .andExpect(jsonPath("$.businessName").value("Existing Store"));
    }

    @Test
    void testGetSellerByUserId_NotFound() throws Exception {
        mockMvc.perform(get("/sellers/user/99999"))
                .andExpect(status().isNotFound());
    }

    // Get Verified Sellers Tests
    @Test
    void testGetVerifiedSellers_Success() throws Exception {
        // Create a verified seller
        Seller verifiedSeller = new Seller();
        verifiedSeller.setUserId(3001L);
        verifiedSeller.setBusinessName("Verified Store");
        verifiedSeller.setBusinessAddress("789 Verified St");
        verifiedSeller.setContactEmail("verified@test.com");
        verifiedSeller.setContactPhone("+15550789");
        verifiedSeller.setVerified(true);
        sellerRepository.save(verifiedSeller);

        mockMvc.perform(get("/sellers/verified"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[*].verified").value(everyItem(equalTo(true))));
    }

    // Verify Seller Tests
    @Test
    void testVerifySeller_Success() throws Exception {
        mockMvc.perform(patch("/sellers/" + testSeller.getId() + "/verify"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testSeller.getId()))
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void testVerifySeller_NotFound() throws Exception {
        mockMvc.perform(patch("/sellers/99999/verify"))
                .andExpect(status().isNotFound());
    }

    // Delete Seller Tests
    @Test
    void testDeleteSeller_Success() throws Exception {
        mockMvc.perform(delete("/sellers/" + testSeller.getId()))
                .andExpect(status().isNoContent());

        // Verify seller is deleted
        mockMvc.perform(get("/sellers/" + testSeller.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteSeller_NotFound() throws Exception {
        mockMvc.perform(delete("/sellers/99999"))
                .andExpect(status().isNotFound());
    }

    // HTTP Method Tests
    @Test
    void testInvalidHttpMethods() throws Exception {
        // Test invalid methods on various endpoints
        mockMvc.perform(put("/sellers"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/sellers"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(post("/sellers/" + testSeller.getId()))
                .andExpect(status().isMethodNotAllowed());
    }
} 