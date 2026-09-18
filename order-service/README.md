# Order Service — Cart & Orders

Shopping cart and order management microservice for the **MarketGrid Commerce** platform.
Features snapshot item pricing, atomic checkout with partial-success tolerance, optimistic stock decrement via product-service, and vendor-scoped order tracking.

## Prerequisites

| Tool   | Version |
|--------|---------|
| Java   | 17+     |
| Maven  | 3.9+    |

## Startup Order

Services must be started in the following order:

```
1. eureka-server    → http://localhost:8761
2. config-server    → http://localhost:8888
3. user-service     → http://localhost:8081
4. vendor-service   → http://localhost:8082
5. product-service  → http://localhost:8083
6. order-service    → http://localhost:8084  ← this service
```

### Running order-service

From the `order-service/` directory:

```bash
mvn spring-boot:run
```

Or from the project root:

```bash
mvn -pl order-service spring-boot:run
```

The service runs on **http://localhost:8084**.

---

## API Endpoints

All endpoints require a valid JWT token in the `Authorization: Bearer <token>` header.

### Cart Endpoints (`/api/cart`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/cart` | 🔒 JWT | View current user's cart with items and total |
| `POST` | `/api/cart/items` | 🔒 JWT | Add a product to cart (or increment quantity) |
| `PUT` | `/api/cart/items/{cartItemId}` | 🔒 JWT (owner) | Update item quantity (0 removes item) |
| `DELETE` | `/api/cart/items/{cartItemId}` | 🔒 JWT (owner) | Remove item from cart |
| `DELETE` | `/api/cart` | 🔒 JWT | Clear all items from cart |

### Order Endpoints (`/api/orders`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/orders/checkout` | 🔒 JWT | Checkout cart: decrements stock & creates order |
| `GET` | `/api/orders` | 🔒 JWT | View customer's order history |
| `GET` | `/api/orders/{orderId}` | 🔒 JWT (owner) | View single order detail grouped by vendor |
| `GET` | `/api/orders/vendor/mine` | 🔒 VENDOR | View orders containing items for the authenticated vendor |

---

## Complete End-to-End Workflow (curl)

### Step 1: Register and Login Customer

```bash
# 1. Register customer
curl -s -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "customer1", "password": "User@12345", "email": "customer1@marketgrid.com", "role": "CUSTOMER"}'

# 2. Login customer
CUSTOMER_TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "customer1", "password": "User@12345"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
```

### Step 2: Register & Approve Vendor

```bash
# 1. Register vendor user
curl -s -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username": "vendor1", "password": "Vendor@12345", "email": "vendor1@marketgrid.com", "role": "VENDOR"}'

# 2. Login vendor
VENDOR_TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "vendor1", "password": "Vendor@12345"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 3. Create vendor profile
curl -s -X POST http://localhost:8082/api/vendors/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d '{
    "businessName": "Apex Electronics",
    "businessDescription": "High quality electronics",
    "contactPhone": "1234567890",
    "address": "123 Tech Park"
  }'

# 4. Approve vendor via ADMIN account
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@12345"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

curl -s -X PUT http://localhost:8082/api/vendors/1/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Step 3: Vendor Creates Product

```bash
curl -s -X POST http://localhost:8083/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $VENDOR_TOKEN" \
  -d '{
    "name": "Mechanical Keyboard",
    "description": "RGB Hot-swappable keyboard",
    "price": 99.99,
    "stockQuantity": 50,
    "category": "Peripherals"
  }'
```

### Step 4: Customer Adds to Cart

```bash
# Add 2 units of product ID 1
curl -s -X POST http://localhost:8084/api/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -d '{"productId": 1, "quantity": 2}'

# View cart
curl -s http://localhost:8084/api/cart \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Step 5: Customer Checks Out

```bash
curl -s -X POST http://localhost:8084/api/orders/checkout \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

Sample Response:
```json
{
  "orderId": 1,
  "userId": 2,
  "totalAmount": 199.98,
  "createdAt": "2026-09-17T10:35:00",
  "vendorGroups": [
    {
      "vendorId": 1,
      "subtotal": 199.98,
      "allConfirmed": true,
      "items": [
        {
          "id": 1,
          "productId": 1,
          "vendorId": 1,
          "quantity": 2,
          "priceAtOrderTime": 99.99,
          "subtotal": 199.98,
          "status": "CONFIRMED"
        }
      ]
    }
  ]
}
```

### Step 6: Customer Views Order History

```bash
curl -s http://localhost:8084/api/orders \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"
```

### Step 7: Vendor Views Their Orders

```bash
curl -s http://localhost:8084/api/orders/vendor/mine \
  -H "Authorization: Bearer $VENDOR_TOKEN"
```

---

## H2 Console

Inspect in-memory tables (`carts`, `cart_items`, `orders`, `order_items`):

> **http://localhost:8084/h2-console**

* **JDBC URL**: `jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1`
* **Username**: `sa`
* **Password**: *(empty)*
