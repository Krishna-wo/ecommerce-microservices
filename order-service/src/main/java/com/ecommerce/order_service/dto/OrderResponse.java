package com.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class OrderResponse {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private String status;
}