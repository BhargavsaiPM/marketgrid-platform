# Eureka Server

Service registry for the **MarketGrid Commerce** microservices platform.

## Prerequisites

| Tool   | Version |
|--------|---------|
| Java   | 17+     |
| Maven  | 3.9+    |

## Running

From the `eureka-server/` directory:

```bash
mvn spring-boot:run
```

Or from the project root:

```bash
mvn -pl eureka-server spring-boot:run
```

## Dashboard

Once the server is running, open the Eureka dashboard at:

> **http://localhost:8761**

You should see the Eureka status page with **no registered instances** (the server itself does not register).

## Configuration

| Property | Value | Purpose |
|----------|-------|---------|
| `server.port` | `8761` | Default Eureka port |
| `eureka.client.register-with-eureka` | `false` | Prevents self-registration |
| `eureka.client.fetch-registry` | `false` | No need to fetch — this *is* the registry |
| `eureka.instance.hostname` | `localhost` | Hostname for standalone mode |
