# MarketGrid Commerce — Docker Guide

This guide explains how to build, run, inspect, and manage the entire MarketGrid Commerce microservices platform using Docker and Docker Compose.

---

## 🏗️ Architecture & Features

- **Multi-Stage Builds:** Every service uses a multi-stage `Dockerfile`:
  - **Stage 1 (Build):** Official `maven:3.9-eclipse-temurin-17` compiles and packages the executable JAR.
  - **Stage 2 (Runtime):** Ultra-lightweight `eclipse-temurin:17-jre-alpine` running `java -jar app.jar` with `curl` for container healthchecks.
- **Dependency-Ordered Startup:** Uses `depends_on` with `condition: service_healthy` to guarantee the exact startup lifecycle:
  `eureka-server:8761` &rarr; `config-server:8888` &rarr; `user-service`, `vendor-service`, `product-service`, `order-service` &rarr; `api-gateway:8080`.
- **Shared Bridge Network:** `marketgrid-network` allows inter-container communication using DNS service names (`http://eureka-server:8761/eureka/`, `http://config-server:8888`).
- **Environment Overrides:** Environment variables in `docker-compose.yml` dynamically override localhost URLs without modifying any Java code or local YAML configs, so local (non-Docker) workflows continue to work unchanged.
- **Host Port Forwarding:** All ports (`8761`, `8888`, `8080`–`8084`) are mapped to the host, allowing seamless testing via browser, cURL, and Postman at `http://localhost:8080`.

---

## 📋 Exact Docker Commands

### 1. Build All Images & Start Everything
Run this command from the project root directory:

```bash
docker compose up --build -d
```
> **Tip:** The `-d` flag runs containers in detached (background) mode. If you prefer to stream all logs live to your terminal, omit `-d`:
> ```bash
> docker compose up --build
> ```

---

### 2. Check Status of All Containers
Check the running state and health status of all 7 services:

```bash
docker compose ps
```

*Expected status output once healthy:*
```text
NAME              IMAGE                            STATUS                    PORTS
eureka-server     marketgrid-commerce-eureka-server    Up (healthy)             0.0.0.0:8761->8761/tcp
config-server     marketgrid-commerce-config-server    Up (healthy)             0.0.0.0:8888->8888/tcp
user-service      marketgrid-commerce-user-service     Up (healthy)             0.0.0.0:8081->8081/tcp
vendor-service    marketgrid-commerce-vendor-service   Up (healthy)             0.0.0.0:8082->8082/tcp
product-service   marketgrid-commerce-product-service  Up (healthy)             0.0.0.0:8083->8083/tcp
order-service     marketgrid-commerce-order-service    Up (healthy)             0.0.0.0:8084->8084/tcp
api-gateway       marketgrid-commerce-api-gateway      Up (healthy)             0.0.0.0:8080->8080/tcp
```

---

### 3. View Logs for a Specific Service
To tail live logs for any service in real time:

```bash
# Gateway logs
docker compose logs -f api-gateway

# Order Service logs
docker compose logs -f order-service

# User Service logs
docker compose logs -f user-service

# Eureka Server logs
docker compose logs -f eureka-server
```
*(Press `Ctrl + C` to stop tailing logs; the container keeps running).*

To view the last 100 lines of logs without streaming:
```bash
docker compose logs --tail=100 order-service
```

---

### 4. Stop & Remove Everything Cleanly
To stop all containers, remove the network, and clean up resources:

```bash
docker compose down
```

To also delete the compiled container images:
```bash
docker compose down --rmi local
```

To remove containers and any associated volumes:
```bash
docker compose down -v
```

---

## 🧪 Testing Through the Gateway While Running in Docker

Because ports are forwarded directly to `localhost`, you can run the authentication and token extraction script directly against the Dockerized system:

```bash
./get-tokens.sh
```

Or access Swagger UI in your browser:
```
http://localhost:8080/swagger-ui.html
```
Or check Eureka registry:
```
http://localhost:8761
```
