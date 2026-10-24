# MarketGrid Commerce — Shell Scripts Execution Guide

This guide explains how to start, verify, authenticate, inspect JWT tokens, seed demo data, and stop the MarketGrid Commerce microservices platform using the staged shell scripts on Windows (via **Git Bash**, **VS Code Terminal**, **WSL**, or **PowerShell**).

---

## 📋 Overview of Scripts

| Script | Purpose | Key Responsibilities |
| :--- | :--- | :--- |
| [`run-services.sh`](run-services.sh) | **Start Services** | Starts all 7 microservices in strict dependency order using `nohup`, polls `/actuator/health` every 2s (35s timeout), writes logs to `logs/<service>.log`, saves PIDs to `pids/<service>.pid`, and outputs a final UP/DOWN status table. |
| [`get-tokens.sh`](get-tokens.sh) | **Authentication & Tokens** | Validates API Gateway reachability, registers test personas (`customer1`, `vendorA`, `vendorB`), authenticates all 4 accounts (`admin`, `customer1`, `vendorA`, `vendorB`), decodes JWT claims, and prints copy-pasteable token exports. Halts immediately on HTTP 500. |
| [`seed-data.ps1`](seed-data.ps1) | **Demo Data Seeding** | Polls API Gateway on port 8080 until reachable, registers 6 vendor profiles across categories (electronics, home, sportswear, beauty, books, food), approves profiles with admin, creates ~30 products with varied stock levels, registers 4 customer accounts, and places 8 multi-vendor/single-vendor checkout orders. Safe to run multiple times (idempotent). |
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
4. **Git Bash** (included with Git for Windows) or **PowerShell**

---

## 💻 How to Open a Shell in VS Code

1. Press <kbd>Ctrl</kbd> + <kbd>`</kbd> (or go to menu: **Terminal** &rarr; **New Terminal**).
2. Look at the terminal dropdown tab in the top right of the terminal window:
   - Click the dropdown arrow next to the `+` icon and select **Git Bash** or **PowerShell**.

---

## 🚀 Execution Workflow

### Step 1: Start All 7 Microservices

In your terminal, run:
```bash
./run-services.sh
```
*(Or from PowerShell: `.\start-all.ps1`)*

---

### Step 2: Register Users & Retrieve JWT Tokens

Once all services show `UP`, run:
```bash
./get-tokens.sh
```

---

### Step 3: Seed Realistic Demo Data

To populate a freshly started platform with sample vendors, products, and checkout orders, run the PowerShell seed script:

```powershell
.\seed-data.ps1
```

#### What `seed-data.ps1` Does:
1. **Health Check**: Polls `http://localhost:8080/actuator/health` until the API Gateway responds `UP`.
2. **Admin Authentication**: Authenticates as the seeded system administrator (`admin` / `Admin@12345`).
3. **Vendor Seeding & Approval**: Registers 6 vendor accounts across distinct categories (Electronics, Home & Living, Sportswear, Beauty & Personal Care, Books & Stationery, Gourmet Food) and approves their vendor profiles via Admin API.
4. **Product Catalog Seeding**: Creates ~30 realistic products spread across the 6 approved vendors with prices ($5–$500) and varying stock levels (including low stock and zero stock items). Note: Image URLs are skipped as the `imageUrl` field does not exist in the current product model.
5. **Customer Orders & Checkout**: Registers 4 customer accounts (`alice_buyer`, `bob_shopper`, `charlie_k`, `diana_pr`) and places 8 checkout orders (at least 3 containing items from multiple vendors).
6. **Idempotency**: Safe to run multiple times. If accounts, vendors, or products already exist, it reuses existing IDs and profiles instead of failing.

---

### Step 4: Using the Retrieved Tokens

#### In Terminal (cURL)
Copy-paste the export lines from Step 2 into your terminal, then query any protected endpoint:
```bash
# Query profile of authenticated user
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" http://localhost:8080/api/users/me

# Access vendor endpoint
curl -H "Authorization: Bearer $VENDOR_A_TOKEN" http://localhost:8080/api/vendors/me
```

#### In Swagger UI Aggregator
1. Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) in your browser.
2. Click the green **Authorize** button at the top right.
3. Paste the token into the `BearerAuth` box and click **Authorize**.

---

### Step 5: Stop All Services Cleanly

When you are done testing:
```bash
./stop-services.sh
```
*(Or from PowerShell: `.\stop-all.ps1`)*

---

## 📁 Runtime Directory Layout

Running the scripts generates the following folders in the root directory:

```text
marketgrid-commerce/
├── logs/                      # Log file for each service
├── pids/                      # Process ID files for clean shutdown
├── run-services.sh            # Script 1: Starts services
├── get-tokens.sh              # Script 2: Authentication & token extraction
├── seed-data.ps1              # Script 3: Demo data seeder
└── stop-services.sh           # Cleanup script
```
