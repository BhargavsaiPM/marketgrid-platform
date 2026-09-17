# Vendor Service

Vendor profile management for the **MarketGrid Commerce** platform.  
Validates JWTs issued by **user-service** using the same shared secret.

## Prerequisites

| Tool   | Version |
|--------|---------|
| Java   | 17+     |
| Maven  | 3.9+    |

## Startup Order

> **eureka-server (8761) → config-server (8888) → user-service (8081) → vendor-service (8082)**

## Running

```bash
# from the vendor-service/ directory
mvn spring-boot:run

# or from the project root
mvn -pl vendor-service spring-boot:run
```

The service starts on **http://localhost:8082**.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/vendors/register` | JWT (VENDOR role) | Create a vendor profile |
| GET | `/api/vendors/me` | JWT | Get your own vendor profile |
| PUT | `/api/vendors/me` | JWT | Update your own vendor profile |
| GET | `/api/vendors/{vendorId}` | Public | View a vendor's profile |
| GET | `/api/vendors` | Public | List all approved vendors |

## Example End-to-End Flow

### 1. Register a VENDOR user (user-service)

```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"shopowner","password":"pass123","email":"shop@example.com","role":"VENDOR"}'
```

### 2. Login to get a JWT (user-service)

```bash
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"shopowner","password":"pass123"}'
```

Copy the `token` value from the response.

### 3. Register a vendor profile (vendor-service)

```bash
curl -X POST http://localhost:8082/api/vendors/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "businessName": "Shop Owner Electronics",
    "businessDescription": "Premium electronics and gadgets",
    "contactPhone": "+91-9876543210",
    "address": "42 MG Road, Bangalore"
  }'
```

### 4. Get your vendor profile (vendor-service)

```bash
curl http://localhost:8082/api/vendors/me \
  -H "Authorization: Bearer <token>"
```

### 5. Update your vendor profile

```bash
curl -X PUT http://localhost:8082/api/vendors/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"businessDescription": "Updated description here"}'
```

### 6. Browse vendors (public, no token needed)

```bash
curl http://localhost:8082/api/vendors
curl http://localhost:8082/api/vendors/1
```

## H2 Console

> **http://localhost:8082/h2-console**

JDBC URL: `jdbc:h2:mem:vendordb` | User: `sa` | Password: *(empty)*

## JWT Notes

- Tokens are **issued by user-service** and **validated here** using the same `jwt.secret`
- The `userId` claim from the JWT is used to link vendor profiles — it is never taken from request bodies
- Only users with role `VENDOR` can call `POST /api/vendors/register` (returns 403 otherwise)
