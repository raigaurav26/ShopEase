# ShopEase - E-Commerce Microservices Platform

An e-commerce backend built with **Java 17, Spring Boot 3, Spring Cloud, and MySQL** using microservices architecture.

## Architecture

```
                    ┌──────────────────┐
                    │   API Gateway    │
                    │    (Port 8080)   │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼──┐  ┌───────▼────┐  ┌──────▼───────┐
    │Auth Service│  │  Product   │  │   Order      │
    │ (Port 8081)│  │  Service   │  │   Service    │
    └────────────┘  │ (Port 8082)│  │  (Port 8083) │
                    └────────────┘  └──────┬───────┘
                                           │
                                    ┌──────┼──────┐
                                    │             │
                             ┌──────▼───┐  ┌─────▼──────┐
                             │ Payment  │  │ Inventory  │
                             │ Service  │  │  Service   │
                             │(Port 8084)│ │(Port 8085) │
                             └──────────┘  └────────────┘

              All services register with Eureka (Port 8761)
```

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.5**
- **Spring Cloud** (Gateway, Eureka, OpenFeign)
- **Spring Security + JWT**
- **Spring Data JPA**
- **MySQL 8**
- **Maven**
- **Docker & Docker Compose**

## Microservices

| Service | Port | Description |
|---------|------|-------------|
| Service Registry | 8761 | Eureka Server for service discovery |
| API Gateway | 8080 | Central entry point, routes requests |
| Auth Service | 8081 | User registration, login, JWT tokens |
| Product Service | 8082 | Product CRUD, search, categories |
| Order Service | 8083 | Order placement with Feign calls |
| Payment Service | 8084 | Mock payment processing |
| Inventory Service | 8085 | Stock management |

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8

### Step 1: Create Databases
```sql
CREATE DATABASE shopease_auth;
CREATE DATABASE shopease_products;
CREATE DATABASE shopease_orders;
CREATE DATABASE shopease_payments;
CREATE DATABASE shopease_inventory;
```

### Step 2: Start Services (in order)
```bash
# 1. Service Registry
cd service-registry && mvn spring-boot:run

# 2. API Gateway
cd api-gateway && mvn spring-boot:run

# 3. Auth Service
cd auth-service && mvn spring-boot:run

# 4. Product Service
cd product-service && mvn spring-boot:run

# 5. Inventory Service
cd inventory-service && mvn spring-boot:run

# 6. Payment Service
cd payment-service && mvn spring-boot:run

# 7. Order Service
cd order-service && mvn spring-boot:run
```

### With Docker Compose
```bash
docker-compose up --build
```

## API Endpoints

### Auth (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login and get JWT |

### Products (`/api/products`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/products` | Add product |
| GET | `/api/products` | Get all products |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products/category/{category}` | Get by category |
| GET | `/api/products/search?keyword=` | Search products |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

### Orders (`/api/orders`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Place order |
| GET | `/api/orders` | Get all orders |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/api/orders/user/{userId}` | Get orders by user |

### Payments (`/api/payments`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments` | Process payment |
| GET | `/api/payments` | Get all payments |
| GET | `/api/payments/{id}` | Get payment by ID |
| GET | `/api/payments/order/{orderId}` | Get by order ID |

### Inventory (`/api/inventory`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/inventory` | Add stock |
| GET | `/api/inventory` | Get all stock |
| GET | `/api/inventory/{productId}` | Get stock by product |
| GET | `/api/inventory/{productId}/check` | Check if in stock |
| PUT | `/api/inventory/{productId}/reduce?qty=` | Reduce stock |

## Sample Requests

### Register
```json
POST /api/auth/register
{
    "name": "Gaurav",
    "email": "gaurav@example.com",
    "password": "password123"
}
```

### Add Product
```json
POST /api/products
{
    "name": "iPhone 15",
    "description": "Latest Apple smartphone",
    "price": 79999.00,
    "category": "Electronics",
    "imageUrl": "https://example.com/iphone15.jpg",
    "quantity": 50
}
```

### Place Order
```json
POST /api/orders
{
    "userId": 1,
    "shippingAddress": "Mumbai, Maharashtra",
    "items": [
        {
            "productId": 1,
            "quantity": 2
        }
    ]
}
```
