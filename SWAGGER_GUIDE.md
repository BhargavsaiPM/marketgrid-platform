# Testing MarketGrid Endpoints with Swagger UI

This guide walks you through testing the entire multi-vendor e-commerce flow across all four services using the interactive **Swagger UI** interfaces.

---

## 1. Swagger UI Dashboard URLs

Each service hosts its own Swagger UI interface. Ensure the services are running before opening these links in your browser:

| Service | Port | Swagger UI URL | Raw OpenAPI JSON |
|---|---|---|---|
| **User Service** | `8081` | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |
| **Vendor Service** | `8082` | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) |
| **Product Service** | `8083` | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) |
| **Order Service** | `8084` | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) | [http://localhost:8084/v3/api-docs](http://localhost:8084/v3/api-docs) |

---

## 2. How to Authenticate in Swagger UI (The Authorize Button)

Every service is configured with a global JWT Bearer security scheme.

1. Locate the **"Authorize"** button (green button with a padlock icon 🔓) at the top-right of the Swagger UI page:
   
   ```
   [ 🔓 Authorize ]
   ```

2. When you click it, a modal window titled **"Available authorizations"** will appear with `BearerAuth (http, Bearer)`.
3. Paste your JWT token into the **Value** input field.
   > **Note:** You can enter just the raw token string (e.g. `eyJhbGciOi...`). Swagger UI automatically prepends `Bearer ` to the `Authorization` header.
4. Click **Authorize**, then click **Close**.
5. The lock icon will now appear closed (🔒). Any endpoint executed with **"Try it out" → "Execute"** will automatically include your token in the request headers.
6. To switch roles (e.g. from Customer to Vendor or Admin), click **Authorize**, click **Logout**, paste the new token, and click **Authorize** again.

---

## 3. End-to-End Walkthrough

Follow this sequence to test the entire lifecycle:

```
[User Service]          [Vendor Service]         [Product Service]         [Order Service]
 Register & Login  ──>   Create Profile    ──>     Create Product   ──>     Add to Cart
 Get JWT Tokens         Admin Approval            Catalog Browsing         Checkout & Track
```

---

### Step 1: User Service (`http://localhost:8081/swagger-ui.html`)

#### 1.1 Register Customer
1. Expand `POST /api/users/register`.
2. Click **Try it out**.
3. In the request body, enter:
   ```json
   {
     "username": "alice_customer",
     "password": "Password@123",
     "email": "alice@example.com",
     "role": "CUSTOMER"
   }
   ```
4. Click **Execute**. Verify the response code is `201 Created`.

#### 1.2 Login as Customer
1. Expand `POST /api/users/login`.
2. Click **Try it out**.
3. In the request body, enter:
   ```json
   {
     "username": "alice_customer",
     "password": "Password@123"
   }
   ```
4. Click **Execute**.
5. Copy the `"token"` string from the response body (exclude quotes).
   > **Tip:** Keep this saved in a text note as **CUSTOMER_TOKEN**.

#### 1.3 Verify Customer Authentication
1. Click the **Authorize** button at the top right.
2. Paste **CUSTOMER_TOKEN** and click **Authorize**, then **Close**.
3. Expand `GET /api/users/me`.
4. Click **Try it out** → **Execute**.
5. Verify response code is `200 OK` returning Alice's profile.

#### 1.4 Register Vendor
1. In `POST /api/users/register`, enter:
   ```json
   {
     "username": "bob_vendor",
     "password": "Password@123",
     "email": "bob@example.com",
     "role": "VENDOR"
   }
   ```
2. Click **Execute**. Verify `201 Created`.

#### 1.5 Login as Vendor
1. In `POST /api/users/login`, enter:
   ```json
   {
     "username": "bob_vendor",
     "password": "Password@123"
   }
   ```
2. Click **Execute**.
3. Copy the `"token"` string and save it as **VENDOR_TOKEN**.

#### 1.6 Login as Admin
The system automatically seeds a default admin on startup (`admin` / `Admin@12345`):
1. In `POST /api/users/login`, enter:
   ```json
   {
     "username": "admin",
     "password": "Admin@12345"
   }
   ```
2. Click **Execute**.
3. Copy the `"token"` string and save it as **ADMIN_TOKEN**.

---

### Step 2: Vendor Service (`http://localhost:8082/swagger-ui.html`)

Open `http://localhost:8082/swagger-ui.html` in your browser.

#### 2.1 Register Vendor Profile
1. Click **Authorize** at the top right.
2. Paste the **VENDOR_TOKEN** and click **Authorize**, then **Close**.
3. Expand `POST /api/vendors/register`.
4. Click **Try it out** and enter:
   ```json
   {
     "businessName": "Bob Tech Hub",
     "businessDescription": "Quality mechanical keyboards and mice",
     "contactPhone": "1234567890",
     "address": "456 Silicon Alley, Suite 100"
   }
   ```
5. Click **Execute**. Verify response is `201 Created` with `"status": "PENDING"` and `"id": 1`.

#### 2.2 View Own Vendor Profile
1. Expand `GET /api/vendors/me`.
2. Click **Try it out** → **Execute**.
3. Verify your profile is returned with `"status": "PENDING"`.

#### 2.3 Approve Vendor as Admin
1. Click **Authorize** at the top right, click **Logout**, paste **ADMIN_TOKEN**, click **Authorize**, then **Close**.
2. Expand `GET /api/vendors/pending`.
3. Click **Try it out** → **Execute**. You will see Bob's vendor profile in the pending list.
4. Expand `PUT /api/vendors/{vendorId}/approve`.
5. Enter `vendorId`: `1`.
6. Click **Execute**. Verify the response returns `"status": "APPROVED"`.

#### 2.4 Test Public Browsing
1. Expand `GET /api/vendors` (public list of all approved vendors).
2. Click **Try it out** → **Execute**.
3. Expand `GET /api/vendors/{vendorId}` and pass `1` to view public vendor details.

---

### Step 3: Product Service (`http://localhost:8083/swagger-ui.html`)

Open `http://localhost:8083/swagger-ui.html` in your browser.

#### 3.1 Create Product as Vendor
1. Click **Authorize** at the top right.
2. Paste the **VENDOR_TOKEN** and click **Authorize**, then **Close**.
3. Expand `POST /api/products`.
4. Click **Try it out** and enter:
   ```json
   {
     "name": "Wireless Mechanical Keyboard",
     "description": "RGB backlight, hot-swappable switches, Bluetooth 5.0",
     "price": 89.99,
     "stockQuantity": 50,
     "category": "Peripherals"
   }
   ```
5. Click **Execute**. Verify response code is `201 Created` with `"id": 1` and `"vendorId": 1`.

#### 3.2 Browse Products (Public)
1. Expand `GET /api/products`.
2. Click **Try it out** → **Execute**. All available products will be returned.
3. You can also test filtering by entering `Peripherals` in the optional `category` parameter.
4. Expand `GET /api/products/{productId}` and pass `1` to get single product detail.
5. Expand `GET /api/products/vendor/{vendorId}` and pass `1` to get all products belonging to Bob.

#### 3.3 Update Product Details
1. Expand `PUT /api/products/{productId}`.
2. Enter `productId`: `1`.
3. In the request body, adjust any field (e.g. update `price` to `79.99`).
4. Click **Execute**. Verify the updated product is returned.

---

### Step 4: Order Service (`http://localhost:8084/swagger-ui.html`)

Open `http://localhost:8084/swagger-ui.html` in your browser.

#### 4.1 Add Item to Cart as Customer
1. Click **Authorize** at the top right.
2. Paste the **CUSTOMER_TOKEN** and click **Authorize**, then **Close**.
3. Expand `POST /api/cart/items` under **Cart Management**.
4. Click **Try it out** and enter:
   ```json
   {
     "productId": 1,
     "quantity": 2
   }
   ```
5. Click **Execute**. Verify response is `200 OK` showing the cart with total amount calculated.

#### 4.2 View Cart
1. Expand `GET /api/cart`.
2. Click **Try it out** → **Execute**.
3. Verify the cart displays the 2 items with snapshot price `79.99` and `subtotal: 159.98`.

#### 4.3 Checkout
1. Expand `POST /api/orders/checkout` under **Order Management**.
2. Click **Try it out** → **Execute** (no body needed).
3. The checkout engine:
   - Calls `product-service` with internal admin credentials to decrement inventory stock by 2.
   - Marks the line item as `CONFIRMED`.
   - Clears the customer's cart.
   - Groups line items by `vendorId`.
4. Verify response is `201 Created` with structure:
   ```json
   {
     "orderId": 1,
     "userId": 2,
     "totalAmount": 159.98,
     "createdAt": "2026-09-18T...",
     "vendorGroups": [
       {
         "vendorId": 1,
         "subtotal": 159.98,
         "allConfirmed": true,
         "items": [
           {
             "id": 1,
             "productId": 1,
             "vendorId": 1,
             "quantity": 2,
             "priceAtOrderTime": 79.99,
             "subtotal": 159.98,
             "status": "CONFIRMED"
           }
         ]
       }
     ]
   }
   ```

#### 4.4 View Customer Order History & Order Details
1. Expand `GET /api/orders` → click **Try it out** → **Execute**. Customer Alice sees her order.
2. Expand `GET /api/orders/{orderId}` → enter `1` → click **Execute**. Shows the detailed vendor-grouped order.

#### 4.5 Verify Stock Decrement in Product Service
1. Switch back to the **Product Service** tab (`http://localhost:8083/swagger-ui.html`).
2. Expand `GET /api/products/1` → click **Execute**.
3. Note that `stockQuantity` has decremented from `50` to `48`.

#### 4.6 View Vendor Orders (Vendor Item Isolation)
1. In the **Order Service** tab (`http://localhost:8084/swagger-ui.html`), click **Authorize** at top right.
2. Click **Logout**, paste **VENDOR_TOKEN**, click **Authorize**, then **Close**.
3. Expand `GET /api/orders/vendor/mine`.
4. Click **Try it out** → **Execute**.
5. Bob only sees order line items that belong to his vendor ID (`1`). If multiple vendors had products in the customer's order, other vendors' items would be hidden.

---

## 4. Common Status Codes & Troubleshooting

| HTTP Status | Reason in Swagger UI | Resolution |
|---|---|---|
| **401 Unauthorized** | Missing or malformed JWT token | Click **Authorize** and paste a valid token from `POST /api/users/login`. |
| **403 Forbidden** | User role does not have permission (e.g. Customer attempting vendor/admin action) | Login with an account having the required role (`VENDOR` or `ADMIN`) and update the token in **Authorize**. |
| **404 Not Found** | Resource ID doesn't exist | Check that the entity (User, Vendor, Product, or Order) was created earlier. In-memory H2 databases reset if a service restarts. |
| **409 Conflict** | Stock lock conflict or all items failed checkout | Insufficient stock or concurrency conflict. Restock the product or retry. |
| **503 Service Unavailable** | A downstream microservice is down | Ensure all required services (Eureka, Config, User, Vendor, Product, Order) are running. |
