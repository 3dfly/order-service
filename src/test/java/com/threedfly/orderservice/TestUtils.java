package com.threedfly.orderservice;

import com.threedfly.orderservice.dto.ShippingAddress;

public class TestUtils {
    
    public static ShippingAddress createTestShippingAddress() {
        ShippingAddress address = new ShippingAddress();
        address.setStreet("123 Test St");
        address.setCity("Test City");
        address.setState("Test State");
        address.setZipCode("12345");
        address.setCountry("Test Country");
        return address;
    }
}