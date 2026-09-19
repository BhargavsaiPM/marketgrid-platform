# MarketGrid Commerce — Multi-Vendor E-Commerce Platform

A microservices-based multi-vendor e-commerce platform built with **Spring Boot 3**, **Spring Cloud 2023**, **Netflix Eureka**, and **Spring Security** with JWT authentication.

---

## Services Architecture & Ports

| Service | Port | Database | Description | Swagger UI |
|---|---|---|---|---|
| **eureka-server** | `8761` | — | Service Discovery & Registry | — |
| **config-server** | `8888` | Native Git/Filesystem | Centralized Configuration Server | — |
| **user-service** | `8081` | H2 (`userdb`) | User Auth, JWT Issuance, Role RBAC, Default Admin Seeder | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) |
| **vendor-service** | `8082` | H2 (`vendordb`) | Vendor Onboarding, Profile Management, Admin Approval | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) |
| **product-service** | `8083` | H2 (`productdb`) | Product Catalog, Stock Management, Category Browsing | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) |
| **order-service** | `8084` | H2 (`orderdb`) | Cart Management, Multi-Vendor Checkout, Vendor Order Tracking | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) |
| **api-gateway** | `8080` | — | Gateway Entry Point | — |

---

## Interactive API Documentation (Swagger UI)

Each service provides interactive OpenAPI 3 documentation with Bearer JWT support. Click the **"Authorize"** button (with lock icon) in Swagger UI to paste a JWT token once, and it will be sent automatically with subsequent requests.

* **User Service**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) (Raw docs: `/v3/api-docs`)
* **Vendor Service**: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) (Raw docs: `/v3/api-docs`)
* **Product Service**: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) (Raw docs: `/v3/api-docs`)
* **Order Service**: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) (Raw docs: `/v3/api-docs`)

👉 **For a step-by-step tutorial on authenticating and running the full e-commerce flow directly inside Swagger UI, see [SWAGGER_GUIDE.md](SWAGGER_GUIDE.md).**

---

## Startup Order

To ensure configuration and discovery resolve correctly, start services in this order:

```
1. eureka-server    → http://localhost:8761
2. config-server    → http://localhost:8888
3. user-service     → http://localhost:8081
4. vendor-service   → http://localhost:8082
5. product-service  → http://localhost:8083
6. order-service    → http://localhost:8084
```

To run all or individual services via Maven:
```bash
# In each module directory:
mvn spring-boot:run

# Or from the project root:
mvn -pl order-service spring-boot:run
```

---

## Complete End-to-End Workflow (curl)

Follow this step-by-step flow to verify all microservices working together end-to-end:

### 1. Register & Login Customer

```bash
# Register Customer
curl -s -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "Password@123", "email": "alice@example.com", "role": "CUSTOMER"}'

# Login Customer & save JWT
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "Password@123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "Customer Token: $CUSTOMER_TOKEN"
```

---

### 2. Register, Login & Approve Vendor

```bash
# 1. Register vendor user account
curl -s -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "bob_vendor", "password": "Password@123", "email": "bob@store.com", "role": "VENDOR"}'

# 2. Login vendor user
VENDOR_TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "bob_vendor", "password": "Password@123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 3. Create vendor profile
curl -s -X POST http://localhost:8082/api/vendors/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d '{
    "businessName": "Bob Tech Hub",
    "businessDescription": "Leading provider of computer accessories",
    "contactPhone": "9876543210",
    "address": "456 Silicon Alley"
  }'

# 4. Login seeded system admin (seeded on user-service startup)
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@12345"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 5. Admin approves vendor profile (ID 1)
curl -s -X PUT http://localhost:8082/api/vendors/1/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### 3. Vendor Creates Product

```bash
curl -s -X POST http://localhost:8083/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d '{
    "name": "Wireless Ergonomic Mouse",
    "description": "2.4GHz rechargeable wireless mouse",
    "price": 49.99,
    "stockQuantity": 100,
    "category": "Peripherals"
  }'
```

---

### 4. Customer Adds Product to Cart

```bash
# Add 2 items of product 1
curl -s -X POST http://localhost:8084/api/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"productId": 1, "quantity": 2}'

# View cart
curl -s http://localhost:8084/api/cart \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

---

### 5. Customer Checks Out

Checkout performs an atomic checkout:
- Calls `product-service` via OpenFeign with cached admin service credentials to decrement inventory stock.
- Marks each item `CONFIRMED` or `FAILED`.
- Clears the cart if at least one item succeeds.
- Groups items by vendor at response time.

```bash
curl -s -X POST http://localhost:8084/api/orders/checkout \
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
curl -s http://localhost:8084/api/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

# Specific order details (ownership validated)
curl -s http://localhost:8084/api/orders/1 \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

---

### 7. Vendor Views Their Orders

Vendors only see line items belonging to their vendor ID, isolated from items sold by other vendors within the same customer order:

```bash
curl -s http://localhost:8084/api/orders/vendor/mine \
  -H "Authorization: Bearer $VENDOR_TOKEN"
```
