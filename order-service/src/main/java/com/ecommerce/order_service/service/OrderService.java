package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public OrderResponse placeOrder(OrderRequest request) {

        // 1. Check user exists
        UserResponse user = webClient.get()
                .uri("http://localhost:8081/api/users/" + request.getUserId())
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();

        if (user == null) {
            throw new RuntimeException("User not found!");
        }

        // 2. Check product exists
        ProductResponse product = webClient.get()
                .uri("http://localhost:8082/api/products/" + request.getProductId())
                .retrieve()
                .bodyToMono(ProductResponse.class)
                .block();

        if (product == null) {
            throw new RuntimeException("Product not found!");
        }

        // 3. Place the order
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setQuantity(request.getQuantity());
        order.setStatus("PLACED");

        Order saved = orderRepository.save(order);

        // 4. Return response
        OrderResponse response = new OrderResponse();
        response.setId(saved.getId());
        response.setUserId(saved.getUserId());
        response.setProductId(saved.getProductId());
        response.setQuantity(saved.getQuantity());
        response.setStatus(saved.getStatus());
        return response;
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found!"));

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setProductId(order.getProductId());
        response.setQuantity(order.getQuantity());
        response.setStatus(order.getStatus());
        return response;
    }
}