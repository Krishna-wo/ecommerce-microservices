package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public OrderService(OrderRepository orderRepository, WebClient webClient) {
        this.orderRepository = orderRepository;
        this.webClient = webClient;
    }

    // @CircuitBreaker tells Spring:
    // "Watch this method! If it fails too many times,
    //  open the circuit and call fallbackPlaceOrder instead!"
    // name must match what we wrote in application.properties
    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackPlaceOrder")
    public OrderResponse placeOrder(OrderRequest request) {
        try {
            // Call user-service
            UserResponse user = webClient.get()
                    .uri("http://localhost:8081/api/users/" + request.getUserId())
                    .retrieve()
                    .bodyToMono(UserResponse.class)
                    .block();

            if (user == null) {
                throw new RuntimeException("User not found!");
            }

            // Call product-service
            ProductResponse product = webClient.get()
                    .uri("http://localhost:8082/api/products/" + request.getProductId())
                    .retrieve()
                    .bodyToMono(ProductResponse.class)
                    .block();

            if (product == null) {
                throw new RuntimeException("Product not found!");
            }

            // Save order
            Order order = new Order();
            order.setUserId(request.getUserId());
            order.setProductId(request.getProductId());
            order.setQuantity(request.getQuantity());
            order.setStatus("PLACED");

            Order saved = orderRepository.save(order);

            OrderResponse response = new OrderResponse();
            response.setId(saved.getId());
            response.setUserId(saved.getUserId());
            response.setProductId(saved.getProductId());
            response.setQuantity(saved.getQuantity());
            response.setStatus(saved.getStatus());
            return response;

        } catch (Exception e) {
            // Rethrow so circuit breaker catches it!
            throw new RuntimeException("Failed to place order: " + e.getMessage());
        }
    }

    // This method is called automatically when circuit is OPEN
    // It must have same parameters + extra Throwable parameter
    // Throwable tells us what error caused the circuit to open
    public OrderResponse fallbackPlaceOrder(OrderRequest request, Throwable throwable) {
        System.out.println("Circuit is OPEN! Reason: " + throwable.getMessage());

        // Return a friendly response instead of crashing!
        OrderResponse response = new OrderResponse();
        response.setId(-1L);
        response.setUserId(request.getUserId());
        response.setProductId(request.getProductId());
        response.setQuantity(request.getQuantity());
        response.setStatus("SERVICE_UNAVAILABLE - Please try again later!");
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