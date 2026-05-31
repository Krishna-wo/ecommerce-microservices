package com.ecommerce.api_gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // These paths don't need token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/register",
            "/api/users/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // If public path - skip token check
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Check Authorization header exists
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract token from "Bearer <token>"
        String token = authHeader.substring(7);

        // Validate token
        if (!jwtUtil.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Token valid - add email to header and forward request
        String email = jwtUtil.extractEmail(token);
        exchange.getRequest().mutate()
                .header("X-User-Email", email)
                .build();

        return chain.filter(exchange);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(path::equals);
    }

    @Override
    public int getOrder() {
        return -1; // Run this filter FIRST before anything else
    }
}