# Config Server

Centralized configuration server for the **MarketGrid Commerce** microservices platform.  
Uses a **native (classpath) backend** — config files live in `src/main/resources/config/`.

## Prerequisites

| Tool   | Version |
|--------|---------|
| Java   | 17+     |
| Maven  | 3.9+    |

## Startup Order

> **eureka-server → config-server → all other services**

The Config Server registers itself with Eureka and must be running **before** any downstream service starts, so those services can pull their configuration at boot time.

## Running

From the `config-server/` directory:

```bash
mvn spring-boot:run
```

Or from the project root:

```bash
mvn -pl config-server spring-boot:run
```

## Verifying

Once running, fetch config for any service by hitting:

```
GET http://localhost:8888/{service-name}/{profile}
```

Examples:

```bash
# Default profile
curl http://localhost:8888/user-service/default

# Any service — should return a valid JSON response (even if the property sources are empty)
curl http://localhost:8888/order-service/default
curl http://localhost:8888/api-gateway/default
```

A successful response looks like:

```json
{
  "name": "user-service",
  "profiles": ["default"],
  "label": null,
  "version": null,
  "state": null,
  "propertySources": []
}
```

You can also verify Eureka registration by opening **http://localhost:8761** — the Config Server should appear in the registered instances list as `CONFIG-SERVER`.

## Configuration Files

Placeholder YAML files for each downstream service are stored in:

```
src/main/resources/config/
├── api-gateway.yml
├── order-service.yml
├── product-service.yml
├── user-service.yml
└── vendor-service.yml
```

Add real configuration values (datasource URLs, JWT secrets, feature flags, etc.) to these files as each service is implemented.
