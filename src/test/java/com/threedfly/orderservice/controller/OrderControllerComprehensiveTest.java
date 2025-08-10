package com.threedfly.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.TestUtils;
import com.threedfly.orderservice.dto.CreateOrderRequest;
import com.threedfly.orderservice.dto.ShippingAddress;
import com.threedfly.orderservice.dto.UpdateOrderRequest;
import com.threedfly.orderservice.entity.Order;
import com.threedfly.orderservice.entity.OrderStatus;
import com.threedfly.orderservice.entity.Seller;
import com.threedfly.orderservice.repository.OrderRepository;
import com.threedfly.orderservice.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "printing.price.per-gram=0.05",
    "printing.price.per-minute=0.10",
    "printing.bambu.slicer.path=src/test/resources/test-mock-slicer.py",
    "printing.bambu.printer.config=/tmp/test-config.ini",
    "printing.temp.directory=/tmp/test-printing"
})
class OrderControllerComprehensiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SellerRepository sellerRepository;

    private CreateOrderRequest validOrderRequest;
    private Seller testSeller;
    private Order testOrder;

    @BeforeEach
    void setUp() throws Exception {
        orderRepository.deleteAll();
        sellerRepository.deleteAll();

        // Create test seller
        testSeller = new Seller();
        testSeller.setUserId(1001L);
        testSeller.setBusinessName("Test Store");
        testSeller.setBusinessAddress("123 Test St");
        testSeller.setContactEmail("test@store.com");
        testSeller.setContactPhone("+15550123");
        testSeller.setVerified(true);
        testSeller = sellerRepository.save(testSeller);

        // Create valid order request
        validOrderRequest = new CreateOrderRequest();
        validOrderRequest.setCustomerId(2001L);
        validOrderRequest.setProductId("PROD-1001");
        validOrderRequest.setQuantity(2);
        validOrderRequest.setStlFileUrl("https://example.com/model.stl");
        validOrderRequest.setShippingAddress(TestUtils.createTestShippingAddress());
        validOrderRequest.setSupplierId(3001L);
        validOrderRequest.setSellerId(testSeller.getId());

        // Create a test order for read/update/delete operations
        testOrder = new Order();
        testOrder.setCustomerId(2002L);
        testOrder.setProductId("PROD-1002");
        testOrder.setQuantity(1);
        testOrder.setStlFileUrl("https://example.com/another-model.stl");
        try {
            testOrder.setShippingAddress(objectMapper.writeValueAsString(TestUtils.createTestShippingAddress()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize shipping address", e);
        }
        testOrder.setSupplierId(3001L);
        testOrder.setSeller(testSeller);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder = orderRepository.save(testOrder);
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        String orderJson = objectMapper.writeValueAsString(validOrderRequest);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(2001L))
                .andExpect(jsonPath("$.productId").value("PROD-1001"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.stlFileUrl").value("https://example.com/model.stl"))
                .andExpect(jsonPath("$.shippingAddress.street").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderDate").exists());
    }

    @Test
    void testCreateOrder_ValidationFailure() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        // Missing required fields
        String invalidJson = objectMapper.writeValueAsString(invalidRequest);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllOrders_Success() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].productId").exists());
    }

    @Test
    void testGetOrderById_Success() throws Exception {
        mockMvc.perform(get("/orders/" + testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.productId").value("PROD-1002"))
                .andExpect(jsonPath("$.customerId").value(2002L));
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        mockMvc.perform(get("/orders/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetOrdersByCustomer_Success() throws Exception {
        mockMvc.perform(get("/orders/customer/" + testOrder.getCustomerId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].customerId").value(testOrder.getCustomerId()));
    }

    @Test
    void testGetOrdersByCustomer_EmptyResult() throws Exception {
        mockMvc.perform(get("/orders/customer/99999"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetOrdersByStatus_Success() throws Exception {
        mockMvc.perform(get("/orders/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[*].status").value(everyItem(equalTo("PENDING"))));
    }

    @Test
    void testGetOrdersByStatus_InvalidStatus() throws Exception {
        mockMvc.perform(get("/orders/status/INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateOrder_Success() throws Exception {
        ShippingAddress newAddress = new ShippingAddress();
        newAddress.setStreet("456 New St");
        newAddress.setCity("New City");
        newAddress.setState("New State");
        newAddress.setZipCode("54321");
        newAddress.setCountry("New Country");

        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setQuantity(5);
        updateRequest.setStlFileUrl("https://example.com/updated-model.stl");
        updateRequest.setShippingAddress(newAddress);

        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/orders/" + testOrder.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.stlFileUrl").value("https://example.com/updated-model.stl"))
                .andExpect(jsonPath("$.shippingAddress.street").value("456 New St"));
    }

    @Test
    void testUpdateOrder_NotFound() throws Exception {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setQuantity(5);
        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/orders/99999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateOrderStatus_Success() throws Exception {
        mockMvc.perform(patch("/orders/" + testOrder.getId() + "/status")
                .param("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void testUpdateOrderStatus_NotFound() throws Exception {
        mockMvc.perform(patch("/orders/99999/status")
                .param("status", "PROCESSING"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateOrderStatus_InvalidStatus() throws Exception {
        mockMvc.perform(patch("/orders/" + testOrder.getId() + "/status")
                .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteOrder_Success() throws Exception {
        mockMvc.perform(delete("/orders/" + testOrder.getId()))
                .andExpect(status().isNoContent());

        // Verify order is deleted
        mockMvc.perform(get("/orders/" + testOrder.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteOrder_NotFound() throws Exception {
        mockMvc.perform(delete("/orders/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCalculatePrice_Success() throws Exception {
        // Create a mock STL file
        MockMultipartFile mockStlFile = new MockMultipartFile(
            "stlFile", 
            "test-model.stl", 
            "application/octet-stream",
            createMockStlContent()
        );

        mockMvc.perform(multipart("/orders/calculate")
                .file(mockStlFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filename").value("test-model.stl"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalPrice").exists())
                .andExpect(jsonPath("$.weightGrams").exists())
                .andExpect(jsonPath("$.printingTimeMinutes").exists())
                .andExpect(jsonPath("$.pricePerGram").value(0.05))
                .andExpect(jsonPath("$.pricePerMinute").value(0.10));
    }

    @Test
    void testCalculatePrice_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "stlFile", 
            "empty.stl", 
            "application/octet-stream",
            new byte[0]
        );

        mockMvc.perform(multipart("/orders/calculate")
                .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void testCalculatePrice_InvalidFileType() throws Exception {
        MockMultipartFile invalidFile = new MockMultipartFile(
            "stlFile", 
            "test.txt", 
            "text/plain",
            "This is not an STL file".getBytes()
        );

        mockMvc.perform(multipart("/orders/calculate")
                .file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void testCalculatePrice_MissingFile() throws Exception {
        mockMvc.perform(post("/orders/calculate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidHttpMethods() throws Exception {
        mockMvc.perform(post("/orders/" + testOrder.getId()))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/orders"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/orders/calculate"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 405 || status == 400;
                });
    }

    @Test
    void testCompleteOrderWorkflow() throws Exception {
        // 1. Create order
        String orderJson = objectMapper.writeValueAsString(validOrderRequest);
        String createResponse = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract order ID from response
        var orderResponse = objectMapper.readTree(createResponse);
        Long orderId = orderResponse.get("id").asLong();

        // 2. Update order status to PROCESSING
        mockMvc.perform(patch("/orders/" + orderId + "/status")
                .param("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        // 3. Update order details
        ShippingAddress newAddress = new ShippingAddress();
        newAddress.setStreet("456 New St");
        newAddress.setCity("New City");
        newAddress.setState("New State");
        newAddress.setZipCode("54321");
        newAddress.setCountry("New Country");

        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setQuantity(3);
        updateRequest.setStlFileUrl("https://example.com/updated-model.stl");
        updateRequest.setShippingAddress(newAddress);

        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/orders/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.stlFileUrl").value("https://example.com/updated-model.stl"))
                .andExpect(jsonPath("$.shippingAddress.street").value("456 New St"));

        // 4. Complete order
        mockMvc.perform(patch("/orders/" + orderId + "/status")
                .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 5. Verify final state
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.stlFileUrl").value("https://example.com/updated-model.stl"))
                .andExpect(jsonPath("$.shippingAddress.street").value("456 New St"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    private byte[] createMockStlContent() {
        // Create a minimal valid STL file content for testing
        String stlContent = """
            solid test
              facet normal 0.0 0.0 1.0
                outer loop
                  vertex 0.0 0.0 0.0
                  vertex 1.0 0.0 0.0
                  vertex 0.0 1.0 0.0
                endloop
              endfacet
            endsolid test
            """;
        return stlContent.getBytes();
    }
}