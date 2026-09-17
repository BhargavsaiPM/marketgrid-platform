# Product Service — MarketGrid Commerce

Product catalog microservice for the MarketGrid Commerce platform. Manages product listings, stock levels, and vendor ownership.

## Key Features

- **Vendor Approval Gate**: Before creating a product, calls vendor-service (via OpenFeign) to verify the vendor is approved.
- **Optimistic Locking**: Stock updates use JPA `@Version` to prevent overselling under concurrent requests.
- **JWT Security**: Validates tokens issued by user-service using a shared secret.

## Startup Order

Start services in this order:

```
1. eureka-server     → http://localhost:8761
2. config-server     → http://localhost:8888
3. user-service      → http://localhost:8081
4. vendor-service    → http://localhost:8082
5. product-service   → http://localhost:8083   ← this service
```

```bash
# From the project root, in separate terminals:
cd eureka-server  && mvn spring-boot:run
cd config-server  && mvn spring-boot:run
cd user-service   && mvn spring-boot:run
cd vendor-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
```

## API Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/products` | 🔒 VENDOR | Create a product |
| GET | `/api/products` | 🌐 Public | List all products (optional `?category=`) |
| GET | `/api/products/{id}` | 🌐 Public | Single product detail |
| GET | `/api/products/vendor/{vendorId}` | 🌐 Public | All products from a vendor |
| PUT | `/api/products/{id}` | 🔒 VENDOR (owner) | Update a product |
| PATCH | `/api/products/{id}/stock` | 🔒 Any JWT | Decrement stock (for order-service) |

## Full Testing Flow (curl)

### 1. Register a user with VENDOR role

```bash
curl -s -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demovendor",
    "password": "Test@1234",
    "email": "demovendor@marketgrid.com",
    "role": "VENDOR"
  }'
```

### 2. Login to get a JWT token

```bash
curl -s -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "demovendor",
    "password": "Test@1234"
  }'
```

Save the returned `token` value — you'll need it for all subsequent requests.

```bash
TOKEN="<paste-your-token-here>"
```

### 3. Register a vendor profile

```bash
curl -s -X POST http://localhost:8082/api/vendors/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "businessName": "TechGear Hub",
    "businessDescription": "Premium electronics and gadgets",
    "contactPhone": "+91-9876543210",
    "address": "42 MG Road, Bengaluru"
  }'
```

### 4. Approve the vendor (via H2 console)

Open the vendor-service H2 console at [http://localhost:8082/h2-console](http://localhost:8082/h2-console):
- **JDBC URL**: `jdbc:h2:mem:vendordb`
- **Username**: `sa`
- **Password**: *(empty)*

Run:
```sql
UPDATE VENDORS SET IS_APPROVED = TRUE WHERE ID = 1;
```

### 5. Create a product

```bash
curl -s -X POST http://localhost:8083/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Wireless Earbuds Pro",
    "description": "Noise-cancelling Bluetooth earbuds with 24h battery life",
    "price": 2999.99,
    "stockQuantity": 100,
    "category": "Electronics"
  }'
```

### 6. Browse products (public — no auth needed)

```bash
# All products
curl -s http://localhost:8083/api/products

# Filter by category
curl -s "http://localhost:8083/api/products?category=Electronics"

# Single product
curl -s http://localhost:8083/api/products/1

# Products by vendor
curl -s http://localhost:8083/api/products/vendor/1
```

### 7. Update a product (owner only)

```bash
curl -s -X PUT http://localhost:8083/api/products/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Wireless Earbuds Pro Max",
    "price": 3499.99
  }'
```

### 8. Decrement stock (simulating an order)

```bash
curl -s -X PATCH http://localhost:8083/api/products/1/stock \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "quantity": 5
  }'
```

## Dev Tools

- **H2 Console**: [http://localhost:8083/h2-console](http://localhost:8083/h2-console) (JDBC URL: `jdbc:h2:mem:productdb`, user: `sa`, no password)
- **Eureka Dashboard**: [http://localhost:8761](http://localhost:8761) — verify product-service is registered
