package com.threedfly.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.threedfly.orderservice.dto.CreateOrderRequest;
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
    void setUp() {
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
        validOrderRequest.setCustomerName("John Doe");
        validOrderRequest.setCustomerEmail("john@example.com");
        validOrderRequest.setProductId(1001L);
        validOrderRequest.setQuantity(2);
        validOrderRequest.setTotalPrice(199.99);
        validOrderRequest.setShippingAddress("456 Customer Ave, City");
        validOrderRequest.setSupplierId(3001L);
        validOrderRequest.setSellerId(testSeller.getId());

        // Create a test order for read/update/delete operations
        testOrder = new Order();
        testOrder.setCustomerId(2002L);
        testOrder.setCustomerName("Jane Smith");
        testOrder.setCustomerEmail("jane@example.com");
        testOrder.setProductId(1002L);
        testOrder.setQuantity(1);
        testOrder.setTotalPrice(99.99);
        testOrder.setShippingAddress("789 Another St, City");
        testOrder.setSupplierId(3001L);
        testOrder.setSeller(testSeller);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder = orderRepository.save(testOrder);
    }

    // Create Order Tests
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
                .andExpect(jsonPath("$.customerName").value("John Doe"))
                .andExpect(jsonPath("$.customerEmail").value("john@example.com"))
                .andExpect(jsonPath("$.productId").value(1001L))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(199.99))
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

    // Get All Orders Tests
    @Test
    void testGetAllOrders_Success() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].customerName").exists());
    }

    // Get Order by ID Tests
    @Test
    void testGetOrderById_Success() throws Exception {
        mockMvc.perform(get("/orders/" + testOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.customerName").value("Jane Smith"))
                .andExpect(jsonPath("$.customerId").value(2002L));
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        mockMvc.perform(get("/orders/99999"))
                .andExpect(status().isNotFound());
    }

    // Get Orders by Customer Tests
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

    // Get Orders by Status Tests
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

    // Update Order Tests
    @Test
    void testUpdateOrder_Success() throws Exception {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setQuantity(5);
        updateRequest.setTotalPrice(299.99);
        updateRequest.setShippingAddress("Updated Address");

        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/orders/" + testOrder.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.totalPrice").value(299.99))
                .andExpect(jsonPath("$.shippingAddress").value("Updated Address"));
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

    // Update Order Status Tests
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

    // Delete Order Tests
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

    // Printing Calculation Tests
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

    // HTTP Method Tests
    @Test
    void testInvalidHttpMethods() throws Exception {
        // Test invalid methods on various endpoints
        mockMvc.perform(post("/orders/" + testOrder.getId()))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/orders"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(get("/orders/calculate"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Accept either 405 (Method Not Allowed) or 400 (Bad Request)
                    assert status == 405 || status == 400;
                });
    }

    // Integration Tests - Complex Workflows
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
        UpdateOrderRequest updateRequest = new UpdateOrderRequest();
        updateRequest.setQuantity(3);
        updateRequest.setTotalPrice(249.99);
        String updateJson = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/orders/" + orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(249.99));

        // 4. Complete order
        mockMvc.perform(patch("/orders/" + orderId + "/status")
                .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 5. Verify final state
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.totalPrice").value(249.99))
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