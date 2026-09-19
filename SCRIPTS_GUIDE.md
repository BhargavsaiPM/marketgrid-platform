# MarketGrid Commerce — Shell Scripts Execution Guide

This guide explains how to start, verify, authenticate, inspect JWT tokens, and stop the MarketGrid Commerce microservices platform using the staged shell scripts on Windows (via **Git Bash**, **VS Code Terminal**, **WSL**, or **PowerShell**).

---

## 📋 Overview of Scripts

| Script | Purpose | Key Responsibilities |
| :--- | :--- | :--- |
| [`run-services.sh`](run-services.sh) | **Start Services** | Starts all 7 microservices in strict dependency order using `nohup`, polls `/actuator/health` every 2s (35s timeout), writes logs to `logs/<service>.log`, saves PIDs to `pids/<service>.pid`, and outputs a final UP/DOWN status table. |
| [`get-tokens.sh`](get-tokens.sh) | **Authentication & Tokens** | Validates API Gateway reachability, registers test personas (`customer1`, `vendorA`, `vendorB`), authenticates all 4 accounts (`admin`, `customer1`, `vendorA`, `vendorB`), decodes JWT claims, and prints copy-pasteable token exports. Halts immediately on HTTP 500. |
| [`stop-services.sh`](stop-services.sh) | **Clean Shutdown** | Reads process IDs from `pids/*.pid`, terminates process trees via `taskkill` (or `kill`), sweeps ports 8761, 8888, 8080–8084, and cleans up PID files. |

---

## 🛠️ Prerequisites

Before running the scripts, make sure the following are installed and in your environment `PATH`:
1. **Java Development Kit (JDK) 17+** (JDK 21 or JDK 25 recommended)
   - Verify: `java -version`
2. **Apache Maven 3.8+**
   - Verify: `mvn -version`
3. **Python 3.8+** (used by `get-tokens.sh` for JSON formatting and base64url JWT decoding)
   - Verify: `python --version` or `python3 --version`
4. **Git Bash** (included with Git for Windows) or **WSL**

---

## 💻 How to Open a Shell in VS Code

1. Press <kbd>Ctrl</kbd> + <kbd>`</kbd> (or go to menu: **Terminal** &rarr; **New Terminal**).
2. Look at the terminal dropdown tab in the top right of the terminal window:
   - Click the dropdown arrow next to the `+` icon and select **Git Bash**.
   - *(Optional)* Set Git Bash as default: press <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>P</kbd>, type `Terminal: Select Default Profile`, and choose `Git Bash`.

---

## 🚀 Execution Workflow

### Step 1: Start All 7 Microservices

In your Git Bash terminal, run:
```bash
./run-services.sh
```
*(Or if running from PowerShell / CMD: `bash run-services.sh`)*

#### Execution Sequence:
1. `eureka-server` (port 8761) &rarr; waits for `/actuator/health` `[UP]`
2. `config-server` (port 8888) &rarr; waits for `/actuator/health` `[UP]`
3. `user-service` (port 8081) &rarr; waits for `/actuator/health` `[UP]`
4. `vendor-service` (port 8082) &rarr; waits for `/actuator/health` `[UP]`
5. `product-service` (port 8083) &rarr; waits for `/actuator/health` `[UP]`
6. `order-service` (port 8084) &rarr; waits for `/actuator/health` `[UP]`
7. `api-gateway` (port 8080) &rarr; waits for `/actuator/health` `[UP]`

#### Expected Output:
```text
================================================================================
                       SERVICES STATUS REPORT                                   
================================================================================
  SERVICE          PORT   STATUS   PID        LOG FILE                 
  ----------------------------------------------------------------------
  eureka-server    8761   UP       31536      logs/eureka-server.log   
  config-server    8888   UP       3112       logs/config-server.log   
  user-service     8081   UP       16720      logs/user-service.log    
  vendor-service   8082   UP       15232      logs/vendor-service.log  
  product-service  8083   UP       18404      logs/product-service.log 
  order-service    8084   UP       15008      logs/order-service.log   
  api-gateway      8080   UP       7668       logs/api-gateway.log     
================================================================================
✓ ALL 7 SERVICES CONFIRMED UP AND READY!
```

---

### Step 2: Register Users & Retrieve JWT Tokens

Once the status table shows all services are `UP`, run:
```bash
./get-tokens.sh
```
*(Or from PowerShell: `bash get-tokens.sh`)*

#### What This Script Does:
1. Pings `http://localhost:8080/actuator/health` through the gateway to confirm the edge is responsive.
2. Registers:
   - `customer1` (`pass123`, `CUSTOMER`)
   - `vendorA` (`pass123`, `VENDOR`)
   - `vendorB` (`pass123`, `VENDOR`)
   *(If an account already exists, it handles the 400 Bad Request gracefully and proceeds).*
   *(If registration returns HTTP 500, it prints the full response and halts immediately).*
3. Logs in as:
   - `admin` (Default seeded password: `Admin@12345`)
   - `customer1` (`pass123`)
   - `vendorA` (`pass123`)
   - `vendorB` (`pass123`)
4. Prints for each persona:
   - **Raw JSON response** (`{"token": "..."}`)
   - **Extracted token string**
   - **Decoded JWT payload** with claims (`sub`, `role`, `userId`, `iat`, `exp`)
5. Outputs clean, copy-pasteable export lines:

```bash
================================================================================
                       SAVED JWT TOKENS FOR MANUAL TESTING                      
================================================================================
# Copy-paste these export lines directly into your terminal or Postman:

ADMIN_TOKEN="eyJhbGciOiJIUzI1NiJ9..."

CUSTOMER_TOKEN="eyJhbGciOiJIUzI1NiJ9..."

VENDOR_A_TOKEN="eyJhbGciOiJIUzI1NiJ9..."

VENDOR_B_TOKEN="eyJhbGciOiJIUzI1NiJ9..."
================================================================================
✓ Authentication completed successfully. All 4 tokens retrieved & verified!
```

---

### Step 3: Using the Retrieved Tokens

#### In Terminal (cURL)
Copy-paste the export lines from Step 2 into your terminal, then query any protected endpoint:
```bash
# Query profile of authenticated user
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" http://localhost:8080/api/users/me

# Access vendor endpoint
curl -H "Authorization: Bearer $VENDOR_A_TOKEN" http://localhost:8080/api/vendors/me
```

#### In Postman
1. Go to the request headers or **Authorization** tab.
2. Select **Bearer Token**.
3. Paste the token string (without quotes).

#### In Swagger UI Aggregator
1. Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) in your browser.
2. Click the green **Authorize** button at the top right.
3. Paste the token into the `BearerAuth` box and click **Authorize**.
4. Select any backend service from the top-right definition dropdown:
   - User Service (`/api/users/v3/api-docs`)
   - Vendor Service (`/api/vendors/v3/api-docs`)
   - Product Service (`/api/products/v3/api-docs`)
   - Order Service (`/api/orders/v3/api-docs`)

---

### Step 4: Stop All Services Cleanly

When you are done testing:
```bash
./stop-services.sh
```
*(Or from PowerShell: `bash stop-services.sh`)*

#### What This Script Does:
1. Reads all saved PIDs from `pids/*.pid` and terminates each process tree cleanly.
2. Sweeps ports `8761`, `8888`, `8080`, `8081`, `8082`, `8083`, and `8084` to ensure no orphaned Java processes remain.
3. Clears the `pids/` directory.

---

## 📁 Runtime Directory Layout

Running the scripts generates the following folders in the root directory:

```text
marketgrid-commerce/
├── logs/                      # Log file for each service
│   ├── eureka-server.log
│   ├── config-server.log
│   ├── user-service.log
│   ├── vendor-service.log
│   ├── product-service.log
│   ├── order-service.log
│   └── api-gateway.log
├── pids/                      # Process ID files for clean shutdown
│   ├── eureka-server.pid
│   ├── config-server.pid
│   ├── user-service.pid
│   ├── vendor-service.pid
│   ├── product-service.pid
│   ├── order-service.pid
│   └── api-gateway.pid
├── run-services.sh            # Script 1: Starts services
├── get-tokens.sh              # Script 2: Authentication & token extraction
└── stop-services.sh           # Cleanup script
```

---

## 🛠️ Troubleshooting & Tips

- **Permission Denied in Git Bash**:
  ```bash
  chmod +x run-services.sh get-tokens.sh stop-services.sh
  ```
- **Inspect Live Logs**:
  ```bash
  tail -f logs/api-gateway.log
  tail -f logs/user-service.log
  ```
- **Check Open Service Ports**:
  ```bash
  netstat -ano | grep -E "(8761|8888|8080|8081|8082|8083|8084)"
  ```
- **Windows PowerShell Alternative**:
  If you prefer native PowerShell scripts without bash:
  - Start: `.\start-all.ps1`
  - Stop: `.\stop-all.ps1`
