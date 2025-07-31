package com.threedfly.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthEndpoint_Success() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("order-service"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.database").exists());
    }

    @Test
    void testHealthEndpoint_ResponseStructure() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.service").isString())
                .andExpect(jsonPath("$.database").exists());
    }

    @Test
    void testReadinessEndpoint_Success() throws Exception {
        mockMvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testReadinessEndpoint_ResponseStructure() throws Exception {
        mockMvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").isBoolean())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void testHealthEndpoint_DatabaseConnection() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database").value(anyOf(
                    equalTo("UP"), 
                    equalTo("DOWN"), 
                    equalTo("NOT_CONFIGURED")
                )));
    }

    @Test
    void testHealthEndpoint_MultipleRequests() throws Exception {
        // Test that health endpoint is stable across multiple requests
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("order-service"));
        }
    }

    @Test
    void testReadinessEndpoint_MultipleRequests() throws Exception {
        // Test that readiness endpoint is stable across multiple requests
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/health/ready"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ready").value(true));
        }
    }

    @Test
    void testHealthEndpoint_MethodNotAllowed() throws Exception {
        // Test that only GET is allowed
        mockMvc.perform(post("/health"))
                .andExpect(status().isMethodNotAllowed());
        
        mockMvc.perform(put("/health"))
                .andExpect(status().isMethodNotAllowed());
        
        mockMvc.perform(delete("/health"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testReadinessEndpoint_MethodNotAllowed() throws Exception {
        // Test that only GET is allowed
        mockMvc.perform(post("/health/ready"))
                .andExpect(status().isMethodNotAllowed());
        
        mockMvc.perform(put("/health/ready"))
                .andExpect(status().isMethodNotAllowed());
        
        mockMvc.perform(delete("/health/ready"))
                .andExpect(status().isMethodNotAllowed());
    }
} 