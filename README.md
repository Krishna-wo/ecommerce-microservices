# 🛒 E-Commerce Microservices Project

A production-ready e-commerce backend built with Spring Boot Microservices architecture.

## 🏗️ Architecture

```
                    ┌─────────────────┐
                    │   API Gateway   │
                    │   port: 8080    │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
    ┌───────▼──────┐ ┌───────▼──────┐ ┌──────▼───────┐
    │ user-service │ │product-service│ │ order-service│
    │  port: 8081  │ │  port: 8082  │ │  port: 8083  │
    └──────────────┘ └──────────────┘ └──────────────┘
            │                │                │
    ┌───────▼────────────────▼────────────────▼───────┐
    │                   MySQL Database                 │
    │        userdb        productdb        orderdb    │
    └──────────────────────────────────────────────────┘

                    ┌─────────────────┐
                    │ Discovery Server│
                    │   port: 8761    │
                    └─────────────────┘

                    ┌─────────────────┐
                    │  Config Server  │
                    │   port: 8888    │
                    └─────────────────┘
```

## 🚀 Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Single entry point, JWT validation, routing |
| user-service | 8081 | User registration, login, JWT generation |
| product-service | 8082 | Product management |
| order-service | 8083 | Order placement, calls user + product service |
| discovery-server | 8761 | Eureka service registry |
| config-server | 8888 | Centralized configuration from GitHub |

## 🛠️ Tech Stack

- **Java 21**
- **Spring Boot 3.2.5**
- **Spring Cloud 2023.0.0**
- **Spring Cloud Gateway** - API Gateway
- **Spring Cloud Netflix Eureka** - Service Discovery
- **Spring Cloud Config** - Centralized Configuration
- **Spring Security + JWT** - Authentication
- **Resilience4j** - Circuit Breaker
- **MySQL 8** - Database
- **Docker + Docker Compose** - Containerization
- **GitHub** - Config repository

## ✨ Features

- ✅ Microservices Architecture
- ✅ Service Discovery (Eureka)
- ✅ API Gateway with JWT Authentication
- ✅ Circuit Breaker Pattern (Resilience4j)
- ✅ Centralized Configuration (GitHub)
- ✅ Docker Compose deployment
- ✅ Inter-service communication (WebClient)

## 🔐 Security Flow

```
1. Register  → POST /api/users/register
2. Login     → POST /api/users/login → returns JWT token
3. Use APIs  → Add header: Authorization: Bearer <token>
4. Gateway validates token before forwarding request
```

## 🚦 How to Run

### Prerequisites
- Docker Desktop installed
- Docker Compose installed

### Start everything with ONE command:

```bash
# Clone the repo
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
```

### Stop everything:
```bash
docker-compose down
```

### Check running containers:
```bash
docker ps
```

### Check logs of specific service:
```bash
docker logs user-service
docker logs order-service
```

## 📡 API Endpoints

### User Service
| Method | URL | Description | Auth Required |
|--------|-----|-------------|---------------|
| POST | /api/users/register | Register new user | No |
| POST | /api/users/login | Login and get JWT token | No |
| GET | /api/users/{id} | Get user by ID | Yes |

### Product Service
| Method | URL | Description | Auth Required |
|--------|-----|-------------|---------------|
| POST | /api/products | Add new product | Yes |
| GET | /api/products | Get all products | Yes |
| GET | /api/products/{id} | Get product by ID | Yes |

### Order Service
| Method | URL | Description | Auth Required |
|--------|-----|-------------|---------------|
| POST | /api/orders | Place new order | Yes |
| GET | /api/orders/{id} | Get order by ID | Yes |

## 🧪 Testing with Postman

### Step 1 - Register:
```json
POST http://localhost:8080/api/users/register
{
    "name": "John",
    "email": "john@gmail.com",
    "password": "1234"
}
```

### Step 2 - Login:
```json
POST http://localhost:8080/api/users/login
{
    "email": "john@gmail.com",
    "password": "1234"
}
```

### Step 3 - Use token in all requests:
```
Headers:
Authorization: Bearer <your-token-here>
```

### Step 4 - Add product:
```json
POST http://localhost:8080/api/products
{
    "name": "iPhone 15",
    "description": "Latest Apple smartphone",
    "price": 999.99,
    "quantity": 50
}
```

### Step 5 - Place order:
```json
POST http://localhost:8080/api/orders
{
    "userId": 1,
    "productId": 1,
    "quantity": 2
}
```

## 🔄 Circuit Breaker

If user-service or product-service goes down, instead of crashing the system returns a friendly response:
```json
{
    "id": -1,
    "status": "SERVICE_UNAVAILABLE - Please try again later!"
}
```
System stays stable! 🔥

## ⚙️ Config Server

All service configurations are stored centrally on GitHub:
- [ecommerce-config](https://github.com/Krishna-wo/ecommerce-config)

Change config on GitHub → all services pick it up automatically!

## 📊 Monitoring

- **Eureka Dashboard** → http://localhost:8761
- **Config Server** → http://localhost:8888/user-service/default

## 👨‍💻 Author

**Krishna** - [GitHub](https://github.com/Krishna-wo)
