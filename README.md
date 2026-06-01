# 📚 E-Commerce Microservices - Complete Documentation

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Why Microservices?](#3-why-microservices)
4. [Service Discovery - Eureka](#4-service-discovery---eureka)
5. [API Gateway](#5-api-gateway)
6. [JWT Authentication](#6-jwt-authentication)
7. [Circuit Breaker](#7-circuit-breaker)
8. [Config Server](#8-config-server)
9. [Docker & Docker Compose](#9-docker--docker-compose)
10. [Service Details](#10-service-details)
11. [Database Design](#11-database-design)
12. [API Reference](#12-api-reference)
13. [How Everything Works Together](#13-how-everything-works-together)

---

## 1. Project Overview

This project is a **production-ready e-commerce backend** built using microservices architecture. Instead of building one big application (monolith), we split it into small independent services that each handle one specific job.

### Tech Stack
| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Programming language |
| Spring Boot | 3.2.5 | Framework for building services |
| Spring Cloud | 2023.0.0 | Tools for microservices |
| Spring Cloud Gateway | 4.1.0 | API Gateway |
| Spring Cloud Netflix Eureka | 4.1.0 | Service Discovery |
| Spring Cloud Config | 4.1.0 | Centralized Configuration |
| Spring Security | 6.2.4 | Security framework |
| JWT (jjwt) | 0.11.5 | Authentication tokens |
| Resilience4j | 2.2.0 | Circuit Breaker |
| MySQL | 8 | Database |
| Docker | Latest | Containerization |
| Docker Compose | Latest | Multi-container management |

---

## 2. Architecture

```
[Client/Postman/Frontend]
         │
         │ ALL requests go through port 8080
         ▼
┌─────────────────────┐
│     API Gateway     │  port: 8080
│  - JWT Validation   │  - Checks token on every request
│  - Routing          │  - Routes to correct service
│  - Load Balancing   │  - lb://SERVICE-NAME
└────────┬────────────┘
         │
         │ Routes based on path
         ├──────────────────────────────────┐
         │                  │               │
/api/users/**      /api/products/**    /api/orders/**
         │                  │               │
         ▼                  ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ user-service │  │product-service│  │ order-service│
│  port: 8081  │  │  port: 8082  │  │  port: 8083  │
│              │  │              │  │  calls user  │
│  - register  │  │  - add       │  │  calls prod  │
│  - login     │  │  - list      │  │  - place ord │
│  - get user  │  │  - get prod  │  │  - get order │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                  │
       ▼                 ▼                  ▼
┌──────────────────────────────────────────────────┐
│                  MySQL Database                   │
│    userdb           productdb         orderdb     │
└──────────────────────────────────────────────────┘

Supporting Services:
┌─────────────────┐     ┌─────────────────┐
│ Discovery Server│     │  Config Server  │
│   port: 8761    │     │   port: 8888    │
│                 │     │                 │
│ - Service reg.  │     │ - Reads GitHub  │
│ - Service disco │     │ - Gives config  │
│ - Health check  │     │   to services   │
└─────────────────┘     └─────────────────┘
```

---

## 3. Why Microservices?

### Monolith vs Microservices

**Monolith (Old way):**
```
One BIG application
Everything in one codebase
One database
Deploy everything together

Problems:
→ One bug can crash everything
→ Hard to scale specific parts
→ Team conflicts on same codebase
→ Slow deployment
→ Technology locked (all Java or all Python)
```

**Microservices (Our way):**
```
Multiple SMALL applications
Each handles ONE job
Own database per service
Deploy independently

Benefits:
→ One service fails, others keep running ✅
→ Scale only what needs scaling ✅
→ Teams work independently ✅
→ Fast deployment ✅
→ Mix technologies ✅
```

### Real World Example
```
Amazon has 500+ microservices!
- Product service
- Payment service  
- Recommendation service
- Shipping service
- Review service
...and more!

Each team owns their service.
If payment service has bug,
product browsing still works!
```

---

## 4. Service Discovery - Eureka

### What is it?
Eureka is a **service registry** - like a phone book for microservices.

### Why do we need it?
```
WITHOUT Eureka:
order-service calls user-service like this:
http://localhost:8081/api/users/1

Problems:
→ What if user-service moves to port 9090?
→ What if user-service moves to different server?
→ What if we run 3 instances of user-service?
→ You have to manually update all URLs! 😱

WITH Eureka:
order-service calls: lb://USER-SERVICE

Eureka knows where USER-SERVICE is!
You never need to change the URL! ✅
```

### How it works
```
Step 1: Every service starts
        → registers itself with Eureka
        → "I am USER-SERVICE at 172.20.10.5:8081"

Step 2: Services send heartbeat every 30 seconds
        → "I am still alive!"

Step 3: When order-service needs user-service
        → asks Eureka: "Where is USER-SERVICE?"
        → Eureka replies: "172.20.10.5:8081"
        → order-service connects there!

Step 4: If service dies
        → stops sending heartbeat
        → Eureka removes it from registry
        → other services get updated list
```

### Why discovery-server has NO Eureka client
```
discovery-server IS Eureka!

It's like asking:
"Why isn't the phone book listed in the phone book?"

That's why in discovery-server application.properties:
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

### Implementation
**discovery-server/application.properties:**
```properties
spring.application.name=discovery-server
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

**Every other service application.properties:**
```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

**Every other service pom.xml:**
```xml

    org.springframework.cloud
    spring-cloud-starter-netflix-eureka-client

```

---

## 5. API Gateway

### What is it?
API Gateway is the **single entry point** for all client requests.

### Why do we need it?
```
WITHOUT Gateway:
Client needs to know ALL service URLs:
→ http://localhost:8081 for users
→ http://localhost:8082 for products
→ http://localhost:8083 for orders

Problems:
→ Client manages multiple URLs
→ Security logic in every service
→ No central place for auth, logging, rate limiting

WITH Gateway:
Client knows ONE URL: http://localhost:8080
Gateway routes to correct service automatically!

Benefits:
→ Single URL for client ✅
→ JWT validation in ONE place ✅
→ Load balancing ✅
→ Easy to add logging, rate limiting ✅
```

### How routing works
```
Request: POST http://localhost:8080/api/users/register
         │
         ▼
Gateway checks routes:
→ Path starts with /api/users/** ?
→ YES! Route to USER-SERVICE
         │
         ▼
lb://USER-SERVICE
→ asks Eureka: "Where is USER-SERVICE?"
→ Eureka: "172.20.10.5:8081"
→ Gateway forwards to: http://172.20.10.5:8081/api/users/register
```

### What is lb:// ?
```
lb = Load Balancer

lb://USER-SERVICE means:
→ Find USER-SERVICE in Eureka
→ If multiple instances running,
  distribute requests between them!

Round Robin distribution:
Request 1 → Instance 1 (port 8081)
Request 2 → Instance 2 (port 8085)
Request 3 → Instance 3 (port 8089)
Request 4 → Instance 1 again...
```

### Implementation
**api-gateway/application.properties:**
```properties
spring.application.name=api-gateway
server.port=8080

eureka.client.service-url.defaultZone=http://localhost:8761/eureka

jwt.secret=ecommerce-super-secret-key-for-jwt-signing-2024

spring.cloud.gateway.routes[0].id=user-service
spring.cloud.gateway.routes[0].uri=lb://USER-SERVICE
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/users/**

spring.cloud.gateway.routes[1].id=product-service
spring.cloud.gateway.routes[1].uri=lb://PRODUCT-SERVICE
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/products/**

spring.cloud.gateway.routes[2].id=order-service
spring.cloud.gateway.routes[2].uri=lb://ORDER-SERVICE
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/orders/**
```

### AuthFilter - JWT Validation
```java
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // PUBLIC_PATHS = paths that don't need token
    // register and login don't need token
    // because user needs to login to GET a token!
    private static final List PUBLIC_PATHS = List.of(
            "/api/users/register",
            "/api/users/login"
    );

    @Override
    public Mono filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        
        // 1. Get the request path
        String path = exchange.getRequest().getURI().getPath();

        // 2. Is it a public path? Skip token check!
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 3. Get Authorization header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        // 4. No token? Return 401!
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 5. Extract token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // 6. Invalid token? Return 401!
        if (!jwtUtil.validateToken(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 7. Valid! Add email to header and forward
        String email = jwtUtil.extractEmail(token);
        exchange.getRequest().mutate()
                .header("X-User-Email", email)
                .build();

        return chain.filter(exchange);
    }

    // getOrder() = -1 means this filter runs FIRST
    // before all other filters
    @Override
    public int getOrder() {
        return -1;
    }
}
```

---

## 6. JWT Authentication

### What is JWT?
JWT = JSON Web Token

A token that proves who you are without storing session data on server.

### JWT Structure
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrcmlzaEBnbWFpbC5jb20ifQ.abc123

Three parts separated by dots:

Part 1 - HEADER (base64 encoded):
{
  "alg": "HS256"  ← algorithm used to sign
}

Part 2 - PAYLOAD (base64 encoded):
{
  "sub": "krish@gmail.com",  ← who is this?
  "iat": 1780214464,         ← when was this created?
  "exp": 1780300864          ← when does it expire?
}

Part 3 - SIGNATURE:
HMACSHA256(
  base64(header) + "." + base64(payload),
  secret
)
← proves token is genuine, not tampered!
```

### Why JWT instead of sessions?
```
Session (old way):
→ Server stores session in memory/database
→ Every request: server looks up session
→ Problem: if you have 10 servers, which has the session?
→ Need sticky sessions or shared session store 😱

JWT (new way):
→ Server stores NOTHING
→ Token contains all info
→ Server just validates token signature
→ Works with any server, perfectly scalable! ✅
```

### Full Authentication Flow
```
REGISTRATION:
Client → POST /api/users/register
         {name, email, password}
         ↓
user-service saves to MySQL
         ↓
Returns {id, name, email}
(no token yet - just created account)

LOGIN:
Client → POST /api/users/login
         {email, password}
         ↓
user-service finds user by email
         ↓
Checks password matches
         ↓
Generates JWT token with email inside
         ↓
Returns {token, email, name}

USING PROTECTED API:
Client → POST /api/orders
         Headers: Authorization: Bearer eyJhbG...
         Body: {userId, productId, quantity}
         ↓
API Gateway AuthFilter runs FIRST
         ↓
Extracts token from header
         ↓
Validates token (checks signature, expiry)
         ↓
VALID → adds X-User-Email header → forwards to order-service
INVALID → returns 401 Unauthorized
```

### Why same secret in user-service AND api-gateway?
```
user-service CREATES token using secret to SIGN it
api-gateway VALIDATES token using same secret to VERIFY it

If secrets are different:
→ gateway can't verify tokens created by user-service
→ everything breaks!

Like a stamp:
→ user-service stamps documents with stamp A
→ api-gateway verifies stamps using stamp A
→ Must be SAME stamp!
```

### Implementation

**JwtUtil.java (in user-service):**
```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate token with email as subject
    // Expires in 24 hours (86400000 ms)
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token - checks signature and expiry
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false; // expired, invalid signature, etc.
        }
    }

    // Extract email from token payload
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
```

**application.properties:**
```properties
jwt.secret=ecommerce-super-secret-key-for-jwt-signing-2024
jwt.expiration=86400000
```

---

## 7. Circuit Breaker

### What is it?
Circuit Breaker prevents **cascading failures** - when one service failure brings down the entire system.

### The Problem (Cascading Failure)
```
user-service CRASHES ❌
         ↓
order-service calls user-service
         ↓
WAITS... 30 seconds timeout...
         ↓
100 users place orders simultaneously
         ↓
100 threads stuck waiting!
         ↓
order-service runs out of memory
         ↓
order-service CRASHES ❌
         ↓
api-gateway calls order-service
         ↓
api-gateway CRASHES ❌
         ↓
ENTIRE SYSTEM DOWN! 💀
All because ONE service crashed!
```

### The Solution (Circuit Breaker)
```
user-service CRASHES ❌
         ↓
order-service calls user-service
         ↓
FAILS 2 times (minimum-number-of-calls=2)
         ↓
Failure rate hits 50% threshold
         ↓
CIRCUIT OPENS! 🔴
         ↓
100 users place orders
         ↓
Circuit is OPEN → don't even call user-service!
→ return fallback immediately!
         ↓
Returns: "SERVICE_UNAVAILABLE - Please try again later!"
         ↓
order-service STAYS HEALTHY ✅
System STAYS STABLE ✅
```

### Three States
```
🟢 CLOSED (Normal)
→ Everything working fine
→ Requests pass through normally
→ Counting failures in background

🔴 OPEN (Problem detected)
→ Too many failures happened
→ Don't even try calling failed service
→ Return fallback immediately
→ Wait 10 seconds (wait-duration-in-open-state)

🟡 HALF-OPEN (Testing recovery)
→ After 10 seconds, try again
→ Allow 3 test requests (permitted-number-of-calls-in-half-open-state)
→ All succeed? → back to CLOSED 🟢
→ Any fail? → back to OPEN 🔴
```

### Why only in order-service?
```
Rule: Circuit Breaker goes in the CALLER service
      not in the service being called!

user-service   → calls nobody → NO circuit breaker needed
product-service → calls nobody → NO circuit breaker needed
order-service  → calls user-service AND product-service
               → NEEDS circuit breaker here!
```

### Implementation

**order-service/application.properties:**
```properties
resilience4j.circuitbreaker.instances.user-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.user-service.minimum-number-of-calls=2
resilience4j.circuitbreaker.instances.user-service.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.user-service.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.user-service.sliding-window-size=4
```

**OrderService.java:**
```java
// @CircuitBreaker watches this method
// If it fails too much → opens circuit → calls fallback
@CircuitBreaker(name = "user-service", fallbackMethod = "fallbackPlaceOrder")
public OrderResponse placeOrder(OrderRequest request) {
    // Call user-service - circuit breaker watches this!
    UserResponse user = webClient.get()
            .uri("http://localhost:8081/api/users/" + request.getUserId())
            .retrieve()
            .bodyToMono(UserResponse.class)
            .block();
    // ... rest of logic
}

// Called automatically when circuit OPENS
// Must have same params + Throwable
public OrderResponse fallbackPlaceOrder(OrderRequest request, Throwable throwable) {
    OrderResponse response = new OrderResponse();
    response.setId(-1L);
    response.setStatus("SERVICE_UNAVAILABLE - Please try again later!");
    return response;
}
```

---

## 8. Config Server

### What is it?
Config Server is a **centralized configuration management** system.

### The Problem
```
WITHOUT Config Server:
Each service has its OWN application.properties

user-service/application.properties:
spring.datasource.url=jdbc:mysql://localhost:3307/userdb
spring.datasource.password=root

product-service/application.properties:
spring.datasource.url=jdbc:mysql://localhost:3307/productdb
spring.datasource.password=root

order-service/application.properties:
spring.datasource.url=jdbc:mysql://localhost:3307/orderdb
spring.datasource.password=root

If database password changes:
→ Update 3 files
→ Restart 3 services 😱

With 50 microservices?
→ Update 50 files
→ Restart 50 services 💀
```

### The Solution
```
WITH Config Server:
All configs stored on GitHub!

GitHub Repo (ecommerce-config):
├── user-service.properties
├── product-service.properties
└── order-service.properties

Config Server reads from GitHub
Services fetch config from Config Server on startup!

If database password changes:
→ Update ONE file on GitHub
→ All services get new config! ✅
No restart needed! 🔥
```

### How it works
```
Step 1: Config Server starts
        → reads GitHub repo

Step 2: user-service starts
        → bootstrap.properties loads FIRST
        → sees: spring.cloud.config.uri=http://localhost:8888
        → calls: http://localhost:8888/user-service/default
        → Config Server reads user-service.properties from GitHub
        → returns all config to user-service
        → user-service starts with that config! ✅

Step 3: If config changes on GitHub
        → Config Server picks it up
        → Services can refresh without restart!
```

### Why bootstrap.properties?
```
Normal application.properties loads LATE
→ Spring context already starting
→ Too late to fetch external config!

bootstrap.properties loads FIRST
→ Before Spring context starts
→ Perfect time to fetch config from server!

Think of it like:
bootstrap.properties = alarm clock (wakes up first)
application.properties = morning routine (runs after)
```

### Implementation

**config-server/application.properties:**
```properties
spring.application.name=config-server
server.port=8888

eureka.client.service-url.defaultZone=http://localhost:8761/eureka

spring.cloud.config.server.git.uri=https://github.com/Krishna-wo/ecommerce-config
spring.cloud.config.server.git.default-label=main
spring.cloud.config.server.git.clone-on-start=true
spring.cloud.config.server.git.force-pull=true
```

**ConfigServerApplication.java:**
```java
@SpringBootApplication
@EnableConfigServer  // ← This makes it a Config Server!
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**Each service bootstrap.properties:**
```properties
spring.application.name=user-service
spring.cloud.config.uri=http://localhost:8888
spring.cloud.config.fail-fast=true
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

**Each service application.properties (only port):**
```properties
server.port=8081
```

**GitHub Config Repo files:**
user-service.properties:
```properties
spring.datasource.url=jdbc:mysql://localhost:3307/userdb
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
jwt.secret=ecommerce-super-secret-key-for-jwt-signing-2024
jwt.expiration=86400000
```

---

## 9. Docker & Docker Compose

### What is Docker?
Docker packages your application + everything it needs into a **container**.

```
WITHOUT Docker:
"Works on my machine!" problem
→ Your laptop: Java 21, MySQL 8 → works ✅
→ Friend's laptop: Java 17, MySQL 5 → breaks ❌

WITH Docker:
Container has everything inside:
→ Java 21
→ Your application
→ All dependencies
Runs the SAME everywhere! ✅
```

### Docker Vocabulary
```
Image    = Recipe/Blueprint
           (like a .exe installer)

Container = Running instance of an Image
            (like the installed + running app)

Dockerfile = Instructions to build an Image
             (like a recipe card)

docker-compose = Tool to run MULTIPLE containers
                 with one command
```

### Why Docker for our project?
```
Without Docker:
→ Install Java 21 on every machine
→ Install MySQL on every machine
→ Configure MySQL manually
→ Run 6 mvn spring-boot:run commands
→ 6 terminals open! 😱

With Docker:
→ docker-compose up -d
→ DONE! Everything running! ✅
→ No installation needed
→ Same on every machine
```

### Dockerfile Explained
```dockerfile
# Start with Java 21 pre-installed
FROM eclipse-temurin:21-jre-alpine

# Set working directory inside container
WORKDIR /app

# Copy compiled jar from target/ into container
COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar

# Tell Docker this container uses port 8081
EXPOSE 8081

# Command to run when container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Why alpine?**
```
eclipse-temurin:21        = 400MB image
eclipse-temurin:21-alpine = 80MB image

Alpine is a tiny Linux distro!
Smaller image = faster download = less storage!
```

### Docker Compose Explained

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8              # Use official MySQL 8 image
    container_name: mysql-db    # Name of container
    environment:
      MYSQL_ROOT_PASSWORD: root # Set root password
      MYSQL_DATABASE: userdb    # Auto-create database
    ports:
      - "3307:3306"             # HOST:CONTAINER port mapping
    volumes:
      - mysql-data:/var/lib/mysql  # Persist data!
    networks:
      - ecommerce-network       # All services in same network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping"] # How to check if healthy
      interval: 10s             # Check every 10 seconds
      retries: 5                # Try 5 times before giving up

  user-service:
    build: ./user-service       # Build from Dockerfile in this folder
    container_name: user-service
    ports:
      - "8081:8081"
    environment:
      # Override application.properties values!
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/userdb
      # Notice: mysql (container name) not localhost!
    depends_on:
      mysql:
        condition: service_healthy  # Wait for MySQL to be ready!
      config-server:
        condition: service_healthy  # Wait for Config Server too!
    networks:
      - ecommerce-network

networks:
  ecommerce-network:
    driver: bridge    # All containers can talk to each other!

volumes:
  mysql-data:         # Named volume - data persists!
```

### Why container name instead of localhost?
```
On your laptop:
MySQL runs at localhost:3307

Inside Docker network:
MySQL container is named "mysql"
Other containers reach it at mysql:3306

SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/userdb
                                   ^^^^^ container name!

Docker's internal DNS resolves "mysql" to MySQL container's IP!
```

### depends_on with healthcheck
```
Without depends_on:
→ All containers start at same time
→ user-service starts before MySQL ready
→ user-service can't connect to MySQL
→ user-service CRASHES! 😱

With depends_on + healthcheck:
→ MySQL starts first
→ Docker checks: is MySQL healthy?
→ MySQL ready? YES!
→ Now start user-service
→ user-service connects successfully! ✅
```

### Useful Docker Commands
```bash
# Start all services in background
docker-compose up -d

# Stop all services
docker-compose down

# Stop and remove volumes (deletes data!)
docker-compose down -v

# Check running containers
docker ps

# Check logs of specific service
docker logs user-service
docker logs user-service -f  # Follow live logs

# Restart specific service
docker-compose restart user-service

# Rebuild and restart
docker-compose up -d --build user-service

# Enter container shell
docker exec -it user-service /bin/sh
```

---

## 10. Service Details

### user-service

**Purpose:** Handles all user operations
**Port:** 8081
**Database:** userdb

**Layers:**
```
controller/ → handles HTTP requests
service/    → business logic
repository/ → database operations
entity/     → User table definition
dto/        → Data Transfer Objects (request/response shapes)
```

**Files:**
```
UserController.java   → REST endpoints
UserService.java      → Business logic + JWT generation
UserRepository.java   → JPA repository (extends JpaRepository)
User.java             → Entity (maps to users table)
RegisterRequest.java  → Input for registration
UserResponse.java     → Output (no password!)
LoginRequest.java     → Input for login
LoginResponse.java    → Output with JWT token
JwtUtil.java          → Generate + validate JWT tokens
SecurityConfig.java   → Disable Spring Security defaults
```

**Why SecurityConfig?**
```
When you add spring-boot-starter-security,
Spring LOCKS all endpoints with HTTP Basic Auth!

You'd need username/password for every request!

SecurityConfig disables this:
.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

Gateway handles security instead!
```

### product-service

**Purpose:** Handles product catalog
**Port:** 8082
**Database:** productdb

**Files:**
```
ProductController.java  → REST endpoints
ProductService.java     → Business logic
ProductRepository.java  → JPA repository
Product.java            → Entity (maps to products table)
ProductRequest.java     → Input for adding product
ProductResponse.java    → Output
```

### order-service

**Purpose:** Handles order placement
**Port:** 8083
**Database:** orderdb

**Special:** Calls user-service and product-service!

**Files:**
```
OrderController.java    → REST endpoints
OrderService.java       → Business logic + Circuit Breaker
OrderRepository.java    → JPA repository
Order.java              → Entity (maps to orders table)
OrderRequest.java       → Input
OrderResponse.java      → Output
UserResponse.java       → Shape of user-service response
ProductResponse.java    → Shape of product-service response
WebClientConfig.java    → Creates WebClient bean
```

**Why WebClient?**
```
WebClient = HTTP client for calling other services

order-service needs to call:
→ user-service to verify user exists
→ product-service to verify product exists

WebClient makes these HTTP calls!

WebClient is reactive (non-blocking)
Better than RestTemplate (blocking) for microservices!
```

### discovery-server

**Purpose:** Service registry (Eureka)
**Port:** 8761

**Special:** NO Eureka client! It IS the Eureka server!

### config-server

**Purpose:** Centralized configuration
**Port:** 8888

**Special:** Reads config from GitHub, serves to all services

### api-gateway

**Purpose:** Single entry point, JWT validation, routing
**Port:** 8080

**Files:**
```
ApiGatewayApplication.java  → Main class
AuthFilter.java             → JWT validation filter
JwtUtil.java                → Token validation
application.properties      → Routes configuration
```

---

## 11. Database Design

### userdb - users table
```sql
CREATE TABLE users (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);
```

### productdb - products table
```sql
CREATE TABLE products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    price       DOUBLE NOT NULL,
    quantity    INT NOT NULL
);
```

### orderdb - orders table
```sql
CREATE TABLE orders (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NOT NULL,
    status     VARCHAR(255) NOT NULL
);
```

**Why separate databases?**
```
Microservices rule:
Each service owns its own data!

If order-service directly queries userdb:
→ Tight coupling between services
→ If userdb changes, order-service breaks!
→ Defeats purpose of microservices!

Instead:
order-service calls user-service API
→ user-service returns user data
→ Loose coupling! ✅
→ user-service can change its database freely!
```

---

## 12. API Reference

### Base URL
```
http://localhost:8080
```
All requests go through API Gateway!

### Authentication
```
No auth needed:
→ POST /api/users/register
→ POST /api/users/login

Auth needed (add header):
→ Authorization: Bearer <token>
```

### User Endpoints

**Register:**
```
POST /api/users/register
Body:
{
    "name": "Krish",
    "email": "krish@gmail.com",
    "password": "1234"
}
Response 200:
{
    "id": 1,
    "name": "Krish",
    "email": "krish@gmail.com"
}
```

**Login:**
```
POST /api/users/login
Body:
{
    "email": "krish@gmail.com",
    "password": "1234"
}
Response 200:
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "krish@gmail.com",
    "name": "Krish"
}
```

**Get User:**
```
GET /api/users/1
Headers: Authorization: Bearer <token>
Response 200:
{
    "id": 1,
    "name": "Krish",
    "email": "krish@gmail.com"
}
```

### Product Endpoints

**Add Product:**
```
POST /api/products
Headers: Authorization: Bearer <token>
Body:
{
    "name": "iPhone 15",
    "description": "Latest Apple smartphone",
    "price": 999.99,
    "quantity": 50
}
Response 200:
{
    "id": 1,
    "name": "iPhone 15",
    "description": "Latest Apple smartphone",
    "price": 999.99,
    "quantity": 50
}
```

**Get All Products:**
```
GET /api/products
Headers: Authorization: Bearer <token>
Response 200:
[
    {
        "id": 1,
        "name": "iPhone 15",
        ...
    }
]
```

**Get Product by ID:**
```
GET /api/products/1
Headers: Authorization: Bearer <token>
Response 200:
{
    "id": 1,
    "name": "iPhone 15",
    ...
}
```

### Order Endpoints

**Place Order:**
```
POST /api/orders
Headers: Authorization: Bearer <token>
Body:
{
    "userId": 1,
    "productId": 1,
    "quantity": 2
}
Response 200:
{
    "id": 1,
    "userId": 1,
    "productId": 1,
    "quantity": 2,
    "status": "PLACED"
}

If service unavailable (Circuit Breaker):
{
    "id": -1,
    "status": "SERVICE_UNAVAILABLE - Please try again later!"
}
```

---

## 13. How Everything Works Together

### Complete Request Flow - Place Order

```
1. User logs in:
   POST /api/users/login
   → Returns JWT token

2. User places order:
   POST /api/orders
   Headers: Authorization: Bearer eyJhbG...
   Body: {userId:1, productId:1, quantity:2}

3. Request hits API Gateway (port 8080)

4. AuthFilter runs FIRST (order=-1):
   → Path is /api/orders
   → Not a public path
   → Check Authorization header
   → "Bearer eyJhbG..." found
   → Extract token: "eyJhbG..."
   → Validate token → VALID!
   → Extract email: "krish@gmail.com"
   → Add header: X-User-Email: krish@gmail.com
   → Forward request!

5. Gateway checks routes:
   → Path /api/orders/** matches ORDER-SERVICE route
   → lb://ORDER-SERVICE
   → Ask Eureka: "Where is ORDER-SERVICE?"
   → Eureka: "172.20.10.5:8083"
   → Forward to http://172.20.10.5:8083/api/orders

6. OrderController receives request:
   → Calls orderService.placeOrder(request)

7. Circuit Breaker checks:
   → Is user-service healthy? YES!
   → Allow request to pass

8. OrderService calls user-service:
   → WebClient GET http://localhost:8081/api/users/1
   → Gets user data back

9. OrderService calls product-service:
   → WebClient GET http://localhost:8082/api/products/1
   → Gets product data back

10. OrderService saves order:
    → Order saved to orderdb with status "PLACED"

11. Response flows back:
    → order-service → gateway → client
    → {id:4, userId:1, productId:1, quantity:2, status:"PLACED"}
```

### Startup Order
```
1. discovery-server FIRST
   → Must be ready before others register

2. config-server SECOND
   → Must be ready before services fetch config

3. user-service, product-service, order-service
   → Fetch config from config-server
   → Register with Eureka

4. api-gateway LAST
   → Must know where all services are
   → Starts routing after all services registered
```

### What happens when a service crashes?

```
user-service crashes:

1. user-service stops sending heartbeat to Eureka
2. Eureka removes user-service from registry
3. api-gateway can no longer route /api/users/**
4. order-service calls user-service
5. Call fails
6. Circuit breaker counts failures
7. After 2 failures → circuit OPENS
8. order-service returns fallback response
9. System stays stable!
10. After 10 seconds → circuit goes HALF-OPEN
11. user-service recovers → restarts → registers with Eureka
12. Circuit breaker tests 3 calls → all pass → CLOSES!
13. Everything back to normal!
```

---

## Quick Start

```bash
# Clone project
git clone https://github.com/Krishna-wo/ecommerce-microservices.git
cd ecommerce-microservices

# Build all services
cd user-service && mvn clean install -DskipTests && cd ..
cd product-service && mvn clean install -DskipTests && cd ..
cd order-service && mvn clean install -DskipTests && cd ..
cd discovery-server && mvn clean install -DskipTests && cd ..
cd config-server && mvn clean install -DskipTests && cd ..
cd api-gateway && mvn clean install -DskipTests && cd ..

# Start everything
docker-compose up -d

# Check all running
docker ps

# View Eureka Dashboard
open http://localhost:8761

# Test API
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Krish","email":"krish@gmail.com","password":"1234"}'
```

---

*Documentation by Krish | E-Commerce Microservices Project*
