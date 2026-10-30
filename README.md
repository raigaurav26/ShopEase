# ShopEase - E-Commerce Microservices Platform

[![CI](https://github.com/raigaurav26/ShopEase/actions/workflows/ci.yml/badge.svg)](https://github.com/raigaurav26/ShopEase/actions/workflows/ci.yml)

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
| POST | `/api/auth/register` | Register new user, returns a token pair |
| POST | `/api/auth/login` | Sign in, returns a token pair |
| POST | `/api/auth/refresh` | Swap a refresh token for a new token pair |
| POST | `/api/auth/logout` | Revoke the session a refresh token belongs to |

### How sign-in works

- **Short-lived access tokens.** Login returns a JWT access token that expires after 15 minutes and an opaque refresh token that lasts 7 days.
- **Refresh token rotation.** Every call to `/refresh` revokes the token it received and returns a new one. Refresh tokens are stored only as SHA-256 hashes, and the lookup takes a row lock so two parallel refreshes with the same token can't both succeed.
- **Reuse detection.** If a refresh token that was already rotated is used again, someone has a copy of it, so every session of that user is revoked and they must sign in again.
- **Logout** revokes the whole session (the chain of rotated tokens from one login), not just the latest token.
- **Account lockout.** After 5 wrong passwords in a row the account is locked for 15 minutes (`423 Locked`).
- **No account enumeration.** An unknown email and a wrong password return the same `401 Invalid email or password`, and an unknown email still runs a BCrypt check so both take the same time.
- **Validation.** Requests are validated with Bean Validation (valid email, password of 8 to 72 characters) and errors come back per field.
- **Configuration.** The signing key comes from the `JWT_SECRET` environment variable (at least 32 bytes). Token lifetimes and lockout limits are set under `jwt.*` and `auth.lockout.*` in `application.yml`.

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

Stock is reduced with a single conditional `UPDATE ... WHERE quantity >= qty`, so concurrent orders for the last units can't oversell. Errors: `404` unknown product, `409` not enough stock, `400` invalid quantity.

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

### Login response
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "3q2-7wV9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "message": "Login successful"
}
```

### Refresh or logout
```json
POST /api/auth/refresh
{
    "refreshToken": "3q2-7wV9..."
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
