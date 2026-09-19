# MarketGrid Commerce — Multi-Vendor E-Commerce Platform

A microservices-based multi-vendor e-commerce platform built with **Spring Boot 3**, **Spring Cloud 2024**, **Spring Cloud Gateway (WebFlux)**, **Netflix Eureka**, and **Spring Security** with JWT authentication.

---

## Services Architecture & Ports

| Service | Port | Database / Type | Description | Swagger UI |
|---|---|---|---|---|
| **eureka-server** | `8761` | — | Service Discovery & Registry | [http://localhost:8761](http://localhost:8761) |
| **config-server** | `8888` | Native Filesystem | Centralized Configuration Server | — |
| **user-service** | `8081` | H2 (`userdb`) | User Auth, JWT Issuance, Role RBAC, Default Admin Seeder | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| **vendor-service** | `8082` | H2 (`vendordb`) | Vendor Onboarding, Profile Management, Admin Approval | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) |
| **product-service** | `8083` | H2 (`productdb`) | Product Catalog, Stock Management, Category Browsing | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) |
| **order-service** | `8084` | H2 (`orderdb`) | Cart Management, Multi-Vendor Checkout, Vendor Order Tracking | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) |
| **api-gateway** | `8080` | Reactive (WebFlux) | Edge API Gateway, Perimeter JWT Validation, Global CORS, Eureka Routing (`lb://`) | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |

---

## API Gateway & Unified Port 8080 Access

> [!IMPORTANT]
> **Single Entry Point**: Going forward, **ALL client and frontend testing should happen exclusively through the API Gateway on port `8080`**, not by hitting individual service ports directly.
> 
> * **Correct:** `http://localhost:8080/api/users/login`
> * **Direct (internal only):** `http://localhost:8081/api/users/login`
>
> **Defense-in-Depth Security**: The Gateway acts as an edge validation checkpoint. It intercepts all incoming requests, verifies token structure and HMAC signature using the shared secret, and rejects unauthorized or malformed requests with `401 Unauthorized` before they hit downstream services. Downstream services independently re-verify the token and enforce fine-grained role-based access control (RBAC).

---

## Interactive API Documentation (Swagger UI)

### 🌟 Unified Gateway Swagger UI (All Services in One Place)
You can view and test **ALL** microservices from a single aggregated Swagger UI at the Gateway:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

Use the **"Select a spec"** dropdown in the top-right corner to seamlessly switch between:
* **User Service** (`/api/users/v3/api-docs`)
* **Vendor Service** (`/api/vendors/v3/api-docs`)
* **Product Service** (`/api/products/v3/api-docs`)
* **Order Service** (`/api/orders/v3/api-docs`)

### Individual Direct Service Docs (Optional)
If accessing services directly during standalone development:
* **User Service**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
* **Vendor Service**: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
* **Product Service**: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
* **Order Service**: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)

👉 **For a step-by-step tutorial on authenticating and running the full e-commerce flow directly inside Swagger UI, see [SWAGGER_GUIDE.md](SWAGGER_GUIDE.md).**

---

## Startup Order

To ensure configuration and discovery resolve correctly, start services in the following order. **`api-gateway` should start LAST** so that it can discover and route to all registered microservices immediately:

```text
1. eureka-server    → http://localhost:8761
2. config-server    → http://localhost:8888
3. user-service     → http://localhost:8081
4. vendor-service   → http://localhost:8082
5. product-service  → http://localhost:8083
6. order-service    → http://localhost:8084
7. api-gateway      → http://localhost:8080 (Starts LAST)
```

To run all or individual services via Maven:
```bash
# In each module directory:
mvn spring-boot:run

# Or from the project root:
mvn -pl api-gateway spring-boot:run
```

---

## Complete End-to-End Workflow via API Gateway (Port 8080)

Follow this step-by-step flow to verify all microservices working together end-to-end through the Gateway on port `8080`:

### 1. Register & Login Customer

```bash
# Register Customer via Gateway
curl -s -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "Password@123", "email": "alice@example.com", "role": "CUSTOMER"}'

# Login Customer & save JWT
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "Password@123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "Customer Token: $CUSTOMER_TOKEN"
```

---

### 2. Register, Login & Approve Vendor

```bash
# 1. Register vendor user account via Gateway
curl -s -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "bob_vendor", "password": "Password@123", "email": "bob@store.com", "role": "VENDOR"}'

# 2. Login vendor user
VENDOR_TOKEN=$(curl -s -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "bob_vendor", "password": "Password@123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 3. Create vendor profile
curl -s -X POST http://localhost:8080/api/vendors/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d '{
    "businessName": "Bob Tech Hub",
    "businessDescription": "Leading provider of computer accessories",
    "contactPhone": "9876543210",
    "address": "456 Silicon Alley"
  }'

# 4. Login seeded system admin (seeded on user-service startup)
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@12345"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 5. Admin approves vendor profile (ID 1)
curl -s -X PUT http://localhost:8080/api/vendors/1/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### 3. Vendor Creates Product & Public Browsing

```bash
# Vendor adds product
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d '{
    "name": "Wireless Ergonomic Mouse",
    "description": "2.4GHz rechargeable wireless mouse",
    "price": 49.99,
    "stockQuantity": 100,
    "category": "Peripherals"
  }'

# Public browsing of products (No JWT required, passed through by Gateway)
curl -s http://localhost:8080/api/products
```

---

### 4. Customer Adds Product to Cart

```bash
# Add 2 items of product 1
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"productId": 1, "quantity": 2}'

# View cart
curl -s http://localhost:8080/api/cart \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

---

### 5. Customer Checks Out

Checkout performs an atomic multi-vendor checkout:
- Calls `product-service` via OpenFeign with cached admin service credentials to decrement inventory stock.
- Marks each item `CONFIRMED` or `FAILED`.
- Clears the cart if at least one item succeeds.
- Groups items by vendor in the response.

```bash
curl -s -X POST http://localhost:8080/api/orders/checkout \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

Sample Response:
```json
{
  "orderId": 1,
  "userId": 2,
  "totalAmount": 99.98,
  "createdAt": "2026-09-17T10:35:00",
  "vendorGroups": [
    {
      "vendorId": 1,
      "subtotal": 99.98,
      "allConfirmed": true,
      "items": [
        {
          "id": 1,
          "productId": 1,
          "vendorId": 1,
          "quantity": 2,
          "priceAtOrderTime": 49.99,
          "subtotal": 99.98,
          "status": "CONFIRMED"
        }
      ]
    }
  ]
}
```

---

### 6. Customer Views Order History & Order Details

```bash
# Order history
curl -s http://localhost:8080/api/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Specific order details (ownership validated)
curl -s http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

---

### 7. Vendor Views Their Orders

Vendors only see line items belonging to their vendor ID, isolated from items sold by other vendors within the same customer order:

```bash
curl -s http://localhost:8080/api/orders/vendor/mine \
  -H "Authorization: Bearer $VENDOR_TOKEN"
```
