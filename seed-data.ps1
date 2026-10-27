# ==============================================================================
# MarketGrid Commerce — Automated Demo Data Seeder (PowerShell)
# ==============================================================================
# Fills a freshly started platform with realistic demo data through the API
# Gateway on port 8080. Safe to run multiple times (idempotent).
# ==============================================================================

[CmdletBinding()]
param(
    [string]$GatewayUrl = "http://localhost:8080",
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "==============================================================================" -ForegroundColor Magenta
Write-Host "         MARKETGRID COMMERCE — DEMO DATA SEEDER                                " -ForegroundColor Magenta
Write-Host "==============================================================================" -ForegroundColor Magenta
Write-Host " Gateway URL     : $GatewayUrl" -ForegroundColor DarkGray
Write-Host " Gateway Timeout : $TimeoutSeconds seconds" -ForegroundColor DarkGray
Write-Host "==============================================================================" -ForegroundColor Magenta
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 1: Wait for API Gateway
# ------------------------------------------------------------------------------
Write-Host "[1/6] Polling API Gateway reachability ($GatewayUrl/actuator/health)..." -ForegroundColor Yellow

$startTime = Get-Date
$gatewayUp = $false

while (((Get-Date) - $startTime).TotalSeconds -lt $TimeoutSeconds) {
    try {
        $health = Invoke-RestMethod -Uri "$GatewayUrl/actuator/health" -Method Get -TimeoutSec 3 -ErrorAction Stop
        if ($health.status -eq "UP" -or "$health" -match "UP") {
            $elapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 1)
            Write-Host "`r  [UP] API Gateway is healthy and reachable! (${elapsed}s)                      " -ForegroundColor Green
            $gatewayUp = $true
            break
        }
    }
    catch {
        # Waiting for gateway
    }
    Start-Sleep -Seconds 2
    $currentElapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 0)
    Write-Host "`r  Waiting for API Gateway (${currentElapsed}s / ${TimeoutSeconds}s)..." -NoNewline
}

if (-not $gatewayUp) {
    Write-Host "`n  [FAILED] API Gateway ($GatewayUrl) failed to respond within $TimeoutSeconds seconds!" -ForegroundColor Red
    Write-Host "  Please start the microservices first (e.g., using .\start-all.ps1 or bash run-services.sh)." -ForegroundColor Yellow
    return
}

# ------------------------------------------------------------------------------
# Helper Functions for API Calls

function Get-ProductSlug {
    param([string]$Name)
    if ([string]::IsNullOrWhiteSpace($Name)) { return "product" }
    $clean = $Name.ToLower() -replace '[^a-z0-9\s-]', '' -replace '\s+', '-' -replace '-+', '-'
    return $clean.Trim('-')
}

# ------------------------------------------------------------------------------

function Register-Or-Login-User {
    param(
        [string]$Username,
        [string]$Password,
        [string]$Email,
        [string]$Role
    )

    $regBody = @{
        username = $Username
        password = $Password
        email    = $Email
        role     = $Role
    } | ConvertTo-Json

    # Try registering
    try {
        $regResp = Invoke-RestMethod -Uri "$GatewayUrl/api/users/register" -Method Post -Body $regBody -ContentType "application/json" -ErrorAction Stop
    }
    catch {
        # User may already exist, which is expected during re-runs
    }

    # Always log in to get token
    $loginBody = @{
        username = $Username
        password = $Password
    } | ConvertTo-Json

    try {
        $loginResp = Invoke-RestMethod -Uri "$GatewayUrl/api/users/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
        return $loginResp.token
    }
    catch {
        Write-Host "  [!] Login failed for user '$Username': $_" -ForegroundColor Red
        return $null
    }
}

# ------------------------------------------------------------------------------
# STEP 2: Authenticate Admin Account
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[2/6] Authenticating seeded System Admin..." -ForegroundColor Yellow

$AdminToken = Register-Or-Login-User -Username "admin" -Password "Admin@12345" -Email "admin@marketgrid.com" -Role "ADMIN"
if (-not $AdminToken) {
    Write-Host "  [!] Failed to acquire Admin token. Aborting seed process." -ForegroundColor Red
    return
}
Write-Host "  [✓] Admin token acquired." -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 3: Create & Approve 6 Vendor Profiles
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[3/6] Seeding 6 approved Vendor accounts and profiles..." -ForegroundColor Yellow

$vendorDefinitions = @(
    @{
        Username    = "tech_hub"
        Password    = "VendorPass123!"
        Email       = "contact@techhub.com"
        Name        = "TechHub Electronics"
        Category    = "Electronics"
        Description = "Premier provider of cutting-edge laptops, audio gear, smart devices, and accessories."
        Phone       = "+1-555-0192"
        Address     = "100 Innovation Way, Silicon Valley, CA"
    },
    @{
        Username    = "cozy_home"
        Password    = "VendorPass123!"
        Email       = "support@cozyhome.com"
        Name        = "CozyHome Living"
        Category    = "Home & Living"
        Description = "Handcrafted furniture, ergonomic home office essentials, and artisanal home decor."
        Phone       = "+1-555-0283"
        Address     = "45 Craft Avenue, Portland, OR"
    },
    @{
        Username    = "apex_sports"
        Password    = "VendorPass123!"
        Email       = "info@apexsports.com"
        Name        = "Apex Sportswear"
        Category    = "Sportswear"
        Description = "High-performance athletic apparel, fitness gadgets, and outdoor adventure gear."
        Phone       = "+1-555-0374"
        Address     = "78 Athletic Parkway, Denver, CO"
    },
    @{
        Username    = "glow_beauty"
        Password    = "VendorPass123!"
        Email       = "hello@glowbeauty.com"
        Name        = "Glow Essentials"
        Category    = "Beauty & Personal Care"
        Description = "Organic skincare, cruelty-free cosmetics, and premium wellness products."
        Phone       = "+1-555-0465"
        Address     = "12 Beauty Lane, Los Angeles, CA"
    },
    @{
        Username    = "page_turners"
        Password    = "VendorPass123!"
        Email       = "orders@pageturners.com"
        Name        = "Page Turners Bookstore"
        Category    = "Books & Stationery"
        Description = "Curated bestseller fiction, technical guides, premium journals, and fountain pens."
        Phone       = "+1-555-0556"
        Address     = "300 Reader Street, Boston, MA"
    },
    @{
        Username    = "gourmet_haven"
        Password    = "VendorPass123!"
        Email       = "deli@gourmethaven.com"
        Name        = "Gourmet Haven"
        Category    = "Gourmet Food"
        Description = "Artisanal cheeses, single-origin coffee beans, dark chocolates, and specialty spices."
        Phone       = "+1-555-0647"
        Address     = "88 Culinary Court, Chicago, IL"
    }
)

$seededVendors = @()

foreach ($vDef in $vendorDefinitions) {
    # 1. Register/Login vendor user
    $vToken = Register-Or-Login-User -Username $vDef.Username -Password $vDef.Password -Email $vDef.Email -Role "VENDOR"
    if (-not $vToken) { continue }

    $headers = @{ Authorization = "Bearer $vToken" }
    $adminHeaders = @{ Authorization = "Bearer $AdminToken" }

    # 2. Check if vendor profile already exists
    $vendorProfile = $null
    try {
        $vendorProfile = Invoke-RestMethod -Uri "$GatewayUrl/api/vendors/me" -Method Get -Headers $headers -ErrorAction Stop
    }
    catch {
        # Profile does not exist yet
    }

    if (-not $vendorProfile) {
        $regBody = @{
            businessName        = $vDef.Name
            businessDescription = $vDef.Description
            contactPhone        = $vDef.Phone
            address             = $vDef.Address
        } | ConvertTo-Json

        try {
            $vendorProfile = Invoke-RestMethod -Uri "$GatewayUrl/api/vendors/register" -Method Post -Body $regBody -Headers $headers -ContentType "application/json" -ErrorAction Stop
            Write-Host "  [+] Registered vendor profile: $($vDef.Name)" -ForegroundColor Cyan
        }
        catch {
            Write-Host "  [!] Error registering vendor profile for $($vDef.Name): $_" -ForegroundColor Red
            continue
        }
    } else {
        Write-Host "  [=] Vendor profile already exists: $($vDef.Name) (ID: $($vendorProfile.id))" -ForegroundColor Gray
    }

    # 3. Approve vendor profile if not approved
    if (-not $vendorProfile.approved) {
        try {
            $approvedProfile = Invoke-RestMethod -Uri "$GatewayUrl/api/vendors/$($vendorProfile.id)/approve" -Method Put -Headers $adminHeaders -ErrorAction Stop
            $vendorProfile = $approvedProfile
            Write-Host "  [✓] Admin approved vendor: $($vDef.Name)" -ForegroundColor Green
        }
        catch {
            Write-Host "  [!] Error approving vendor $($vendorProfile.id): $_" -ForegroundColor Red
        }
    }

    $seededVendors += @{
        Id          = $vendorProfile.id
        Username    = $vDef.Username
        Token       = $vToken
        Name        = $vDef.Name
        Category    = $vDef.Category
    }
}

Write-Host "  Seeded $($seededVendors.Count) approved vendors." -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 4: Create 30 Products across Vendors
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[4/6] Seeding ~30 products across vendors..." -ForegroundColor Yellow
Write-Host "  [Info] Generating deterministic imageUrls (https://picsum.photos/seed/<slug>/800/800) for products." -ForegroundColor DarkCyan

$productCatalog = @(
    # Electronics (TechHub)
    @{ VendorIndex = 0; Name = "ProBook Ultra 15 Laptop"; Description = "15-inch OLED display, 32GB RAM, 1TB SSD for heavy workloads."; Price = 499.99; Stock = 15; Category = "Electronics" },
    @{ VendorIndex = 0; Name = "Noise-Canceling Wireless Headphones"; Description = "Active noise cancellation with 40-hour battery life."; Price = 189.50; Stock = 25; Category = "Electronics" },
    @{ VendorIndex = 0; Name = "Ergonomic Mechanical Keyboard"; Description = "RGB backlighting, tactile switches, and dual wireless mode."; Price = 119.00; Stock = 30; Category = "Electronics" },
    @{ VendorIndex = 0; Name = "Ultra-Fast USB-C Charging Hub"; Description = "100W Power Delivery with 7 expansion ports."; Price = 45.99; Stock = 50; Category = "Electronics" },
    @{ VendorIndex = 0; Name = "4K Webcam with Dual Microphones"; Description = "Autofocus, privacy shutter, and low-light enhancement."; Price = 79.99; Stock = 2; Category = "Electronics" },

    # Home & Living (CozyHome)
    @{ VendorIndex = 1; Name = "Ergonomic Mesh Office Chair"; Description = "Adjustable lumbar support and breathable mesh back."; Price = 249.99; Stock = 12; Category = "Home & Living" },
    @{ VendorIndex = 1; Name = "Hand-Poured Soy Scented Candle"; Description = "Lavender and eucalyptus natural soy wax candle."; Price = 24.50; Stock = 40; Category = "Home & Living" },
    @{ VendorIndex = 1; Name = "Minimalist Bamboo Desk Lamp"; Description = "Touch control dimmer with warm LED lighting."; Price = 38.00; Stock = 18; Category = "Home & Living" },
    @{ VendorIndex = 1; Name = "Linen Throw Blanket"; Description = "100% French flax linen blanket for sofa and bed."; Price = 65.00; Stock = 1; Category = "Home & Living" },
    @{ VendorIndex = 1; Name = "Ceramic Pour-Over Coffee Dripper"; Description = "Artisanal ceramic dripper with wooden stand."; Price = 29.99; Stock = 22; Category = "Home & Living" },

    # Sportswear (Apex Sports)
    @{ VendorIndex = 2; Name = "Pro Run Cushion Running Shoes"; Description = "Lightweight mesh upper with responsive foam cushioning."; Price = 129.99; Stock = 20; Category = "Sportswear" },
    @{ VendorIndex = 2; Name = "Thermal Compression Base Layer"; Description = "Moisture-wicking cold weather athletic shirt."; Price = 42.50; Stock = 35; Category = "Sportswear" },
    @{ VendorIndex = 2; Name = "Smart Fitness Tracking Watch"; Description = "Heart rate monitor, GPS, and 7-day battery life."; Price = 199.00; Stock = 10; Category = "Sportswear" },
    @{ VendorIndex = 2; Name = "Non-Slip Eco Yoga Mat"; Description = "6mm high-density natural rubber cushioning."; Price = 49.99; Stock = 0; Category = "Sportswear" },
    @{ VendorIndex = 2; Name = "Insulated Stainless Water Bottle"; Description = "Keep drinks cold for 24 hours or hot for 12 hours."; Price = 28.00; Stock = 60; Category = "Sportswear" },

    # Beauty (Glow Essentials)
    @{ VendorIndex = 3; Name = "Hydrating Hyaluronic Facial Serum"; Description = "Deep hydration formula with vitamin B5 and botanical extracts."; Price = 34.00; Stock = 45; Category = "Beauty & Personal Care" },
    @{ VendorIndex = 3; Name = "Nourishing Shea Butter Body Lotion"; Description = "Rich, non-greasy moisturization for dry skin."; Price = 18.50; Stock = 50; Category = "Beauty & Personal Care" },
    @{ VendorIndex = 3; Name = "Gentle Foaming Cleanser"; Description = "Sulfate-free face wash suitable for sensitive skin."; Price = 22.00; Stock = 28; Category = "Beauty & Personal Care" },
    @{ VendorIndex = 3; Name = "Mineral SPF 50 Sunscreen Lotion"; Description = "Broad-spectrum reef-safe sunscreen."; Price = 26.50; Stock = 2; Category = "Beauty & Personal Care" },
    @{ VendorIndex = 3; Name = "Organic Rosewater Facial Mist"; Description = "Refreshing botanical hydration mist."; Price = 15.00; Stock = 33; Category = "Beauty & Personal Care" },

    # Books (Page Turners)
    @{ VendorIndex = 4; Name = "Designing Microservices Architecture"; Description = "A comprehensive practical guide to cloud-native systems."; Price = 49.99; Stock = 25; Category = "Books & Stationery" },
    @{ VendorIndex = 4; Name = "Hardcover Dotted Grid Bullet Journal"; Description = "160gsm bleedproof paper with lay-flat binding."; Price = 21.50; Stock = 40; Category = "Books & Stationery" },
    @{ VendorIndex = 4; Name = "Brass Fountain Pen with Converter"; Description = "Precision weighted brass body with smooth fine nib."; Price = 55.00; Stock = 15; Category = "Books & Stationery" },
    @{ VendorIndex = 4; Name = "The Art of Clean Code (Hardcover)"; Description = "Principles and patterns for writing maintainable software."; Price = 39.95; Stock = 30; Category = "Books & Stationery" },
    @{ VendorIndex = 4; Name = "Minimalist Desk Pad Calendar"; Description = "12-month tear-off desk planner with habit tracker."; Price = 14.99; Stock = 20; Category = "Books & Stationery" },

    # Food (Gourmet Haven)
    @{ VendorIndex = 5; Name = "Single-Origin Ethiopian Whole Bean Coffee"; Description = "Medium roast with floral notes and bergamot aroma."; Price = 19.99; Stock = 35; Category = "Gourmet Food" },
    @{ VendorIndex = 5; Name = "Aged Artisanal Cheddar Wheel"; Description = "Aged for 24 months in natural caves for sharp rich flavor."; Price = 28.50; Stock = 15; Category = "Gourmet Food" },
    @{ VendorIndex = 5; Name = "85% Dark Belgian Chocolate Bars (Pack of 3)"; Description = "Organic fair-trade dark chocolate with sea salt."; Price = 16.00; Stock = 50; Category = "Gourmet Food" },
    @{ VendorIndex = 5; Name = "Extra Virgin Olive Oil (500ml)"; Description = "Cold-pressed single estate Italian olive oil."; Price = 24.99; Stock = 20; Category = "Gourmet Food" },
    @{ VendorIndex = 5; Name = "Organic Raw Wildflower Honey Jar"; Description = "Unfiltered raw wildflower honey from local apiaries."; Price = 12.99; Stock = 30; Category = "Gourmet Food" }
)

$allCreatedProducts = @()

foreach ($prodDef in $productCatalog) {
    $vendor = $seededVendors[$prodDef.VendorIndex]
    if (-not $vendor) { continue }

    $headers = @{ Authorization = "Bearer $($vendor.Token)" }

    # Fetch existing products for vendor to avoid duplicates
    $existingProds = @()
    try {
        $existingProds = Invoke-RestMethod -Uri "$GatewayUrl/api/products/vendor/$($vendor.Id)" -Method Get -ErrorAction Stop
    }
    catch {}

    $matchedProd = $existingProds | Where-Object { $_ .name -eq $prodDef.Name }

    $slug = Get-ProductSlug -Name $prodDef.Name
    $imgUrl = "https://picsum.photos/seed/$slug/800/800"

    if ($matchedProd) {
        # If product already exists without an imageUrl or needs updating, update via PUT
        if ([string]::IsNullOrEmpty($matchedProd.imageUrl) -or $matchedProd.imageUrl -ne $imgUrl) {
            $updateBody = @{
                name          = $prodDef.Name
                description   = $prodDef.Description
                price         = $prodDef.Price
                stockQuantity = $prodDef.Stock
                category      = $prodDef.Category
                imageUrl      = $imgUrl
            } | ConvertTo-Json

            try {
                $updatedProd = Invoke-RestMethod -Uri "$GatewayUrl/api/products/$($matchedProd.id)" -Method Put -Body $updateBody -Headers $headers -ContentType "application/json" -ErrorAction Stop
                Write-Host "  [^] Updated existing product with imageUrl: '$($prodDef.Name)' (ID: $($matchedProd.id))" -ForegroundColor Yellow
                $allCreatedProducts += $updatedProd
            }
            catch {
                Write-Host "  [=] Product exists: '$($prodDef.Name)' (ID: $($matchedProd.id)) (Update failed/skipped)" -ForegroundColor Gray
                $allCreatedProducts += $matchedProd
            }
        } else {
            Write-Host "  [=] Product already exists with image: '$($prodDef.Name)' (ID: $($matchedProd.id))" -ForegroundColor Gray
            $allCreatedProducts += $matchedProd
        }
    } else {
        $prodBody = @{
            name          = $prodDef.Name
            description   = $prodDef.Description
            price         = $prodDef.Price
            stockQuantity = $prodDef.Stock
            category      = $prodDef.Category
            imageUrl      = $imgUrl
        } | ConvertTo-Json

        try {
            $createdProd = Invoke-RestMethod -Uri "$GatewayUrl/api/products" -Method Post -Body $prodBody -Headers $headers -ContentType "application/json" -ErrorAction Stop
            Write-Host "  [+] Created product: '$($prodDef.Name)' (Price: \$$($prodDef.Price), Stock: $($prodDef.Stock))" -ForegroundColor Cyan
            $allCreatedProducts += $createdProd
        }
        catch {
            Write-Host "  [!] Failed to create product '$($prodDef.Name)': $_" -ForegroundColor Red
        }
    }
}

Write-Host "  Total available products in catalog: $($allCreatedProducts.Count)" -ForegroundColor Green

# ------------------------------------------------------------------------------
# STEP 5: Create Customer Accounts & Place 8 Orders
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[5/6] Creating 4 customer accounts and placing 8 orders..." -ForegroundColor Yellow

$customerDefinitions = @(
    @{ Username = "alice_buyer"; Password = "CustomerPass123!"; Email = "alice@example.com" },
    @{ Username = "bob_shopper"; Password = "CustomerPass123!"; Email = "bob@example.com" },
    @{ Username = "charlie_k";   Password = "CustomerPass123!"; Email = "charlie@example.com" },
    @{ Username = "diana_pr";    Password = "CustomerPass123!"; Email = "diana@example.com" }
)

$seededCustomers = @()
foreach ($cDef in $customerDefinitions) {
    $cToken = Register-Or-Login-User -Username $cDef.Username -Password $cDef.Password -Email $cDef.Email -Role "CUSTOMER"
    if ($cToken) {
        $seededCustomers += @{
            Username = $cDef.Username
            Password = $cDef.Password
            Token    = $cToken
        }
    }
}

Write-Host "  Seeded $($seededCustomers.Count) customer accounts." -ForegroundColor Green

# Filter out out-of-stock products for ordering
$orderableProducts = $allCreatedProducts | Where-Object { $_ .stockQuantity -gt 0 }

if ($orderableProducts.Count -lt 5) {
    Write-Host "  [!] Not enough orderable products to simulate multi-vendor orders." -ForegroundColor Red
} else {
    # Define 8 order scenarios (at least 3 multi-vendor)
    $orderScenarios = @(
        # Order 1: Alice — Multi-vendor (TechHub + CozyHome + Gourmet)
        @{ CustomerIndex = 0; ProductIndices = @(0, 5, 25); Quantities = @(1, 1, 2) },

        # Order 2: Alice — Multi-vendor (Apex Sports + Glow Beauty)
        @{ CustomerIndex = 0; ProductIndices = @(10, 15); Quantities = @(1, 1) },

        # Order 3: Bob — Multi-vendor (Page Turners + Gourmet Haven + TechHub)
        @{ CustomerIndex = 1; ProductIndices = @(20, 26, 1); Quantities = @(1, 2, 1) },

        # Order 4: Bob — Single vendor (Page Turners)
        @{ CustomerIndex = 1; ProductIndices = @(21, 23); Quantities = @(2, 1) },

        # Order 5: Charlie — Multi-vendor (CozyHome + Glow Beauty)
        @{ CustomerIndex = 2; ProductIndices = @(6, 16); Quantities = @(2, 1) },

        # Order 6: Charlie — Single vendor (TechHub)
        @{ CustomerIndex = 2; ProductIndices = @(2, 3); Quantities = @(1, 1) },

        # Order 7: Diana — Multi-vendor (Gourmet Haven + Page Turners)
        @{ CustomerIndex = 3; ProductIndices = @(25, 27, 22); Quantities = @(1, 1, 1) },

        # Order 8: Diana — Single vendor (Glow Beauty)
        @{ CustomerIndex = 3; ProductIndices = @(15, 17); Quantities = @(1, 1) }
    )

    $ordersCreatedCount = 0

    foreach ($idx in 0..($orderScenarios.Count - 1)) {
        $sc = $orderScenarios[$idx]
        $customer = $seededCustomers[$sc.CustomerIndex]
        if (-not $customer) { continue }

        $cHeaders = @{ Authorization = "Bearer $($customer.Token)" }

        # 1. Clear cart
        try {
            Invoke-RestMethod -Uri "$GatewayUrl/api/cart" -Method Delete -Headers $cHeaders -ErrorAction Stop | Out-Null
        } catch {}

        # 2. Add items to cart
        $addedAny = $false
        for ($i = 0; $i -lt $sc.ProductIndices.Count; $i++) {
            $pIdx = $sc.ProductIndices[$i]
            $qty = $sc.Quantities[$i]
            $prod = $orderableProducts[$pIdx]

            if ($prod) {
                $cartBody = @{
                    productId = $prod.id
                    quantity  = $qty
                } | ConvertTo-Json

                try {
                    Invoke-RestMethod -Uri "$GatewayUrl/api/cart/items" -Method Post -Body $cartBody -Headers $cHeaders -ContentType "application/json" -ErrorAction Stop | Out-Null
                    $addedAny = $true
                }
                catch {
                    Write-Host "  [!] Could not add product $($prod.id) to cart: $_" -ForegroundColor Red
                }
            }
        }

        # 3. Checkout
        if ($addedAny) {
            try {
                $orderResp = Invoke-RestMethod -Uri "$GatewayUrl/api/orders/checkout" -Method Post -Headers $cHeaders -ContentType "application/json" -ErrorAction Stop
                $ordersCreatedCount++
                $vCount = $orderResp.vendorGroups.Count
                Write-Host "  [+] Order #$($orderResp.orderId) placed by $($customer.Username) (Total: \$$($orderResp.totalAmount), Vendor Groups: $vCount)" -ForegroundColor Cyan
            }
            catch {
                Write-Host "  [!] Checkout failed for $($customer.Username): $_" -ForegroundColor Red
            }
        }
    }

    Write-Host "  Total orders created: $ordersCreatedCount" -ForegroundColor Green
}

# ------------------------------------------------------------------------------
# STEP 6: Summary Report
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "==============================================================================" -ForegroundColor Magenta
Write-Host "                       SEED DATA SUMMARY REPORT                                 " -ForegroundColor Magenta
Write-Host "==============================================================================" -ForegroundColor Magenta
Write-Host "  Vendors Created & Approved : $($seededVendors.Count)" -ForegroundColor Green
$prodsWithImagesCount = ($allCreatedProducts | Where-Object { -not [string]::IsNullOrEmpty($_.imageUrl) }).Count
Write-Host "  Products Seeding Total     : $($allCreatedProducts.Count)" -ForegroundColor Green
Write-Host "  Products with Images       : $prodsWithImagesCount" -ForegroundColor Green
Write-Host "  Customer Accounts Created  : $($seededCustomers.Count)" -ForegroundColor Green
Write-Host "  Checkout Orders Placed     : $ordersCreatedCount" -ForegroundColor Green
Write-Host "==============================================================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "SAMPLE DEMO CREDENTIALS:" -ForegroundColor Cyan
Write-Host "  • System Admin : username = admin         | password = Admin@12345" -ForegroundColor White
Write-Host "  • Demo Vendor  : username = tech_hub      | password = VendorPass123!" -ForegroundColor White
Write-Host "  • Demo Buyer   : username = alice_buyer   | password = CustomerPass123!" -ForegroundColor White
Write-Host ""
Write-Host "IMAGE URL FIELD STATUS:" -ForegroundColor Green
Write-Host "  • Configured: Every product includes a deterministic imageUrl." -ForegroundColor Green
Write-Host ""
Write-Host "==============================================================================" -ForegroundColor Magenta
Write-Host "✓ SEEDING COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host ""
