# User Service

User registration, authentication, and JWT-based identity for the **MarketGrid Commerce** platform.

## Prerequisites

| Tool   | Version |
|--------|---------|
| Java   | 17+     |
| Maven  | 3.9+    |

## Startup Order

> **eureka-server → config-server → user-service**

## Running

```bash
# from the user-service/ directory
mvn spring-boot:run

# or from the project root
mvn -pl user-service spring-boot:run
```

The service starts on **http://localhost:8081**.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/users/register` | Public | Create a new user account |
| POST | `/api/users/login` | Public | Authenticate and receive a JWT |
| GET | `/api/users/me` | JWT required | Get the authenticated user's profile |

### Register

```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123","email":"alice@example.com","role":"CUSTOMER"}'
```

**201 Created** response:
```json
{ "id": 1, "username": "alice", "email": "alice@example.com", "role": "CUSTOMER" }
```

### Login

```bash
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
```

**200 OK** response:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

### Get Current User (JWT required)

```bash
curl http://localhost:8081/api/users/me \
  -H "Authorization: Bearer <token>"
```

**200 OK** response:
```json
{ "id": 1, "username": "alice", "email": "alice@example.com", "role": "CUSTOMER" }
```

## H2 Console

While running, the in-memory database can be inspected at:

> **http://localhost:8081/h2-console**

JDBC URL: `jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1` | User: `sa` | Password: *(empty)*

## Default Admin Credentials

> [!WARNING]
> **FOR DEVELOPMENT/TESTING ONLY — change or remove before any real deployment.**

On application startup, a default administrator account is automatically seeded if one does not already exist:

| Property | Value |
|---|---|
| **Username** | `admin` |
| **Password** | `Admin@12345` |
| **Email** | `admin@marketgrid.com` |
| **Role** | `ADMIN` |

To authenticate as admin and obtain a JWT token:

```bash
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@12345"}'
```

## JWT Notes

- Tokens embed `userId`, `username`, and `role` claims
- Expiration: 24 hours (configurable via `jwt.expiration-ms`)
- The `jwt.secret` property **must be identical** across every service that validates tokens

