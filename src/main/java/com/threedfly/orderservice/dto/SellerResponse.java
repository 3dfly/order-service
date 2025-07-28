package com.threedfly.orderservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
    private Long id;
    private Long userId;
    private String businessName;
    private String businessAddress;
    private String contactPhone;
    private String contactEmail;
    private boolean verified;
    private List<Long> productIds;
    private List<Long> shopIds;
} 